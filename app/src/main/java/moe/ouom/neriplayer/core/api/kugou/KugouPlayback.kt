package moe.ouom.neriplayer.core.api.kugou

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.player.model.PlaybackAudioInfo
import moe.ouom.neriplayer.core.player.model.PlaybackAudioSource
import moe.ouom.neriplayer.core.player.model.SongUrlResult
import moe.ouom.neriplayer.core.player.model.deriveCodecLabel
import moe.ouom.neriplayer.core.player.url.buildKugouQualityCandidates
import moe.ouom.neriplayer.core.player.url.buildKugouQualityOptions
import moe.ouom.neriplayer.core.player.url.qualityLabelForKugou
import moe.ouom.neriplayer.core.player.url.resolveKugouPlaybackQualityKey
import moe.ouom.neriplayer.data.model.SongItem

/**
 * 酷狗免费音质 key（v5/url 免费档）。
 */
const val KUGOU_FREE_QUALITY = "128"

/**
 * 酷狗高音质 key（登录后可尝试，失败自动降级到 [KUGOU_FREE_QUALITY]）。
 */
const val KUGOU_VIP_QUALITY = "320"

/**
 * 酷狗无损音质 key（需会员，失败自动逐档降级）。
 */
const val KUGOU_LOSSLESS_QUALITY = "flac"

/**
 * 酷狗 Hi-Res 音质 key（需会员，失败自动逐档降级）。
 */
const val KUGOU_HI_RES_QUALITY = "high"

/**
 * 酷狗蝰蛇母带音质 key（需会员，失败自动逐档降级）。
 */
const val KUGOU_VIPER_TAPE_QUALITY = "viper_tape"

/**
 * 解析酷狗云盘播放地址（`user.getCloudUrl` → query_musicclound_url）。
 * 响应结构与 v5/url 类似：flat 结构 `url` / `backupUrl` / `data.play_url` 均尝试。
 */
