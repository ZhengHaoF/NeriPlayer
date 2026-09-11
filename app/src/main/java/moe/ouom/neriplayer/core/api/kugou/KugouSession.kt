package moe.ouom.neriplayer.core.api.kugou

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.auth.kugou.KugouCookieStore
import top.ghhccghk.multiplatform.kugouapi.KuGouClient

/**
 * 酷狗会话：持有 KuGouApi_Kotlin_SDK 客户端，首次使用时注册设备身份，并管理登录态（token 等）持久化。
 *
 * - 设备注册幂等且线程安全；
 * - 登录态经 [KugouCookieStore] 持久化，启动时恢复，登录/退出时同步落盘。
 * 全局单例由 AppContainer 提供。
 */
class KugouSession(
    private val cookieStore: KugouCookieStore
) {

    val client: KuGouClient by lazy {
        // 注意：lazy 初始化期间不能再访问 this.client（会触发无限递归），
        // 直接操作新建的临时 client 注入持久化登录态。
        KuGouClient().also { restored ->
            applyCookiesTo(restored, cookieStore.getCookiesOnce())
        }
    }

    /** 是否已登录（token 非空且 userid 非 0）。 */
    val isLoggedIn: Boolean
        get() = client.cookieJar.isLoggedIn()

    private val _loggedInFlow = MutableStateFlow(cookieStore.isLoggedIn)
    /** 登录状态流（登录/退出后自动更新），供设置页等 UI 观察。 */
    val loggedInFlow: StateFlow<Boolean> = _loggedInFlow.asStateFlow()

    /** 应用一份 cookie 快照到 SDK CookieJar，可选落盘。 */
    fun applyCookies(cookies: Map<String, String>, persist: Boolean = true) {
        applyCookiesTo(client, cookies)
        if (persist) {
            persistCookies()
        }
        _loggedInFlow.value = cookieStore.isLoggedIn
    }

    /** 将 cookie 快照注入指定 client 的 CookieJar（lazy 初始化时使用，避免访问 this.client）。 */
    private fun applyCookiesTo(target: KuGouClient, cookies: Map<String, String>) {
        if (cookies.isEmpty()) return
        val jar = target.cookieJar
        cookies["token"]?.takeIf { it.isNotBlank() }?.let { jar.setToken(it) }
        cookies["userid"]?.takeIf { it.isNotBlank() }?.let { jar.setUserid(it.toLongOrNull() ?: 0L) }
        cookies["dfid"]?.takeIf { it.isNotBlank() }?.let { jar.setDfid(it) }
        cookies["KUGOU_API_MID"]?.takeIf { it.isNotBlank() }?.let { jar.setMid(it) }
        cookies["KUGOU_API_GUID"]?.takeIf { it.isNotBlank() }?.let { jar.setGuid(it) }
        cookies["KUGOU_API_DEV"]?.takeIf { it.isNotBlank() }?.let { jar.setDev(it) }
        cookies["KUGOU_API_MAC"]?.takeIf { it.isNotBlank() }?.let { jar.setMac(it) }
        cookies["KUGOU_API_WEBGL"]?.takeIf { it.isNotBlank() }?.let { jar.setWebGLHash(it) }
        cookies["vip_token"]?.takeIf { it.isNotBlank() }?.let { jar.setVipToken(it) }
        cookies["vip_type"]?.takeIf { it.isNotBlank() }?.let { jar.set("vip_type", it) }
        cookies["auth"]?.takeIf { it.isNotBlank() }?.let { jar.setAuth(it) }
    }

    /** 将当前 CookieJar 快照落盘。 */
    fun persistCookies() {
        cookieStore.save(client.cookieJar.getAll())
    }

    /** 退出登录：清空本地登录态（设备身份 dfid/mid 保留）。 */
    fun logout() {
        cookieStore.clear()
        // clear 后恢复设备身份，避免重新注册
        client.cookieJar.setToken("")
        client.cookieJar.setUserid(0L)
        client.cookieJar.setVipToken("")
        client.cookieJar.setAuth("")
        _loggedInFlow.value = false
    }

    private val deviceRegistrationMutex = Mutex()

    @Volatile
    private var deviceRegistered = false

    /** 确保设备已注册（幂等）。所有依赖设备身份的请求前调用一次。 */
    suspend fun ensureDeviceRegistered() {
        if (deviceRegistered) return
        deviceRegistrationMutex.withLock {
            if (deviceRegistered) return
            try {
                val response = client.auth.registerDev()
                val bizStatus = response.body["status"]?.jsonPrimitive?.int ?: 0
                if (bizStatus == 1) {
                    deviceRegistered = true
                    NPLogger.d(
                        "KugouSession",
                        "device registered: ${response.body["data"].toString().take(160)}"
                    )
                } else {
                    NPLogger.w("KugouSession", "registerDev rejected: ${response.body}")
                }
            } catch (error: Exception) {
                NPLogger.e("KugouSession", "registerDev error", error)
                throw error
            }
        }
    }
}