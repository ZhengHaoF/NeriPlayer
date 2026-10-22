package moe.ouom.neriplayer.platform.lyrics.matching

import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.SongSourceTags
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricIdentity
import moe.ouom.neriplayer.data.model.music.MusicPlatform

/**
 * 从 [SongItem] 取出该平台原生曲目身份，供歌词匹配器跳过关键词搜索直接取词。
 *
 * 酷狗与 QQ 音乐的条目在建 `SongItem` 时就把原生 ID 放进了 [SongItem.audioId]
 * （酷狗是 KRC hash，QQ 是 songmid），所以 `album` 里那份 `Kugou|<hash>|<id>` 打包标记
 * 对取词链路是冗余的，这里不再解析它。
 *
 * 用户手工匹配过歌词之后，`matchedSongId` 可能指向另一首歌（匹配到了更合适的版本），
 * 此时它以 [SongItem.matchedSongId] 为准，与 fork 原有的取值顺序一致。
 */
fun lyricIdentityOf(song: SongItem): EditableLyricIdentity? = when {
    song.isKugouSourced() -> kugouIdentityOf(song)
    song.isQqMusicSourced() -> qqMusicIdentityOf(song)
    else -> null
}

private fun kugouIdentityOf(song: SongItem): EditableLyricIdentity? {
    val songId = if (song.matchedLyricSource == MusicPlatform.KUGOU) {
        song.matchedSongId
    } else {
        song.subAudioId
    }?.takeIf { it.isNotBlank() }
    return EditableLyricIdentity.Kugou(
        audioHash = song.audioId.orEmpty(),
        songId = songId
    ).takeIf { it.isUsable }
}

private fun qqMusicIdentityOf(song: SongItem): EditableLyricIdentity? {
    val songMid = if (song.matchedLyricSource == MusicPlatform.QQ_MUSIC) {
        song.matchedSongId
    } else {
        song.audioId
    }.orEmpty()
    return EditableLyricIdentity.QqMusic(songMid = songMid).takeIf { it.isUsable }
}

private fun SongItem.isKugouSourced(): Boolean =
    channelId.equals(KUGOU_CHANNEL_ID, ignoreCase = true) ||
        album.startsWith(SongSourceTags.KUGOU) ||
        matchedLyricSource == MusicPlatform.KUGOU

private fun SongItem.isQqMusicSourced(): Boolean =
    channelId.equals(QQ_MUSIC_CHANNEL_ID, ignoreCase = true) ||
        album.startsWith(SongSourceTags.QQ_MUSIC) ||
        matchedLyricSource == MusicPlatform.QQ_MUSIC

private const val KUGOU_CHANNEL_ID = "kugou"
private const val QQ_MUSIC_CHANNEL_ID = "qqmusic"
