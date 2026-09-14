package moe.ouom.neriplayer.core.api.qqmusic

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.player.model.PlaybackAudioInfo
import moe.ouom.neriplayer.core.player.model.PlaybackAudioSource
import moe.ouom.neriplayer.core.player.model.SongUrlResult
import moe.ouom.neriplayer.core.player.model.deriveCodecLabel
import moe.ouom.neriplayer.core.player.url.buildQQMusicQualityCandidates
import moe.ouom.neriplayer.core.player.url.buildQQMusicQualityOptions
import moe.ouom.neriplayer.core.player.url.qqMusicQualityLabel
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.util.network.awaitResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** QQ音乐取址结果：失败原因需区分「需会员」与其他错误，故不复用 [SongUrlResult]。 */
internal sealed class QQMusicResolveOutcome {
    data class Playable(val success: SongUrlResult.Success) : QQMusicResolveOutcome()

    /** 服务端返回 `result=104003`：曲目需会员或属数字专辑，匿名态取不到直链。 */
    data object RequiresVip : QQMusicResolveOutcome()

    /** 其他失败：结构异常、无可用直链、网络错误。 */
    data object Unavailable : QQMusicResolveOutcome()
}

/** 档位码 → 取址 filename 所需的文件扩展名。 */
internal fun qqMusicFileExtension(quality: String): String = when (quality.uppercase()) {
    "F000", "AI00", "Q000", "Q001", "TL01" -> "flac"
    "O400", "O600", "O800", "Q003" -> "ogg"
    "M500", "M800" -> "mp3"
    "C200", "C400", "C600" -> "m4a"
    "D004", "DT03" -> "mp4"
    else -> "mp3"
}

private fun qqMusicMimeType(quality: String): String? = when (qqMusicFileExtension(quality)) {
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    "flac" -> "audio/flac"
    "ogg" -> "audio/ogg"
    "mp4" -> "audio/mp4"
    else -> null
}

/** vkey 响应中的业务错误码。 */
private const val QQMUSIC_RESULT_OK = 0
private const val QQMUSIC_RESULT_VIP_REQUIRED = 104003

/**
 * 解析 QQ音乐播放地址（`vkey.GetVkeyServer` / `CgiGetVkey`）。
 *
 * 按 [quality] 起逐档降级，首个可播档位即结果。未登录时调用方只应传入免费档
 * （[QQ_MUSIC_FREE_QUALITY] / [QQ_MUSIC_ANONYMOUS_FALLBACK_QUALITY]），
 * 避免必然被拒的会员档请求。
 *
 * 响应为 `req_0.data.midurlinfo[0]`：
 * - `purl` 非空 → 直链 = `sip[0] + purl`（`purl` 自带协议时直接采用）；
 * - `purl` 为空 → 由 `result` 判定原因（104003 = 需会员）。
 */
internal suspend fun QQMusicSession.resolveQQMusicPlaybackUrl(
    song: SongItem,
    quality: String = QQ_MUSIC_FREE_QUALITY,
    getLocalizedString: (Int) -> String
): QQMusicResolveOutcome {
    val songMid = song.audioId?.trim().orEmpty()
    if (songMid.isBlank()) {
        NPLogger.w(TAG, "QQ Music song missing songmid: song=${song.name}")
        return QQMusicResolveOutcome.Unavailable
    }
    // filename 的 mid 段优先用 media_mid；匿名档实测用 songmid 也能取到直链，故缺失时回退
    val filenameMid = song.subAudioId?.trim()?.takeIf { it.isNotBlank() } ?: songMid

    // 登录态维持：节流主动续期，避免 musickey 静默过期后长时间降级
    if (isLoggedIn) {
        runCatching { refreshCredentialIfNeeded() }
            .onFailure { NPLogger.w(TAG, "credential refresh failed: ${it.message}") }
    }

    val outcome = resolveViaQualityChain(songMid, filenameMid, quality, getLocalizedString)
    if (outcome !is QQMusicResolveOutcome.Unavailable || !isLoggedIn) return outcome

    // 全链路失败且非「需会员」：会话可能已在两次启动之间失效（失败码不确定，无法按码识别）。
    // 短节流续期一次后重试整条降级链；续期被拒则已降级匿名态，重试无意义。
    val renewed = runCatching {
        refreshCredentialIfNeeded(minIntervalMs = RETRY_REFRESH_INTERVAL_MS)
    }.getOrDefault(false)
    if (!renewed) return outcome
    NPLogger.d(TAG, "credential renewed after failure, retrying chain: songmid=$songMid")
    return resolveViaQualityChain(songMid, filenameMid, quality, getLocalizedString)
}

