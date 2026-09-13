package moe.ouom.neriplayer.core.api.qqmusic

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.auth.qqmusic.QQMusicCredential
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "QQMusicCredentialRefresh"

private const val MUSICU_HOST = "https://u.y.qq.com/cgi-bin/musicu.fcg"

/** 登录/续期业务错误码全集（对齐 QQMusicapi `login.js` `LOGIN_ERROR_CODES`）。 */
private val LOGIN_BIZ_ERROR_CODES = setOf(
    1000, 104401, 104400, 20261, 20271, 20272, 20274, 20277, 20278, 20279, 20450, 104604
)

/** 鉴权过期类错误码：凭证已被服务端判定失效，续期不可恢复。 */
private val CREDENTIAL_EXPIRED_CODES = setOf(1000, 104401, 104400)

/** QQ音乐凭证续期结果，失败语义决定调用方是「保留现凭证」还是「降级匿名态」。 */
internal sealed class QQMusicRefreshResult {

    /** 续期成功，服务端下发了新凭证。 */
    data class Renewed(val credential: QQMusicCredential) : QQMusicRefreshResult()

    /** 服务端判定鉴权过期（1000 / 104401 / 104400）：凭证不可再用，应登出降级匿名态。 */
    data object Rejected : QQMusicRefreshResult()

    /** 瞬时失败（网络 / 限流 / 参数态错误 / 响应异常）：保留现凭证，稍后再试。 */
    data object TransientError : QQMusicRefreshResult()
}

/**
 * 续期 QQ音乐登录凭证（`music.login.LoginServer` / `Login`，`loginMode: 2`，
 * 对齐 QQMusicapi `login.js` 的 `refreshCredential`）。
 *
 * - comm 走 Android 平台（依赖 [QQMusicDevice] 的 QIMEI + session，24h 有效期），
 *   凭证同时经 cookie（`qm_keyst` / `qqmusic_key` / `uin`）与 comm（`qq` / `authst`）双通道携带；
 * - param 按 `loginType` 分三套（1 = 微信 / 2 = QQ 扫码 / 其他 = 合并形态），
 *   `expired_in` 沿用蓝本命名（值取凭证 `expiredAt`，1:1 对齐，勿"修正"）；
 * - 失败语义：仅鉴权过期码（1000 / 104401 / 104400）判为 [QQMusicRefreshResult.Rejected]，
 *   其余一律 [QQMusicRefreshResult.TransientError]，避免协议抖动误把用户登出。
 */
internal suspend fun QQMusicSession.refreshQQMusicCredential(): QQMusicRefreshResult =
    withContext(Dispatchers.IO) {
        val cred = credential
        if (!cred.isLoggedIn) return@withContext QQMusicRefreshResult.TransientError
        if (cred.refreshKey.isBlank()) {
            // 无 refresh_key 则无法续期；musickey 仍可能有效，保留登录态直至其自然失效
            NPLogger.w(TAG, "refreshKey missing, skip renewal (musicid=${cred.musicid})")
            return@withContext QQMusicRefreshResult.TransientError
        }

        try {
            device.ensureReady()
        } catch (error: Exception) {
            NPLogger.w(TAG, "device not ready for credential refresh: ${error.message}")
            return@withContext QQMusicRefreshResult.TransientError
        }

        val payload = JSONObject()
            .put("comm", device.buildAndroidComm(cred))
            .put(
                "req_0",
                JSONObject()
                    .put("module", "music.login.LoginServer")
                    .put("method", "Login")
                    .put("param", buildRefreshParam(cred))
            )

        // Android 平台协议要求 POST + JSON body（对齐 QQMusicapi requestApi），
        // GET + data 查询参数会得到全局 code=500001
        val request = Request.Builder()
            .url(MUSICU_HOST.toHttpUrl())
            .header("User-Agent", device.androidUserAgent())
            .header("Referer", "https://y.qq.com")
            .header("Cookie", cred.toCookieHeader())
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val responseText = try {
            client.newCall(request).awaitResponse { response ->
                if (!response.isSuccessful) throw IOException("Login refresh HTTP ${response.code}")
                response.body.string()
            }
        } catch (error: IOException) {
            NPLogger.w(TAG, "credential refresh HTTP failed: ${error.message}")
            return@withContext QQMusicRefreshResult.TransientError
        }

        val root = runCatching { JSONObject(responseText) }.getOrNull()
        if (root == null) {
            NPLogger.w(TAG, "credential refresh response not JSON: ${responseText.take(160)}")
            return@withContext QQMusicRefreshResult.TransientError
        }
        val globalCode = root.optInt("code", 0)
        if (globalCode != 0) {
            NPLogger.w(TAG, "credential refresh global code=$globalCode: ${responseText.take(200)}")
            return@withContext QQMusicRefreshResult.TransientError
        }

        val req0 = root.optJSONObject("req_0")
        val bizCode = req0?.optInt("code", -1) ?: -1
        if (bizCode != 0) {
            if (bizCode !in LOGIN_BIZ_ERROR_CODES) {
                NPLogger.w(TAG, "credential refresh unexpected code=$bizCode: ${responseText.take(200)}")
            }
            return@withContext if (bizCode in CREDENTIAL_EXPIRED_CODES) {
                NPLogger.w(TAG, "credential expired on server: code=$bizCode")
                QQMusicRefreshResult.Rejected
            } else {
                QQMusicRefreshResult.TransientError
            }
        }

        val data = req0.optJSONObject("data")
        val renewed = data?.let { QQMusicCredential.fromApiData(it) }
        if (renewed == null || renewed.musicid == 0L || renewed.musickey.isBlank()) {
            // code=0 但未回凭证：结构异常，保留现凭证观察
            NPLogger.w(TAG, "credential refresh returned no credential: ${responseText.take(200)}")
            return@withContext QQMusicRefreshResult.TransientError
        }
        val merged = mergeRenewedCredential(old = cred, renewed = renewed)
        NPLogger.d(TAG, "credential renewed: musicid=${merged.musicid} loginType=${merged.loginType}")
        QQMusicRefreshResult.Renewed(merged)
    }

