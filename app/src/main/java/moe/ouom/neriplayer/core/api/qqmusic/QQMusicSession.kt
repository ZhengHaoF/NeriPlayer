package moe.ouom.neriplayer.core.api.qqmusic

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.auth.qqmusic.QQMusicCookieStore
import moe.ouom.neriplayer.data.auth.qqmusic.QQMusicCredential
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
/** DissInfo 等接口要求 Referer 带尾斜杠，否则 `invalid referer`。 */
internal const val QQMUSIC_REFERER = "https://y.qq.com/"
internal const val QQMUSIC_API_HOST = "https://u.y.qq.com/cgi-bin/musicu.fcg"

/**
 * QQ音乐会话：持有网络客户端、设备标识 guid、QIMEI 设备身份与登录态。
 *
 * - 匿名播放走 web 协议（`platform=20` / `ct=24`），不需要 QIMEI；
 * - 登录（QR 扫码）走 Android 协议，经 [QQMusicDevice] 完成 QIMEI 注册 + getSession；
 * - 凭证经 [QQMusicCookieStore] 加密持久化，退出登录时保留 guid / QIMEI / session。
 */
class QQMusicSession(
    context: Context,
    private val cookieStore: QQMusicCookieStore = QQMusicCookieStore(context),
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val client: OkHttpClient by lazy { AppContainer.sharedOkHttpClient }

    /** 设备标识：安装后生成一次并持久化复用，退出登录也不重置。 */
    val guid: String by lazy { loadOrCreateGuid() }

    /** QIMEI + session 设备身份（登录链路强制前置），与登录态解耦。 */
    val device: QQMusicDevice by lazy {
        QQMusicDevice(
            context = context.applicationContext,
            client = client,
            guidProvider = { guid },
        )
    }

    private val _loggedInFlow = MutableStateFlow(cookieStore.isLoggedIn)

    /** 取址 QPS 闸门：音质降级链 + 外层重试可能连发 `CgiGetVkey`，需串行限速。 */
    internal val vkeyRateLimiter = QQMusicVkeyRateLimiter()

    /** 续期并发串行化：避免多个播放请求同时触发续期。 */
    private val refreshMutex = Mutex()

    /** 上次续期尝试时间（含失败，节流用）。内存态即可：进程重启后重试一次无妨。 */
    private var lastRefreshAttemptAtMs = 0L

    /** 登录状态流（登录/退出后自动更新），供设置页等 UI 观察。 */
    val loggedInFlow: StateFlow<Boolean> = _loggedInFlow.asStateFlow()

    val isLoggedIn: Boolean
        get() = cookieStore.isLoggedIn

    val credential: QQMusicCredential
        get() = cookieStore.credential

    /** 登录成功后写入凭证并广播登录态。 */
    fun applyCredential(credential: QQMusicCredential) {
        cookieStore.save(credential)
        _loggedInFlow.value = true
    }

    /** 退出登录：清空登录态；guid / QIMEI / session 设备身份保留。 */
    fun logout() {
        cookieStore.clear()
        _loggedInFlow.value = false
    }

    /**
     * 维持登录态：以 `refreshKey` 续期凭证（`music.login.LoginServer / Login`，`loginMode: 2`）。
     *
     * 语义（对齐实施方案 §8 阶段 4b「失败降级匿名态」）：
     * - 成功 → 应用新凭证并返回 true；
     * - 服务端判定鉴权过期（1000 / 104401 / 104400）→ 登出，UI 经 [loggedInFlow] 回到匿名态；
     * - 瞬时错误（网络 / 限流）→ 保留现凭证，返回 false。
     *
     * @param minIntervalMs 距上次尝试的最小间隔（节流，防止高频打续期接口）
     */
    suspend fun refreshCredentialIfNeeded(
        minIntervalMs: Long = DEFAULT_CREDENTIAL_REFRESH_INTERVAL_MS
    ): Boolean {
        if (!isLoggedIn) return false
        refreshMutex.withLock {
            if (!isLoggedIn) return false
            val now = System.currentTimeMillis()
            if (now - lastRefreshAttemptAtMs < minIntervalMs) return false
            lastRefreshAttemptAtMs = now
            val result = runCatching { refreshQQMusicCredential() }
                .onFailure { NPLogger.w(TAG, "credential refresh crashed: ${it.message}") }
                .getOrElse { QQMusicRefreshResult.TransientError }
            return when (result) {
                is QQMusicRefreshResult.Renewed -> {
                    applyCredential(result.credential)
                    true
                }
                QQMusicRefreshResult.Rejected -> {
                    NPLogger.w(TAG, "credential rejected, degrading to anonymous state")
                    logout()
                    false
                }
                QQMusicRefreshResult.TransientError -> false
            }
        }
    }

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

        /** 常规维持节流：每日至多一次主动续期。 */
        const val DEFAULT_CREDENTIAL_REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