private suspend fun QQMusicSession.resolveViaQualityChain(
    songMid: String,
    filenameMid: String,
    quality: String,
    getLocalizedString: (Int) -> String
): QQMusicResolveOutcome {
    var sawVipRequired = false
    for (candidate in buildQQMusicQualityCandidates(quality, isLoggedIn)) {
        val outcome = tryResolveQQMusicUrl(
            songMid = songMid,
            filenameMid = filenameMid,
            quality = candidate,
            getLocalizedString = getLocalizedString
        )
        when (outcome) {
            is QQMusicResolveOutcome.Playable -> return outcome
            QQMusicResolveOutcome.RequiresVip -> sawVipRequired = true
            QQMusicResolveOutcome.Unavailable -> Unit
        }
    }
    return if (sawVipRequired) {
        NPLogger.w(TAG, "QQ Music track requires VIP: songmid=$songMid")
        QQMusicResolveOutcome.RequiresVip
    } else {
        QQMusicResolveOutcome.Unavailable
    }
}

private suspend fun QQMusicSession.tryResolveQQMusicUrl(
    songMid: String,
    filenameMid: String,
    quality: String,
    getLocalizedString: (Int) -> String
): QQMusicResolveOutcome = withContext(Dispatchers.IO) {
    val responseText = try {
        requestVkey(songMid = songMid, filenameMid = filenameMid, quality = quality)
    } catch (error: Exception) {
        NPLogger.e(TAG, "QQ Music vkey request failed: songmid=$songMid quality=$quality", error)
        return@withContext QQMusicResolveOutcome.Unavailable
    }

    val root = runCatching { JSONObject(responseText) }.getOrNull()
    if (root == null) {
        NPLogger.w(TAG, "QQ Music vkey response is not JSON: ${responseText.take(160)}")
        return@withContext QQMusicResolveOutcome.Unavailable
    }
    val data = root.optJSONObject("req_0")?.optJSONObject("data")
    val info = data?.optJSONArray("midurlinfo")?.optJSONObject(0)
    if (info == null) {
        NPLogger.w(TAG, "QQ Music vkey response has no midurlinfo: ${responseText.take(200)}")
        return@withContext QQMusicResolveOutcome.Unavailable
    }

    val purl = info.optString("purl").trim()
    val resultCode = info.optInt("result", QQMUSIC_RESULT_OK)
    if (purl.isBlank()) {
        NPLogger.w(
            TAG,
            "QQ Music vkey empty: songmid=$songMid quality=$quality result=$resultCode"
        )
        return@withContext if (resultCode == QQMUSIC_RESULT_VIP_REQUIRED) {
            QQMusicResolveOutcome.RequiresVip
        } else {
            QQMusicResolveOutcome.Unavailable
        }
    }

    val url = if (purl.startsWith("http://", true) || purl.startsWith("https://", true)) {
        purl
    } else {
        val sip = data.optJSONArray("sip")
        val prefix = if (sip != null && sip.length() > 0) sip.optString(0).orEmpty() else ""
        prefix + purl
    }
    if (url.isBlank()) return@withContext QQMusicResolveOutcome.Unavailable

    // 以服务端实际下发的档位为准：请求 F000 可能回落 M500（对齐酷狗「请求无损但回落 mp3」）
    val actualQuality = actualQualityFromPurl(purl) ?: quality
    if (actualQuality != quality) {
        NPLogger.d(
            TAG,
            "quality adjusted by server: requested=$quality actual=$actualQuality songmid=$songMid"
        )
    }

    val mimeType = qqMusicMimeType(actualQuality)
    val audioInfo = PlaybackAudioInfo(
        source = PlaybackAudioSource.QQ_MUSIC,
        qualityKey = actualQuality,
        qualityLabel = qqMusicQualityLabel(actualQuality, getLocalizedString),
        // 匿名态只有免费档可取；登录后开放全档位切换
        qualityOptions = if (isLoggedIn) {
            buildQQMusicQualityOptions(getLocalizedString)
        } else {
            emptyList()
        },
        codecLabel = deriveCodecLabel(mimeType),
        mimeType = mimeType
    )
    NPLogger.d(TAG, "resolved QQ Music url: songmid=$songMid quality=$actualQuality")
    // 不设 cacheKeyOverride：缓存 key 统一走 computeCacheKey（首个请求档位），
    // 避免写入用 actualQuality、查找用偏好档导致的 key 不对称。
    QQMusicResolveOutcome.Playable(
        SongUrlResult.Success(
            url = url,
            mimeType = mimeType,
            audioInfo = audioInfo
        )
    )
}

