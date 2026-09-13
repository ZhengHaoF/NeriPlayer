package moe.ouom.neriplayer.core.api.qqmusic

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.auth.qqmusic.QQMusicCredential
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "QQMusicDevice"

private const val QIMEI_HOST = "https://api.tencentmusic.com/tme/trpc/proxy"
private const val MUSICU_HOST = "https://u.y.qq.com/cgi-bin/musicu.fcg"
private const val QIMEI_PUBLIC_KEY =
    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEIxgwoutfwoJxcGQeedgP7FG9qaIuS0qzfR8gWkrkTZKM2iWHn2ajQpBRZjMSoSf6+KJGvar2ORhBfpDXyVtZCKpqLQ+FLkpncClKVIrBwv6PHyUvuCb0rIarmgDnzkfQAqVufEtR64iazGDKatvJ9y6B9NMbHddGSAUmRTCrHQIDAQAB"
private const val QIMEI_SECRET = "ZdJqM15EeO2zWc08"
private const val QIMEI_APP_KEY = "0AND0HD6FE4HY80F"
private const val QIMEI_CHANNEL_ID = "10003505"
private const val QIMEI_APP_VERSION = "14.9.0.8"
private const val QIMEI_SDK_VERSION = "1.2.13.6"
private const val QIMEI_APPID = "qimei_qq_android"
private const val QIMEI_SIGN_SALT = "qimei_qq_androidpzAuCmaFAaFaHrdakPjLIEqKrGnSOOvH"
private const val PACKAGE_ID = "com.tencent.qqmusic"
private const val ANDROID_CT = 11
private const val ANDROID_CV = 14090008
private const val ANDROID_V = 14090008
private const val LIFETIME_SECONDS = 24 * 60 * 60L
private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

/** 固定伪造设备画像（MI 6 / Android 10），对齐 QQMusicapi `device.json`，避免真机信息外发。 */
private data class QQMusicDeviceProfile(
    val brand: String = "Xiaomi",
    val device: String = "sagit",
    val model: String = "MI 6",
    val board: String = "eomam",
    val bootloader: String = "U-boot",
    val display: String,
    val fingerprint: String,
    val product: String = "iarim",
    val incremental: String = "5891938",
    val release: String = "10",
    val codename: String = "REL",
    val sdk: Int = 29,
    val androidId: String,
    val imei: String,
    val openUdid: String,
    val procVersion: String,
    val macAddress: String = "00:50:56:C0:00:08",
) {
    companion object {
        fun create(): QQMusicDeviceProfile {
            val displaySuffix = QQMusicCrypto.randomInt(100_000, 999_998)
            val fingerprintSuffix = QQMusicCrypto.randomInt(1_000_000, 9_999_998)
            return QQMusicDeviceProfile(
                display = "QMAPI.$displaySuffix.001",
                fingerprint = "xiaomi/iarim/sagit:10/eomam.200122.001/$fingerprintSuffix:user/release-keys",
                androidId = QQMusicCrypto.randomHexChars(8),
                imei = QQMusicCrypto.randomImei(),
                openUdid = UUID.randomUUID().toString().replace("-", ""),
                procVersion = "Linux 5.4.0-54-generic-${QQMusicCrypto.randomHexChars(4)} (android-build@google.com)",
            )
        }
    }
}

/** 登录态取址所需的登录凭据快照。 */
internal data class QQMusicAndroidSession(
    val uid: String,
    val sid: String,
    val vkey: String,
)

/**
 * QQ音乐 Android 协议设备身份：QIMEI 注册 + getSession。
 *
 * 路线 A（QR 扫码）的强制前置：`QQLogin` 与凭证续期都走 Android comm，
 * 而 session 与 comm 参数依赖 QIMEI（q16/q36）与 session.uid/sid。
 * 二者均为 24 小时有效期，签发时间落盘后可跨冷启动复用；
 * 注册用互斥锁串行化，对齐 QQMusicapi `QimeiManager._lock`。
 *
 * 设备身份与登录态解耦：退出登录时不清除本类持久化字段。
 */