/**
 * 续期 param 按 `loginType` 分三套，1:1 对齐 QQMusicapi `login.js` `refreshCredential`。
 * QQ 扫码登录产出 loginType=2；loginType=1（微信）依赖 `unionid`，由 [QQMusicCredential] 携带。
 */
private fun buildRefreshParam(cred: QQMusicCredential): JSONObject = when (cred.loginType) {
    1 -> JSONObject()
        .put("openid", cred.openid)
        .put("refresh_token", cred.refreshToken)
        .put("str_musicid", cred.musicid.toString())
        .put("musickey", cred.musickey)
        .put("unionid", cred.unionid)
        .put("refresh_key", cred.refreshKey)
        .put("loginMode", 2)

    2 -> JSONObject()
        .put("openid", cred.openid)
        .put("access_token", cred.accessToken)
        .put("refresh_token", cred.refreshToken)
        .put("expired_in", cred.expiredAt)
        .put("musicid", cred.musicid)
        .put("musickey", cred.musickey)
        .put("refresh_key", cred.refreshKey)
        .put("loginMode", 2)

    else -> JSONObject()
        .put("openid", cred.openid)
        .put("access_token", cred.accessToken)
        .put("refresh_token", cred.refreshToken)
        .put("expired_in", cred.expiredAt)
        .put("str_musicid", cred.musicid.toString())
        .put("musicid", cred.musicid)
        .put("musickey", cred.musickey)
        .put("unionid", cred.unionid)
        .put("refresh_key", cred.refreshKey)
        .put("loginMode", 2)
}

/**
 * 合并续期响应与旧凭证：服务端若未回传某字段（省略 / 置空），保留旧值，
 * 防止 refresh_key 等关键字段被意外清空导致无法再次续期。
 */
private fun mergeRenewedCredential(
    old: QQMusicCredential,
    renewed: QQMusicCredential
): QQMusicCredential = renewed.copy(
    musicid = renewed.musicid.takeIf { it != 0L } ?: old.musicid,
    musickey = renewed.musickey.ifBlank { old.musickey },
    refreshKey = renewed.refreshKey.ifBlank { old.refreshKey },
    openid = renewed.openid.ifBlank { old.openid },
    accessToken = renewed.accessToken.ifBlank { old.accessToken },
    refreshToken = renewed.refreshToken.ifBlank { old.refreshToken },
    unionid = renewed.unionid.ifBlank { old.unionid },
    expiredAt = renewed.expiredAt.takeIf { it > 0L } ?: old.expiredAt,
    loginType = renewed.loginType.takeIf { it != 0 } ?: old.loginType,
)
