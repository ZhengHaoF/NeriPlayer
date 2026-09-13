package moe.ouom.neriplayer.core.api.qqmusic

import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.auth.qqmusic.QQMusicCredential
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "QQMusicQrLogin"

/** ptlogin2 / QQ Connect 的浏览器 UA（对齐 Web 侧扫码页）。 */
private const val PTLOGIN_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

private const val APP_ID = "716027609"
private const val DAID = "383"
private const val PT_THIRD_AID = "100497308"

enum class QQMusicQrLoginStatus {
    WAITING,
    SCANNED,
    SUCCESS,
    EXPIRED,
    ERROR,
}

data class QQMusicQrTicket(
    val qrImage: ByteArray,
    val qrsig: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QQMusicQrTicket) return false
        return qrsig == other.qrsig && qrImage.contentEquals(other.qrImage)
    }

    override fun hashCode(): Int = 31 * qrsig.hashCode() + qrImage.contentHashCode()
}

/**
 * QQ 扫码登录客户端（路线 A，对齐 QQMusicapi `login.js`）。
 *
 * Cookie 按蓝本**手动**管理（不依赖 CookieJar 域匹配）：
 * ptqrshow 的 Set-Cookie 域是 `ptlogin2.qq.com`，请求却打到 `ssl.ptlogin2.qq.com`，
 * 自动匹配在部分 OkHttp 版本上不可靠。
 *
 * 链路：ptqrshow → ptqrlogin 轮询 → check_sig → authorize → QQLogin。
 */
