package moe.ouom.neriplayer.core.api.qqmusic

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import okhttp3.OkHttpClient

/** QQ音乐匿名实测可取的最高档（MP3 128）。 */
const val QQ_MUSIC_FREE_QUALITY = "M500"

/** QQ音乐匿名兜底档（AAC 96）：部分曲目不下发 M500 时回落到它。 */
const val QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY = "C400"

/** QQ音乐 web 平台参数：与现有搜索链路同源，匿名可用且**不需要 QIMEI**。 */
internal const val QQMUSIC_PLATFORM = "20"
internal const val QQMUSIC_COMM_CT = 24
internal const val QQMUSIC_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
internal const val QQMUSIC_REFERER = "https://y.qq.com"
internal const val QQMUSIC_API_HOST = "https://u.y.qq.com/cgi-bin/musicu.fcg"

/**
 * QQ音乐会话：持有网络客户端与设备标识 guid。
 *
 * 匿名播放走 web 协议（`platform=20` / `ct=24`），与 [moe.ouom.neriplayer.core.api.search.QQMusicSearchApi]
 * 同源，因此本会话在登录功能落地前不承担认证职责，只负责把 guid 稳定下来 ——
 * 固定的设备标识可避免每次请求随机导致的画像漂移与风控误判。
 *
 * 登录（QR 扫码）落地后，QIMEI 注册与 session 会并入本类，见 `docs/接入QQ音乐-实施方案.md` 阶段 4a。
 */
class QQMusicSession(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val client: OkHttpClient by lazy { AppContainer.sharedOkHttpClient }

    /** 设备标识：安装后生成一次并持久化复用，退出登录也不重置。 */
    val guid: String by lazy { loadOrCreateGuid() }

    private fun loadOrCreateGuid(): String {
        val existing = runCatching { prefs.getString(KEY_GUID, null) }.getOrNull()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString().replace("-", "")
        runCatching { prefs.edit().putString(KEY_GUID, generated).apply() }
            .onFailure { NPLogger.w(TAG, "failed to persist guid: ${it.message}") }
        NPLogger.d(TAG, "generated new QQ Music guid")
        return generated
    }

    private companion object {
        const val TAG = "QQMusicSession"
        const val PREFS_NAME = "qqmusic_session"
        const val KEY_GUID = "guid"
    }
}