internal suspend fun KugouSession.resolveKugouCloudUrl(song: SongItem): SongUrlResult {
    val hash = song.audioId.orEmpty().trim()
    if (hash.isBlank()) {
        NPLogger.w("KugouPlayback", "kugou cloud song missing hash: song=${song.name}")
        return SongUrlResult.Failure
    }
    val albumAudioId = song.subAudioId?.toLongOrNull() ?: 0L
    val audioId = song.id
    val name = song.name
    ensureDeviceRegistered()

    val response = try {
        client.user.getCloudUrl(
            hash = hash,
            albumAudioId = albumAudioId,
            audioId = audioId,
            name = name
        )
    } catch (error: Exception) {
        NPLogger.e("KugouPlayback", "kugou getCloudUrl failed: hash=$hash", error)
        return SongUrlResult.Failure
    }
    val body = response.body
    val bizStatus = body["status"].asIntOrNull()
    if (bizStatus != 1) {
        NPLogger.w("KugouPlayback", "kugou cloud url rejected: hash=$hash body=${body.toString().take(160)}")
        return SongUrlResult.Failure
    }

    val url = body["url"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
        ?: body["backupUrl"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
        ?: body["data"]?.jsonObject?.get("play_url")?.jsonPrimitive?.content
        ?: body["data"]?.jsonObject?.get("url")?.jsonPrimitive?.content
    if (url.isNullOrBlank()) {
        NPLogger.w("KugouPlayback", "kugou cloud url empty: hash=$hash body=${body.toString().take(160)}")
        return SongUrlResult.Failure
    }

    val extName = body["extName"]?.jsonPrimitive?.content.orEmpty()
    val mimeType = kugouMimeTypeForExtension(extName)
    val audioInfo = PlaybackAudioInfo(
        source = PlaybackAudioSource.KUGOU,
        qualityKey = "cloud",
        qualityLabel = "云盘",
        bitrateKbps = null,
        codecLabel = deriveCodecLabel(mimeType),
        mimeType = mimeType
    )
    NPLogger.d("KugouPlayback", "resolved kugou cloud url: hash=$hash ext=$extName")
    return SongUrlResult.Success(
        url = url,
        mimeType = mimeType,
        expectedContentLength = null,
        audioInfo = audioInfo,
        cacheKeyOverride = "kugou-cloud-$hash"
    )
}

/**
 * 解析酷狗播放地址（v5/url）。
 *
 * 音质由 [quality] 决定，按档位从高到低依次尝试，首个可播档位即为结果。
 * 未登录时只尝试免费档，避免必然被拒的会员档请求。
 *
 * 响应为扁平结构：
 * - `status: 1` 可播 / `2` 不可播；
 * - `priv_status: 1` 可播（0 表示 VIP/付费，需登录或切源）；字段可能缺失，缺失时不作为否决依据；
 * - `url: [String]` / `backupUrl: [String]` 为顶层数组，取第一个即直链。
 */
internal suspend fun KugouSession.resolveKugouPlaybackUrl(
    song: SongItem,
    quality: String = KUGOU_VIP_QUALITY,
    getLocalizedString: (Int) -> String
): SongUrlResult {
    val hash = song.audioId.orEmpty().trim()
    if (hash.isBlank()) {
        NPLogger.w("KugouPlayback", "kugou song missing hash: song=${song.name}")
        return SongUrlResult.Failure
    }
    val albumAudioId = song.subAudioId?.toLongOrNull() ?: 0L
    val albumId = song.albumId

    ensureDeviceRegistered()

    for (candidate in buildKugouQualityCandidates(quality, isLoggedIn)) {
        val resolved = tryResolveSongUrl(
            hash = hash,
            albumAudioId = albumAudioId,
            albumId = albumId,
            quality = candidate,
            getLocalizedString = getLocalizedString
        )
        if (resolved != null) return resolved
    }
    return SongUrlResult.Failure
}

private suspend fun KugouSession.tryResolveSongUrl(
    hash: String,
    albumAudioId: Long,
    albumId: Long,
    quality: String,
    getLocalizedString: (Int) -> String
): SongUrlResult? {
    val response = try {
        client.song.getSongUrl(
            hash = hash,
            albumId = albumId,
            albumAudioId = albumAudioId,
            quality = quality,
        )
    } catch (error: Exception) {
        NPLogger.e("KugouPlayback", "kugou getSongUrl failed: hash=$hash quality=$quality", error)
        return null
    }

    val body = response.body
    val bizStatus = body["status"].asIntOrNull()
    // priv_status 并非每条响应都携带, 缺失时不能据此判定不可播
    val privStatus = body["priv_status"].asIntOrNull()
    if (bizStatus != 1 || (privStatus != null && privStatus != 1)) {
        NPLogger.w(
            "KugouPlayback",
            "kugou song not playable: hash=$hash quality=$quality status=$bizStatus priv=$privStatus " +
                "body=${body.toString().take(160)}"
        )
        return null
    }

    val url = body["url"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
        ?: body["backupUrl"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
    if (url.isNullOrBlank()) {
        NPLogger.w("KugouPlayback", "kugou song url empty: hash=$hash body=${body.toString().take(160)}")
        return null
    }

    val fileSize = body["fileSize"]?.jsonPrimitive?.content?.toLongOrNull()?.takeIf { it > 0L }
    val bitRate = body["bitRate"]?.jsonPrimitive?.content?.toIntOrNull()?.div(1_000)
        ?.takeIf { it > 0 }
    val extName = body["extName"]?.jsonPrimitive?.content.orEmpty()
    val mimeType = kugouMimeTypeForExtension(extName)
    val actualQuality = resolveKugouPlaybackQualityKey(
        extName = extName,
        bitrateKbps = bitRate,
        requestedQualityKey = quality
    )
    val audioInfo = PlaybackAudioInfo(
        source = PlaybackAudioSource.KUGOU,
        qualityKey = actualQuality,
        qualityLabel = qualityLabelForKugou(actualQuality, getLocalizedString),
        qualityOptions = buildKugouQualityOptions(getLocalizedString),
        bitrateKbps = bitRate,
        codecLabel = deriveCodecLabel(mimeType),
        mimeType = mimeType
    )

    NPLogger.d(
        "KugouPlayback",
        "resolved kugou url: hash=$hash requested=$quality actual=$actualQuality " +
            "bitrate=${bitRate}kbps ext=$extName"
    )
    return SongUrlResult.Success(
        url = url,
        mimeType = mimeType,
        expectedContentLength = fileSize,
        audioInfo = audioInfo,
        cacheKeyOverride = "kugou-$hash-$actualQuality"
    )
}

private fun kugouMimeTypeForExtension(extName: String?): String? = when (extName?.lowercase()) {
    "mp3" -> "audio/mpeg"
    "flac" -> "audio/flac"
    "m4a", "aac" -> "audio/aac"
    "ogg" -> "audio/ogg"
    "ape" -> "audio/ape"
    "wav" -> "audio/wav"
    else -> null
}

/**
 * 宽松取整：字段缺失或为 JSON null 时返回 null，不抛异常
 */
private fun kotlinx.serialization.json.JsonElement?.asIntOrNull(): Int? =
    runCatching { this?.jsonPrimitive?.int }.getOrNull()