/**
 * 从 purl 文件名解析服务端实际下发的档位码。
 * purl 形如 `M500{media_mid}.mp3?...`；档位码取文件名前 4 位，
 * 且扩展名须与档位自洽（如 M500 → mp3），否则视为无法识别，由调用方回退请求档位。
 */
internal fun actualQualityFromPurl(purl: String): String? {
    val fileName = purl.substringBefore('?').substringAfterLast('/')
    val dot = fileName.lastIndexOf('.')
    if (dot < 5) return null
    val candidate = fileName.substring(0, 4).uppercase()
    val ext = fileName.substring(dot + 1).lowercase()
    val validShape = candidate[0] in 'A'..'Z' && candidate.substring(1).all { it in '0'..'9' }
    if (!validShape || ext != qqMusicFileExtension(candidate)) return null
    return candidate
}

/** 发起一次 `CgiGetVkey`，返回响应原文。 */
private suspend fun QQMusicSession.requestVkey(
    songMid: String,
    filenameMid: String,
    quality: String
): String {
    val ext = qqMusicFileExtension(quality)
    val cred = credential
    val loggedIn = isLoggedIn
    val param = JSONObject()
        .put("guid", guid)
        .put("songmid", JSONArray().put(songMid))
        .put("songtype", JSONArray().put(0))
        .put("uin", if (loggedIn) cred.musicid.toString() else "0")
        .put("loginflag", 1)
        .put("platform", QQMUSIC_PLATFORM)
        .put("filename", JSONArray().put("$quality$filenameMid$filenameMid.$ext"))

    val payload = JSONObject()
        .put(
            "req_0",
            JSONObject()
                .put("module", "vkey.GetVkeyServer")
                .put("method", "CgiGetVkey")
                .put("param", param)
        )
        .put(
            "comm",
            JSONObject()
                .put("uin", if (loggedIn) cred.musicid.toString() else "0")
                .put("format", "json")
                .put("ct", QQMUSIC_COMM_CT)
                .put("cv", 0)
        )

    val url = QQMUSIC_API_HOST.toHttpUrl().newBuilder()
        .addQueryParameter("format", "json")
        .addQueryParameter("data", payload.toString())
        .build()
    val requestBuilder = Request.Builder()
        .url(url)
        .header("User-Agent", QQMUSIC_USER_AGENT)
        .header("Referer", QQMUSIC_REFERER)
    if (loggedIn) {
        requestBuilder.header("Cookie", cred.toCookieHeader())
    }
    val request = requestBuilder.build()

    // 取址 QPS 闸门：降级链 / 外层重试 / 并发预取都会打到本接口
    vkeyRateLimiter.acquire()

    return client.newCall(request).awaitResponse { response ->
        if (!response.isSuccessful) {
            throw IOException("QQ Music vkey HTTP ${response.code}")
        }
        response.body.string()
    }
}

private const val TAG = "QQMusicPlayback"

/** 失败后重试续期的最短间隔：会话失效兑底，同时避免对不可播曲目反复续期。 */
private const val RETRY_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
