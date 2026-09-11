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
import moe.ouom.neriplayer.data.model.SongItem

/**
 * 酷狗免费音质 key（v5/url 免费档）。
 */
const val KUGOU_FREE_QUALITY = "128"

/**
 * 酷狗 VIP 高音质 key（登录后可尝试，失败自动降级到 [KUGOU_FREE_QUALITY]）。
 */
const val KUGOU_VIP_QUALITY = "320"

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
    val bizStatus = body["status"]?.jsonPrimitive?.int
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
    val mimeType = when (extName.lowercase()) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "m4a", "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        else -> null
    }
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
 * 解析酷狗播放地址（免费音质 v5/url）。
 *
 * 响应为扁平结构：
 * - `status: 1` 可播 / `2` 不可播；
 * - `priv_status: 1` 可播（0 表示 VIP/付费，需登录或切源）；
 * - `url: [String]` / `backupUrl: [String]` 为顶层数组，取第一个即直链。
 */
internal suspend fun KugouSession.resolveKugouPlaybackUrl(
    song: SongItem,
    quality: String = if (isLoggedIn) KUGOU_VIP_QUALITY else KUGOU_FREE_QUALITY
): SongUrlResult {
    val hash = song.audioId.orEmpty().trim()
    if (hash.isBlank()) {
        NPLogger.w("KugouPlayback", "kugou song missing hash: song=${song.name}")
        return SongUrlResult.Failure
    }
    val albumAudioId = song.subAudioId?.toLongOrNull() ?: 0L
    val albumId = song.albumId

    ensureDeviceRegistered()

    // 登录后按 高音质→128 降级尝试；未登录仅 128。
    val qualityCandidates = buildList {
        add(quality)
        if (isLoggedIn && quality != KUGOU_FREE_QUALITY) add(KUGOU_FREE_QUALITY)
    }.distinct()

    for (candidate in qualityCandidates) {
        val resolved = tryResolveSongUrl(hash, albumAudioId, albumId, candidate)
        if (resolved != null) return resolved
    }
    return SongUrlResult.Failure
}

private suspend fun KugouSession.tryResolveSongUrl(
    hash: String,
    albumAudioId: Long,
    albumId: Long,
    quality: String
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
    val bizStatus = body["status"]?.jsonPrimitive?.int
    val privStatus = body["priv_status"]?.jsonPrimitive?.int
    if (bizStatus != 1 || privStatus != 1) {
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
    val mimeType = when (extName.lowercase()) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "m4a", "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        else -> null
    }
    val audioInfo = PlaybackAudioInfo(
        source = PlaybackAudioSource.KUGOU,
        qualityKey = quality,
        qualityLabel = quality,
        bitrateKbps = bitRate,
        codecLabel = deriveCodecLabel(mimeType),
        mimeType = mimeType
    )

    NPLogger.d(
        "KugouPlayback",
        "resolved kugou url: hash=$hash quality=$quality bitrate=${bitRate}kbps ext=$extName"
    )
    return SongUrlResult.Success(
        url = url,
        mimeType = mimeType,
        expectedContentLength = fileSize,
        audioInfo = audioInfo,
        cacheKeyOverride = "kugou-$hash-$quality"
    )
}

/**
 * 供 UI 展示所需的酷狗音质选项（阶段 3 扩展高音质前仅免费档）。
 * 保持与 [PlayerUrlResolver] 内其它平台一致的辅助函数形态，便于后续扩展。
 */
@Suppress("unused")
internal fun buildKugouQualityLabel(qualityKey: String): String = qualityKey