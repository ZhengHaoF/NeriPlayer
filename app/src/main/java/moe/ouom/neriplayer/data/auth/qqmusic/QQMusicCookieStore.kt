package moe.ouom.neriplayer.data.auth.qqmusic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.ouom.neriplayer.core.logging.NPLogger
import org.json.JSONObject

/** QQ音乐登录凭证（`QQLogin` 响应落盘后的形态）。 */
data class QQMusicCredential(
    val musicid: Long = 0L,
    val musickey: String = "",
    val refreshKey: String = "",
    val openid: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val unionid: String = "",
    val expiredAt: Long = 0L,
    val loginType: Int = 2,
) {
    val isLoggedIn: Boolean
        get() = musicid != 0L && musickey.isNotBlank()

    fun toCookieMap(): Map<String, String> {
        if (!isLoggedIn) return emptyMap()
        val uin = musicid.toString()
        return mapOf(
            "uin" to uin,
            "qqmusic_uin" to uin,
            "qm_keyst" to musickey,
            "qqmusic_key" to musickey,
        )
    }

    fun toCookieHeader(): String =
        toCookieMap().entries.joinToString("; ") { "${it.key}=${it.value}" }

    companion object {

        /**
         * 从 `QQLogin` / 凭证续期响应的 `req_0.data` 解析凭证。
         * 字段名兼容 snake_case 与 camelCase（对齐 QQMusicapi `Credential.fromDict` 别名表）。
         */
        fun fromApiData(data: JSONObject): QQMusicCredential {
            val musicid = data.opt("musicid")?.toString()?.toLongOrNull()
                ?: data.optLong("musicid", 0L)
            val musickey = data.optString("musickey").orEmpty()
            val loginType = when {
                data.has("login_type") -> data.optInt("login_type", 2)
                data.has("loginType") -> data.optInt("loginType", 2)
                musickey.startsWith("W_X") -> 1
                else -> 2
            }
            return QQMusicCredential(
                musicid = musicid,
                musickey = musickey,
                refreshKey = data.optString("refresh_key")
                    .ifBlank { data.optString("refreshKey") }
                    .orEmpty(),
                openid = data.optString("openid").orEmpty(),
                accessToken = data.optString("access_token")
                    .ifBlank { data.optString("accessToken") }
                    .orEmpty(),
                refreshToken = data.optString("refresh_token")
                    .ifBlank { data.optString("refreshToken") }
                    .orEmpty(),
                unionid = data.optString("unionid").orEmpty(),
                expiredAt = data.optLong("expired_at").takeIf { it > 0L }
                    ?: data.optLong("expiredAt", 0L),
                loginType = loginType,
            )
        }

        fun fromJson(raw: String?): QQMusicCredential {
            if (raw.isNullOrBlank()) return QQMusicCredential()
            return runCatching {
                val obj = JSONObject(raw)
                QQMusicCredential(
                    musicid = obj.optLong("musicid", 0L),
                    musickey = obj.optString("musickey").orEmpty(),
                    refreshKey = obj.optString("refreshKey").orEmpty(),
                    openid = obj.optString("openid").orEmpty(),
                    accessToken = obj.optString("accessToken").orEmpty(),
                    refreshToken = obj.optString("refreshToken").orEmpty(),
                    unionid = obj.optString("unionid").orEmpty(),
                    expiredAt = obj.optLong("expiredAt", 0L),
                    loginType = obj.optInt("loginType", 2),
                )
            }.getOrElse { QQMusicCredential() }
        }
    }

    fun toJson(): String = JSONObject().apply {
        put("musicid", musicid)
        put("musickey", musickey)
        put("refreshKey", refreshKey)
        put("openid", openid)
        put("accessToken", accessToken)
        put("refreshToken", refreshToken)
        put("unionid", unionid)
        put("expiredAt", expiredAt)
        put("loginType", loginType)
    }.toString()
}

/**
 * QQ音乐登录态加密持久化（对齐 `KugouCookieStore`）。
 * 单例由 AppContainer 提供；登录成功后由 `QQMusicSession` 调用 [save]。
 */
class QQMusicCookieStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { error ->
        NPLogger.e("QQMusicCookieStore", "failed to open encrypted prefs, fallback to plain", error)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    private val _credentialFlow: MutableStateFlow<QQMusicCredential> =
        MutableStateFlow(loadFromPrefs())

    val credentialFlow: StateFlow<QQMusicCredential> = _credentialFlow.asStateFlow()

    val credential: QQMusicCredential
        get() = _credentialFlow.value

    val isLoggedIn: Boolean
        get() = credential.isLoggedIn

    fun save(credential: QQMusicCredential) {
        runCatching {
            prefs.edit().putString(KEY_CREDENTIAL, credential.toJson()).apply()
        }.onFailure { error ->
            NPLogger.e("QQMusicCookieStore", "failed to persist qq music credential", error)
        }
        _credentialFlow.value = credential
        NPLogger.d(
            "QQMusicCookieStore",
            "qq music credential saved: musicid=${credential.musicid} loginType=${credential.loginType}"
        )
    }

    fun clear() {
        runCatching { prefs.edit().remove(KEY_CREDENTIAL).apply() }
        _credentialFlow.value = QQMusicCredential()
    }

    private fun loadFromPrefs(): QQMusicCredential {
        val raw = runCatching { prefs.getString(KEY_CREDENTIAL, null) }.getOrNull()
        return QQMusicCredential.fromJson(raw)
    }

    private companion object {
        const val FILE_NAME = "qqmusic_auth"
        const val KEY_CREDENTIAL = "credential"
    }
}