class QQMusicDevice(
    context: Context,
    private val client: OkHttpClient,
    /** 与匿名播放共用的 OpenUDID / udid。 */
    private val guidProvider: () -> String,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mutex = Mutex()

    private val profile: QQMusicDeviceProfile by lazy { loadOrCreateProfile() }

    val androidId: String get() = profile.androidId
    val imei: String get() = profile.imei
    val openUdid: String get() = profile.openUdid
    val brand: String get() = profile.brand
    val model: String get() = profile.model
    val fingerprint: String get() = profile.fingerprint
    val osRelease: String get() = profile.release
    val sdk: Int get() = profile.sdk

    /**
     * 确保 QIMEI + session 均在有效期内。过期或缺失时静默重注册。
     * 并发调用只会有一次注册请求。
     */
    suspend fun ensureReady() {
        if (hasValidQimei() && hasValidSession()) return
        mutex.withLock {
            if (hasValidQimei() && hasValidSession()) return
            if (!hasValidQimei()) {
                registerQimei()
            }
            refreshSession()
        }
    }

    fun buildAndroidComm(credential: QQMusicCredential? = null): JSONObject {
        val guid = guidProvider()
        val session = currentSession()
        val comm = JSONObject()
            .put("ct", ANDROID_CT)
            .put("cv", ANDROID_CV)
            .put("v", ANDROID_V)
            .put("chid", QIMEI_CHANNEL_ID)
            .put("tmeAppID", "qqmusic")
            .put("QIMEI", cachedQ16())
            .put("QIMEI36", cachedQ36())
            .put("OpenUDID", guid)
            .put("udid", guid)
            .put("OpenUDID2", guid)
            .put("uid", session?.uid ?: "")
            .put("sid", session?.sid ?: "")
            .put("aid", profile.androidId)
            .put("os_ver", profile.release)
            .put("phonetype", profile.model)
            .put("devicelevel", profile.sdk.toString())
            .put("newdevicelevel", profile.sdk.toString())
            .put("rom", profile.fingerprint)
        if (credential?.isLoggedIn == true) {
            comm.put("qq", credential.musicid.toString())
            comm.put("authst", credential.musickey)
            comm.put("tmeLoginType", credential.loginType)
        }
        return comm
    }

    fun androidUserAgent(): String = "QQMusic $ANDROID_V(android ${profile.release})"

    // ==================== QIMEI ====================

    private suspend fun registerQimei() {
        // 对齐 QQMusicapi randomHex(16)：返回 16 个 hex 字符，UTF-8 恰好 16 字节，可直接作 AES-128 key
        val cryptKey = QQMusicCrypto.randomHexChars(16)
        val nonce = QQMusicCrypto.randomHexChars(16)
        val ts = System.currentTimeMillis() / 1000L
        val payload = buildQimeiPayload()
        val key = QQMusicCrypto.rsaPkcs1Base64(cryptKey, QIMEI_PUBLIC_KEY)
        val params = QQMusicCrypto.aesCbcBase64(
            cryptKey.toByteArray(Charsets.UTF_8),
            payload.toString()
        )
        val extra = """{"appKey":"$QIMEI_APP_KEY"}"""
        val reqSign = QQMusicCrypto.md5Hex(
            key,
            params,
            (ts * 1000).toString(),
            nonce,
            QIMEI_SECRET,
            extra
        )
        val headerSign = QQMusicCrypto.md5Hex(QIMEI_SIGN_SALT, ts.toString())

        val body = JSONObject()
            .put("app", 0)
            .put("os", 1)
            .put(
                "qimeiParams",
                JSONObject()
                    .put("key", key)
                    .put("params", params)
                    .put("time", ts.toString())
                    .put("nonce", nonce)
                    .put("sign", reqSign)
                    .put("extra", extra)
            )

        val request = Request.Builder()
            .url(QIMEI_HOST)
            .header("Host", "api.tencentmusic.com")
            .header("method", "GetQimei")
            .header("service", "trpc.tme_datasvr.qimeiproxy.QimeiProxy")
            .header("appid", QIMEI_APPID)
            .header("sign", headerSign)
            .header("user-agent", "QQMusic")
            .header("timestamp", ts.toString())
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val responseText = client.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("QIMEI HTTP ${response.code}")
            response.body.string()
        }

        val root = JSONObject(responseText)
        val inner = when (val data = root.opt("data")) {
            is String -> JSONObject(data).optJSONObject("data")
            is JSONObject -> data.optJSONObject("data")
            else -> null
        }
        val q16 = inner?.optString("q16").orEmpty()
        val q36 = inner?.optString("q36").orEmpty()
        if (q16.isBlank() || q36.isBlank()) {
            throw IllegalStateException("QIMEI response missing q16/q36: ${responseText.take(200)}")
        }

        prefs.edit()
            .putString(KEY_Q16, q16)
            .putString(KEY_Q36, q36)
            .putLong(KEY_QIMEI_SAVE_TIME, System.currentTimeMillis())
            .apply()
        NPLogger.d(TAG, "QIMEI registered")
    }

    private fun buildQimeiPayload(): JSONObject {
        val fixedRand = QQMusicCrypto.randomInt(0, 14_400)
        val upTimeMs = System.currentTimeMillis() - fixedRand * 1000L
        val upTimeStr = formatUpTime(upTimeMs)
        val reserved = JSONObject()
            .put("harmony", "0")
            .put("clone", "0")
            .put("containe", "")
            .put("oz", "UhYmelwouA+V2nPWbOvLTgN2/m8jwGB+yUB5v9tysQg=")
            .put("oo", "Xecjt+9S1+f8Pz2VLSxgpw==")
            .put("kelong", "0")
            .put("uptimes", upTimeStr)
            .put("multiUser", "0")
            .put("bod", profile.brand)
            .put("dv", profile.device)
            .put("firstLevel", "")
            .put("manufact", profile.brand)
            .put("name", profile.model)
            .put("host", "se.infra")
            .put("kernel", profile.procVersion)

        return JSONObject()
            .put("androidId", profile.androidId)
            .put("platformId", 1)
            .put("appKey", QIMEI_APP_KEY)
            .put("appVersion", QIMEI_APP_VERSION)
            .put(
                "beaconIdSrc",
                QQMusicCrypto.randomBeaconId(QQMusicCrypto.utcMonthStart())
            )
            .put("brand", profile.brand)
            .put("channelId", QIMEI_CHANNEL_ID)
            .put("cid", "")
            .put("imei", profile.imei)
            .put("imsi", "")
            .put("mac", "")
            .put("model", profile.model)
            .put("networkType", "unknown")
            .put("oaid", "")
            .put("osVersion", "Android ${profile.release},level ${profile.sdk}")
            .put("qimei", "")
            .put("qimei36", "")
            .put("sdkVersion", QIMEI_SDK_VERSION)
            .put("targetSdkVersion", "33")
            .put("audit", "")
            .put("userId", "{}")
            .put("packageId", PACKAGE_ID)
            .put("deviceType", "Phone")
            .put("sdkName", "")
            .put("reserved", reserved.toString())
    }

    // ==================== session ====================

    private suspend fun refreshSession() {
        val payload = JSONObject()
            .put("comm", buildAndroidComm(null))
            .put(
                "req_0",
                JSONObject()
                    .put("module", "music.getSession.session")
                    .put("method", "GetSession")
                    .put(
                        "param",
                        JSONObject()
                            .put("uid", currentSession()?.uid ?: "")
                            .put("vkey", 0)
                            .put("caller", 0)
                    )
            )

        // Android 平台协议同样要求 POST + JSON body（对齐 QQMusicapi `_doEnsureSession`）。
        val request = Request.Builder()
            .url(MUSICU_HOST.toHttpUrl())
            .header("User-Agent", androidUserAgent())
            .header("Referer", "https://y.qq.com")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val responseText = client.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("GetSession HTTP ${response.code}")
            response.body.string()
        }

        val root = JSONObject(responseText)
        val session = root.optJSONObject("req_0")?.optJSONObject("data")?.optJSONObject("session")
        val uid = session?.opt("uid")?.toString().orEmpty()
        val sid = session?.optString("sid").orEmpty()
        val vkey = session?.optString("vkey").orEmpty()
        if (sid.isBlank()) {
            throw IllegalStateException("GetSession missing sid: ${responseText.take(200)}")
        }

        prefs.edit()
            .putString(KEY_SESSION_UID, uid)
            .putString(KEY_SESSION_SID, sid)
            .putString(KEY_SESSION_VKEY, vkey)
            .putLong(KEY_SESSION_SAVE_TIME, System.currentTimeMillis())
            .apply()
        NPLogger.d(TAG, "session refreshed: uid=$uid sidLen=${sid.length}")
    }

    private fun currentSession(): QQMusicAndroidSession? {
        val sid = prefs.getString(KEY_SESSION_SID, null).orEmpty()
        if (sid.isBlank()) return null
        return QQMusicAndroidSession(
            uid = prefs.getString(KEY_SESSION_UID, "").orEmpty(),
            sid = sid,
            vkey = prefs.getString(KEY_SESSION_VKEY, "").orEmpty(),
        )
    }

    private fun hasValidQimei(): Boolean {
        val q16 = prefs.getString(KEY_Q16, null)
        val q36 = prefs.getString(KEY_Q36, null)
        if (q16.isNullOrBlank() || q36.isNullOrBlank()) return false
        return isFresh(KEY_QIMEI_SAVE_TIME)
    }

    private fun hasValidSession(): Boolean {
        val sid = prefs.getString(KEY_SESSION_SID, null)
        if (sid.isNullOrBlank()) return false
        return isFresh(KEY_SESSION_SAVE_TIME)
    }

    private fun isFresh(timeKey: String): Boolean {
        val savedAt = prefs.getLong(timeKey, 0L)
        if (savedAt <= 0L) return false
        val now = System.currentTimeMillis() / 1000L
        val savedAtSeconds = savedAt / 1000L
        return now - savedAtSeconds < LIFETIME_SECONDS
    }

    private fun cachedQ16(): String = prefs.getString(KEY_Q16, "").orEmpty()
    private fun cachedQ36(): String = prefs.getString(KEY_Q36, "").orEmpty()

    private fun loadOrCreateProfile(): QQMusicDeviceProfile {
        val existing = runCatching { prefs.getString(KEY_PROFILE, null) }.getOrNull()
        if (!existing.isNullOrBlank()) {
            runCatching { return parseProfile(existing) }
                .onFailure { NPLogger.w(TAG, "failed to parse device profile: ${it.message}") }
        }
        val created = QQMusicDeviceProfile.create()
        runCatching {
            prefs.edit().putString(KEY_PROFILE, profileToJson(created)).apply()
        }.onFailure { NPLogger.w(TAG, "failed to persist device profile: ${it.message}") }
        NPLogger.d(TAG, "generated new QQ Music device profile")
        return created
    }

    private fun parseProfile(raw: String): QQMusicDeviceProfile {
        val obj = JSONObject(raw)
        return QQMusicDeviceProfile(
            display = obj.getString("display"),
            fingerprint = obj.getString("fingerprint"),
            androidId = obj.getString("androidId"),
            imei = obj.getString("imei"),
            openUdid = obj.getString("openUdid"),
            procVersion = obj.getString("procVersion"),
            brand = obj.optString("brand", "Xiaomi"),
            device = obj.optString("device", "sagit"),
            model = obj.optString("model", "MI 6"),
            board = obj.optString("board", "eomam"),
            bootloader = obj.optString("bootloader", "U-boot"),
            product = obj.optString("product", "iarim"),
            incremental = obj.optString("incremental", "5891938"),
            release = obj.optString("release", "10"),
            codename = obj.optString("codename", "REL"),
            sdk = obj.optInt("sdk", 29),
            macAddress = obj.optString("macAddress", "00:50:56:C0:00:08"),
        )
    }

    private fun profileToJson(profile: QQMusicDeviceProfile): String = JSONObject()
        .put("display", profile.display)
        .put("fingerprint", profile.fingerprint)
        .put("androidId", profile.androidId)
        .put("imei", profile.imei)
        .put("openUdid", profile.openUdid)
        .put("procVersion", profile.procVersion)
        .put("brand", profile.brand)
        .put("device", profile.device)
        .put("model", profile.model)
        .put("board", profile.board)
        .put("bootloader", profile.bootloader)
        .put("product", profile.product)
        .put("incremental", profile.incremental)
        .put("release", profile.release)
        .put("codename", profile.codename)
        .put("sdk", profile.sdk)
        .put("macAddress", profile.macAddress)
        .toString()

    private fun formatUpTime(epochMs: Long): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return format.format(java.util.Date(epochMs))
    }

    private companion object {
        const val PREFS_NAME = "qqmusic_device"
        const val KEY_PROFILE = "profile"
        const val KEY_Q16 = "qimei"
        const val KEY_Q36 = "qimei36"
        const val KEY_QIMEI_SAVE_TIME = "qimei_save_time"
        const val KEY_SESSION_UID = "session_uid"
        const val KEY_SESSION_SID = "session_sid"
        const val KEY_SESSION_VKEY = "session_vkey"
        const val KEY_SESSION_SAVE_TIME = "session_save_time"
    }
}