class QQMusicQrLoginClient(
    private val client: OkHttpClient,
    private val device: QQMusicDevice,
) {
    /** 本登录会话内的 cookie 名 → 值（与蓝本 options.cookies 同语义）。 */
    private val cookies = linkedMapOf<String, String>()

    private val cookieLock = Any()

    /**
     * check_sig / authorize 必须禁跟随重定向：成功时是 302，
     * code 在 Location 里；跟随后 Location 丢失。共享 client 默认会跟。
     */
    private val noRedirectClient: OkHttpClient by lazy {
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    /** 取登录二维码（PNG 字节 + qrsig）。 */
    suspend fun createTicket(): QQMusicQrTicket = withContext(Dispatchers.IO) {
        synchronized(cookieLock) { cookies.clear() }
        val url = "https://ssl.ptlogin2.qq.com/ptqrshow".toHttpUrl().newBuilder()
            .addQueryParameter("appid", APP_ID)
            .addQueryParameter("e", "2")
            .addQueryParameter("l", "M")
            .addQueryParameter("s", "3")
            .addQueryParameter("d", "72")
            .addQueryParameter("v", "4")
            .addQueryParameter("t", Math.random().toString())
            .addQueryParameter("daid", DAID)
            .addQueryParameter("pt_3rd_aid", PT_THIRD_AID)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", PTLOGIN_UA)
            .header("Referer", "https://xui.ptlogin2.qq.com/")
            .build()

        val (bytes, setCookieHeaders) = client.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("ptqrshow HTTP ${response.code}")
            response.body.bytes() to response.headers("Set-Cookie")
        }
        absorbSetCookies(setCookieHeaders)
        val qrsig = synchronized(cookieLock) { cookies["qrsig"] }.orEmpty()
        if (qrsig.isBlank()) {
            NPLogger.w(TAG, "ptqrshow no qrsig: setCookie=$setCookieHeaders")
            throw IllegalStateException("ptqrshow missing qrsig cookie")
        }
        NPLogger.d(TAG, "qr ticket created, imageBytes=${bytes.size} qrsigLen=${qrsig.length}")
        QQMusicQrTicket(qrImage = bytes, qrsig = qrsig)
    }

    /**
     * 轮询一次扫码状态。SUCCESS 时继续完成 check_sig → authorize → QQLogin 并返回凭证。
     */
    suspend fun poll(
        ticket: QQMusicQrTicket
    ): Pair<QQMusicQrLoginStatus, QQMusicCredential?> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val url = "https://ssl.ptlogin2.qq.com/ptqrlogin".toHttpUrl().newBuilder()
            .addQueryParameter("u1", "https://graph.qq.com/oauth2.0/login_jump")
            .addQueryParameter("ptqrtoken", QQMusicCrypto.hash33(ticket.qrsig).toString())
            .addQueryParameter("ptredirect", "0")
            .addQueryParameter("h", "1")
            .addQueryParameter("t", "1")
            .addQueryParameter("g", "1")
            .addQueryParameter("from_ui", "1")
            .addQueryParameter("ptlang", "2052")
            .addQueryParameter("action", "0-0-$now")
            .addQueryParameter("js_ver", "20102616")
            .addQueryParameter("js_type", "1")
            .addQueryParameter("pt_uistyle", "40")
            .addQueryParameter("aid", APP_ID)
            .addQueryParameter("daid", DAID)
            .addQueryParameter("pt_3rd_aid", PT_THIRD_AID)
            .addQueryParameter("has_onekey", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", PTLOGIN_UA)
            .header("Referer", "https://xui.ptlogin2.qq.com/")
            .header("Cookie", cookieHeader("qrsig"))
            .build()

        val (text, setCookieHeaders) = client.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("ptqrlogin HTTP ${response.code}")
            response.body.string() to response.headers("Set-Cookie")
        }
        absorbSetCookies(setCookieHeaders)

        val args = parsePtuiArgs(text)
            ?: run {
                NPLogger.w(TAG, "ptqrlogin unparsable: ${text.take(160)}")
                return@withContext QQMusicQrLoginStatus.ERROR to null
            }
        val code = args.getOrNull(0)?.toIntOrNull()
            ?: return@withContext QQMusicQrLoginStatus.ERROR to null
        NPLogger.d(TAG, "ptqrlogin code=$code args=${args.take(3)}")
        // 对齐 QQMusicapi / QQMusicApi：0=DONE 65=TIMEOUT 66=SCAN 67=CONF 68=REFUSE
        when (code) {
            0 -> {
                val location = args.getOrNull(2).orEmpty()
                val sigx = QUERY_SIGX.find(location)?.groupValues?.get(1)
                val uin = QUERY_UIN.find(location)?.groupValues?.get(1)
                if (sigx.isNullOrBlank() || uin.isNullOrBlank()) {
                    NPLogger.w(TAG, "success response missing sigx/uin: $location")
                    return@withContext QQMusicQrLoginStatus.ERROR to null
                }
                NPLogger.d(TAG, "qr confirmed, starting authorize uin=$uin")
                val credential = authorize(uin = uin, sigx = sigx)
                return@withContext QQMusicQrLoginStatus.SUCCESS to credential
            }
            65 -> return@withContext QQMusicQrLoginStatus.EXPIRED to null
            66 -> return@withContext QQMusicQrLoginStatus.WAITING to null
            67 -> return@withContext QQMusicQrLoginStatus.SCANNED to null
            68 -> return@withContext QQMusicQrLoginStatus.EXPIRED to null
            else -> return@withContext QQMusicQrLoginStatus.WAITING to null
        }
    }

    private suspend fun authorize(uin: String, sigx: String): QQMusicCredential {
        val checkSigCookies = checkSig(uin = uin, sigx = sigx)
        val pSkey = listOf("p_skey", "p-skey", "pskey", "ptsigx", "skey")
            .firstNotNullOfOrNull { checkSigCookies[it] }
            ?: error("check_sig cookies missing p_skey")
        val code = authorizeCode(checkSigCookies = checkSigCookies, pSkey = pSkey)
        return qqLogin(code = code)
    }

    private suspend fun checkSig(uin: String, sigx: String): Map<String, String> {
        val url = "https://ssl.ptlogin2.graph.qq.com/check_sig".toHttpUrl().newBuilder()
            .addQueryParameter("uin", uin)
            .addQueryParameter("pttype", "1")
            .addQueryParameter("service", "ptqrlogin")
            .addQueryParameter("nodirect", "0")
            .addQueryParameter("ptsigx", sigx)
            .addQueryParameter("s_url", "https://graph.qq.com/oauth2.0/login_jump")
            .addQueryParameter("ptlang", "2052")
            .addQueryParameter("ptredirect", "100")
            .addQueryParameter("aid", APP_ID)
            .addQueryParameter("daid", DAID)
            .addQueryParameter("j_later", "0")
            .addQueryParameter("low_login_hour", "0")
            .addQueryParameter("regmaster", "0")
            .addQueryParameter("pt_login_type", "3")
            .addQueryParameter("pt_aid", "0")
            .addQueryParameter("pt_aaid", "16")
            .addQueryParameter("pt_light", "0")
            .addQueryParameter("pt_3rd_aid", PT_THIRD_AID)
            .build()
        // 对齐蓝本：check_sig 不带业务 Cookie，p_skey 只从本响应 Set-Cookie 取
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", PTLOGIN_UA)
            .header("Referer", "https://xui.ptlogin2.qq.com/")
            .build()

        val checkResult = noRedirectClient.newCall(request)
            .awaitResponse { response ->
                Quad(
                    response.code,
                    response.header("Location").orEmpty(),
                    response.headers("Set-Cookie"),
                    runCatching { response.peekBody(400).string() }.getOrDefault("")
                )
            }
        val status = checkResult.a
        val location = checkResult.b
        val setCookieHeaders = checkResult.c
        val bodyPreview = checkResult.d
        val fromResponse = parseSetCookiePairs(setCookieHeaders)
        NPLogger.d(
            TAG,
            "check_sig status=$status location=${location.take(120)} " +
                "setCookieNames=${fromResponse.keys} body=${bodyPreview.take(120)}"
        )

        val pSkey = listOf("p_skey", "p-skey", "pskey", "ptsigx", "skey")
            .firstNotNullOfOrNull { name -> fromResponse[name]?.takeIf { it.isNotBlank() } }
        if (pSkey == null) {
            throw IllegalStateException(
                "check_sig missing p_skey: status=$status " +
                    "setCookie=${setCookieHeaders.joinToString(" || ")} " +
                    "location=${location.take(160)} body=${bodyPreview.take(160)}"
            )
        }
        // 存起来，authorize 阶段按蓝本只带 check_sig 返回的 cookie
        synchronized(cookieLock) {
            cookies.putAll(fromResponse)
        }
        return fromResponse
    }

    private suspend fun authorizeCode(checkSigCookies: Map<String, String>, pSkey: String): String {
        val form = FormBody.Builder()
            .add("response_type", "code")
            .add("client_id", PT_THIRD_AID)
            .add(
                "redirect_uri",
                "https://y.qq.com/portal/wx_redirect.html?login_type=1&surl=https://y.qq.com/"
            )
            .add("scope", "get_user_info,get_app_friends")
            .add("state", "state")
            .add("switch", "")
            .add("from_ptlogin", "1")
            .add("src", "1")
            .add("update_auth", "1")
            .add("openapi", "1010_1030")
            .add("g_tk", QQMusicCrypto.hash33(pSkey, 5381).toString())
            .add("auth_time", System.currentTimeMillis().toString())
            .add("ui", UUID.randomUUID().toString())
            .build()
        val request = Request.Builder()
            .url("https://graph.qq.com/oauth2.0/authorize")
            .header("User-Agent", PTLOGIN_UA)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Referer", "https://xui.ptlogin2.qq.com/")
            .header("Cookie", checkSigCookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
            .post(form)
            .build()

        val (status, location, setCookieHeaders) = noRedirectClient.newCall(request)
            .awaitResponse { response ->
                Triple(
                    response.code,
                    response.header("Location").orEmpty(),
                    response.headers("Set-Cookie")
                )
            }
        absorbSetCookies(setCookieHeaders)
        val code = QUERY_CODE.find(location)?.groupValues?.get(1)
        if (code.isNullOrBlank()) {
            NPLogger.w(
                TAG,
                "authorize missing code: status=$status location=$location setCookie=$setCookieHeaders"
            )
            throw IllegalStateException("authorize missing code: status=$status location=$location")
        }
        NPLogger.d(TAG, "authorize got code from status=$status")
        return code
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private suspend fun qqLogin(code: String): QQMusicCredential {
        device.ensureReady()
        val payload = JSONObject()
            .put("comm", device.buildAndroidComm(null).put("tmeLoginType", 2))
            .put(
                "req_0",
                JSONObject()
                    .put("module", "QQConnectLogin.LoginServer")
                    .put("method", "QQLogin")
                    .put("param", JSONObject().put("code", code))
            )

        // Android 平台协议（ct=11）要求 POST + JSON body（对齐 QQMusicapi requestApi），
        // 不能像 web 平台那样用 GET + data 查询参数，否则服务端全局返回 code=500001。
        val url = "https://u.y.qq.com/cgi-bin/musicu.fcg".toHttpUrl()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", device.androidUserAgent())
            .header("Referer", "https://y.qq.com")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val responseText = client.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("QQLogin HTTP ${response.code}")
            response.body.string()
        }

        val root = JSONObject(responseText)
        if (root.optInt("code", 0) != 0) {
            throw IllegalStateException(
                "QQLogin global code=${root.optInt("code")}: ${responseText.take(200)}"
            )
        }
        val req0 = root.optJSONObject("req_0")
        val bizCode = req0?.optInt("code", -1) ?: -1
        val data = req0?.optJSONObject("data") ?: JSONObject()
        if (bizCode != 0) {
            throw IllegalStateException("QQLogin req0 code=$bizCode: ${responseText.take(200)}")
        }

        val credential = QQMusicCredential.fromApiData(data)
        if (!credential.isLoggedIn) {
            throw IllegalStateException(
                "QQLogin missing musicid/musickey: ${responseText.take(200)}"
            )
        }
        NPLogger.d(
            TAG,
            "QQLogin success: musicid=${credential.musicid} loginType=${credential.loginType}"
        )
        return credential
    }

    /** 只保留本会话关注的 cookie（蓝本 ptqrlogin 只带 qrsig）。 */
    private fun cookieHeader(vararg only: String): String {
        val snapshot = synchronized(cookieLock) { cookies.toMap() }
        val source = if (only.isEmpty()) snapshot else only.mapNotNull { k ->
            snapshot[k]?.let { k to it }
        }.toMap()
        return source.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun parseSetCookiePairs(headers: List<String>): Map<String, String> {
        val out = linkedMapOf<String, String>()
        for (header in headers) {
            val pair = header.substringBefore(';')
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val name = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (name.isNotBlank() && value.isNotBlank()) {
                out[name] = value
            }
        }
        return out
    }

    private fun absorbSetCookies(headers: List<String>) {
        for ((name, value) in parseSetCookiePairs(headers)) {
            synchronized(cookieLock) {
                cookies[name] = value
            }
        }
    }

    private fun parsePtuiArgs(text: String): List<String>? {
        val body = PTUI_CB.find(text)?.groupValues?.get(1) ?: return null
        return PTUI_ARGS.findAll(body)
            .map { it.groupValues[1].replace("\\'", "'") }
            .toList()
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        val PTUI_CB = Regex("""ptuiCB\((.*?)\)""")
        val PTUI_ARGS = Regex("""'((?:\\.|[^'])*)'""")
        val QUERY_SIGX = Regex("""(?:\?|&)ptsigx=(.+?)&s_url""")
        val QUERY_UIN = Regex("""(?:\?|&)uin=(.+?)&service""")
        val QUERY_CODE = Regex("""code=([^&]+)""")
    }
}
