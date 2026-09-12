package moe.ouom.neriplayer.core.api.qqmusic

import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.SongItem

/** QQ音乐歌曲在 [SongItem.channelId] 中的标识。 */
const val QQMUSIC_CHANNEL_ID = "qqmusic"

/** 按专辑 mid 拼接封面地址（与现有 QQ音乐搜索链路的规则一致）。 */
internal fun buildQQMusicCoverUrl(albumMid: String?): String? {
    val mid = albumMid?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return "https://y.qq.com/music/photo_new/T002R800x800M000$mid.jpg"
}

/**
 * 构建 QQ音乐 [SongItem]。
 *
 * - [SongItem.album] = `"QQMusic|{songMid}"`，供 [PlayerManager.isQQMusicTrack] 识别；
 * - [SongItem.audioId] = songmid，取址与缓存的主键；
 * - [SongItem.subAudioId] = media_mid，取址 filename 优先使用它（缺失时回退 songmid）；
 * - [SongItem.channelId] = [QQMUSIC_CHANNEL_ID]。
 *
 * songmid 是字符串而 [SongItem.id] 是 Long，故 id 取 hashCode
 * （与酷狗概念版把 hash 映射到 id 的处理方式一致）。
 */
fun buildQQMusicSongItem(
    songMid: String,
    songName: String,
    singer: String,
    albumName: String? = null,
    durationMs: Long = 0L,
    coverUrl: String? = null,
    mediaMid: String? = null
): SongItem {
    return SongItem(
        id = songMid.hashCode().toLong(),
        name = songName,
        artist = singer,
        album = "${PlayerManager.QQMUSIC_SOURCE_TAG}|$songMid",
        albumId = 0L,
        durationMs = durationMs,
        coverUrl = coverUrl,
        channelId = QQMUSIC_CHANNEL_ID,
        audioId = songMid,
        subAudioId = mediaMid?.trim()?.takeIf { it.isNotBlank() }
    )
}
