package moe.ouom.neriplayer.data.auth.kugou

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.ouom.neriplayer.core.logging.NPLogger
import org.json.JSONObject

/**
 * 酷狗概念版登录态持久化：将 SDK CookieJar 的快照（token/userid/dfid/vip_token 等）加密存到本地。
 * 单例由 AppContainer 提供；登录成功后 [KugouSession] 调用 [save] 落盘。
 */
class KugouCookieStore(context: Context) {

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
        NPLogger.e("KugouCookieStore", "failed to open encrypted prefs, fallback to plain", error)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    private val _cookieFlow: MutableStateFlow<Map<String, String>> = MutableStateFlow(loadFromPrefs())

    val cookieFlow: StateFlow<Map<String, String>> = _cookieFlow.asStateFlow()

    /** 是否具备登录态：token 非空且 userid 非 0（与 SDK CookieJar.isLoggedIn 口径一致）。 */
    val isLoggedIn: Boolean
        get() = isLoggedInCookies(_cookieFlow.value)

    fun getCookiesOnce(): Map<String, String> = _cookieFlow.value

    fun save(cookies: Map<String, String>) {
        val filtered = cookies.filter { !it.value.isNullOrBlank() }
        runCatching {
            prefs.edit().putString(KEY_JAR, toJson(filtered)).apply()
        }.onFailure { error ->
            NPLogger.e("KugouCookieStore", "failed to persist kugou cookies", error)
        }
        _cookieFlow.value = filtered
        NPLogger.d("KugouCookieStore", "kugou cookies saved: keys=${filtered.keys.joinToString()}")
    }

    fun clear() {
        runCatching { prefs.edit().remove(KEY_JAR).apply() }
        _cookieFlow.value = emptyMap()
    }

    private fun loadFromPrefs(): Map<String, String> {
        val raw = runCatching { prefs.getString(KEY_JAR, null).orEmpty() }.getOrElse { "" }
        if (raw.isBlank()) return emptyMap()
        return runCatching { fromJson(raw) }.getOrElse { error ->
            NPLogger.w("KugouCookieStore", "failed to parse kugou cookies", error)
            emptyMap()
        }
    }

    private fun toJson(map: Map<String, String>): String = JSONObject(map).toString()

    private fun fromJson(raw: String): Map<String, String> {
        val obj = JSONObject(raw)
        return obj.keys().asSequence().associateWith { key -> obj.optString(key) }
    }

    private companion object {
        const val FILE_NAME = "kugou_auth"
        const val KEY_JAR = "cookie_jar"

        fun isLoggedInCookies(cookies: Map<String, String>): Boolean {
            return !cookies["token"].isNullOrBlank() && cookies["userid"] != "0"
        }
    }
}