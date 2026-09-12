package moe.ouom.neriplayer.core.api.kugou

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.SongItem

/** 酷狗概念版搜索结果单曲（complexsearch 响应 `data.lists[]` 条目的结构化视图）。 */
data class KugouSong(
    val hash: String,
    val songName: String,
    val singer: String,
    val albumName: String?,
    val albumId: Long,
    val albumAudioId: Long,
    val coverUrl: String?,
    val durationMs: Long
)

/** 从 complexsearch 响应条目（酷狗概念版驼峰字段）解析为 [KugouSong]；缺 hash 视为无效返回 null。 */
internal fun parseKugouSearchItem(json: JsonObject): KugouSong? {
    val hash = json["FileHash"]?.jsonPrimitive?.contentOrNull?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val fileName = json["FileName"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val singer = json["SingerName"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val songName = json["OriSongName"]?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: fileName.substringAfter(" - ", fileName)
    val albumAudioId = json["MixSongID"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
    val albumId = json["AlbumID"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
    val durationSec = json["Duration"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
    val coverUrl = json["Image"]?.jsonPrimitive?.contentOrNull
        ?.replace("{size}", "320")
        ?.takeIf { it.startsWith("http", ignoreCase = true) }

    return KugouSong(
        hash = hash,
        songName = songName,
        singer = singer,
        albumName = json["AlbumName"]?.jsonPrimitive?.contentOrNull,
        albumId = albumId,
        albumAudioId = albumAudioId,
        coverUrl = coverUrl,
        durationMs = durationSec * 1_000L
    )
}

/**
 * 转成可播放的 [SongItem]（酷狗概念版源标记，与 B 站 `album` 前缀约定一致）：
 * - [SongItem.album] = `"Kugou|{hash}|{albumAudioId}"`，供 [PlayerManager.isKugouTrack] 识别；
 * - [SongItem.audioId] = 播放 hash；
 * - [SongItem.subAudioId] = album_audio_id；
 * - [SongItem.channelId] = "kugou"。
 */
fun KugouSong.toSongItem(): SongItem = buildKugouSongItem(
    hash = hash,
    songName = songName,
    singer = singer,
    albumName = albumName,
    albumId = albumId,
    albumAudioId = albumAudioId,
    coverUrl = coverUrl,
    durationMs = durationMs
)

/** 直接按部件构建酷狗概念版 [SongItem]（榜单/歌单/推荐等场景复用）。 */
fun buildKugouSongItem(
    hash: String,
    songName: String,
    singer: String,
    albumName: String?,
    albumId: Long,
    albumAudioId: Long,
    coverUrl: String?,
    durationMs: Long = 0L
): SongItem {
    val albumAudioIdPart = albumAudioId.takeIf { it > 0L }?.toString().orEmpty()
    return SongItem(
        id = albumAudioId.takeIf { it > 0L } ?: hash.hashCode().toLong(),
        name = songName,
        artist = singer,
        album = listOfNotNull(PlayerManager.KUGOU_SOURCE_TAG, hash, albumAudioIdPart)
            .joinToString(separator = "|"),
        albumId = albumId,
        durationMs = durationMs,
        coverUrl = coverUrl,
        channelId = KUGOU_CHANNEL_ID,
        audioId = hash,
        subAudioId = albumAudioIdPart.takeIf { it.isNotBlank() }
    )
}

/** 酷狗概念版歌曲在 [SongItem.channelId] 中的标识。 */
const val KUGOU_CHANNEL_ID = "kugou"