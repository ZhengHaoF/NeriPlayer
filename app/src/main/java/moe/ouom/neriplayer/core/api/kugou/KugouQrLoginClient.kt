package moe.ouom.neriplayer.core.api.kugou

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.logging.NPLogger

/** 酷狗概念版 QR 登录会话（二维码 key + 二维码图片 base64）。 */
data class KugouQrLoginSession(
    val key: String,
    val qrCodeUrl: String,
    val qrImageBase64: String?
)

/** 酷狗概念版 QR 轮询状态。 */
enum class KugouQrLoginStatus {
    WAITING,
    SCANNED,
    SUCCESS,
    EXPIRED,
    ERROR
}

/**
 * 酷狗概念版二维码登录客户端：
 * - [createSession] 创建二维码（返回 base64 图片，可直接展示）；
 * - [check] 轮询扫码状态；成功后返回登录 cookie（token/userid 等，源自 Set-Cookie）。
 */
class KugouQrLoginClient(private val session: KugouSession) {

    suspend fun createSession(): KugouQrLoginSession {
        session.ensureDeviceRegistered()
        val response = session.client.auth.createQrKey()
        if (response.body["status"]?.jsonPrimitive?.int != 1) {
            throw IllegalStateException("createQrKey rejected: ${response.body}")
        }
        val data = response.body["data"]?.jsonObject
        val key = data?.get("qrcode")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("createQrKey missing qrcode: ${response.body}")
        val qrImageBase64 = data?.get("qrcode_img")?.jsonPrimitive?.contentOrNull
        NPLogger.d(
            "KugouQrLogin",
            "session created key=${key.take(4)}... imageBytes=${qrImageBase64?.length ?: 0}"
        )
        return KugouQrLoginSession(
            key = key,
            qrCodeUrl = session.client.auth.createQrCodeUrl(key),
            qrImageBase64 = qrImageBase64
        )
    }

    /** 轮询一次扫码状态；SUCCESS 时附上登录凭证（token/userid 等，取自响应 data）。 */
    suspend fun check(
        loginSession: KugouQrLoginSession
    ): Pair<KugouQrLoginStatus, Map<String, String>> {
        val response = session.client.auth.checkQrCode(key = loginSession.key)
        val data = response.body["data"]?.jsonObject
        val status = data?.get("status")?.jsonPrimitive?.int ?: 0
        val bizStatus = response.body["status"]?.jsonPrimitive?.int
        NPLogger.d(
            "KugouQrLogin",
            "check status=$status biz=$bizStatus cookieKeys=${response.cookies.keys}"
        )
        if (bizStatus != 1) {
            return KugouQrLoginStatus.ERROR to emptyMap()
        }
        return when (status) {
            // 酷狗概念版轮询语义（对齐 KuGouMusicApi login_qr_check）：
            // 0=过期，1=等待扫码，2=已扫码待确认，4=授权登录成功（data 内携带 token/userid）
            0 -> KugouQrLoginStatus.EXPIRED to emptyMap()
            2 -> KugouQrLoginStatus.SCANNED to emptyMap()
            4 -> {
                val vipToken = data?.get("vip_token")?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() && it != "0" }
                val loginCookies = buildMap {
                    data?.get("token")?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("token", it) }
                    data?.get("userid")?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("userid", it) }
                    data?.get("vip_type")?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("vip_type", it) }
                    vipToken?.let { put("vip_token", it) }
                }
                KugouQrLoginStatus.SUCCESS to loginCookies
            }
            else -> KugouQrLoginStatus.WAITING to emptyMap()
        }
    }
}