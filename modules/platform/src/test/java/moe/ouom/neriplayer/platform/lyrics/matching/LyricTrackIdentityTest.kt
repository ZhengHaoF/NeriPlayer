package moe.ouom.neriplayer.platform.lyrics.matching

import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricIdentity
import moe.ouom.neriplayer.data.model.music.MusicPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricTrackIdentityTest {

    @Test
    fun `kugou channel song carries its hash and sub audio id`() {
        val song = song(
            channelId = "kugou",
            album = "Kugou|HASH123|9001",
            audioId = "HASH123",
            subAudioId = "9001"
        )

        assertEquals(
            EditableLyricIdentity.Kugou(audioHash = "HASH123", songId = "9001"),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `a manual lyric match to another version wins over the original song id`() {
        val song = song(
            channelId = "kugou",
            album = "Kugou|HASH123|9001",
            audioId = "HASH123",
            subAudioId = "9001",
            matchedLyricSource = MusicPlatform.KUGOU,
            matchedSongId = "777"
        )

        assertEquals(
            EditableLyricIdentity.Kugou(audioHash = "HASH123", songId = "777"),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `cloud drive songs are recognised by their packed album tag alone`() {
        val song = song(
            channelId = null,
            album = "KugouCloud|HASH999|",
            audioId = "HASH999",
            subAudioId = null
        )

        assertEquals(
            EditableLyricIdentity.Kugou(audioHash = "HASH999", songId = null),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `qq music channel song uses its song mid`() {
        val song = song(
            channelId = "qqmusic",
            album = "QQMusic|00MID",
            audioId = "00MID",
            subAudioId = "00MEDIA"
        )

        assertEquals(
            EditableLyricIdentity.QqMusic(songMid = "00MID"),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `a manual qq match replaces the song mid`() {
        val song = song(
            channelId = "qqmusic",
            album = "QQMusic|00MID",
            audioId = "00MID",
            matchedLyricSource = MusicPlatform.QQ_MUSIC,
            matchedSongId = "99MID"
        )

        assertEquals(
            EditableLyricIdentity.QqMusic(songMid = "99MID"),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `channel id casing does not matter`() {
        val song = song(channelId = "Kugou", album = "Kugou|HASH123|", audioId = "HASH123")

        assertEquals(
            EditableLyricIdentity.Kugou(audioHash = "HASH123", songId = null),
            lyricIdentityOf(song)
        )
    }

    @Test
    fun `netease and local songs have no platform lyric identity`() {
        assertNull(
            lyricIdentityOf(
                song(channelId = "netease", album = "Netease|123", audioId = "123")
            )
        )
        assertNull(lyricIdentityOf(song(channelId = null, album = "", audioId = null)))
    }

    @Test
    fun `a blank audio id is not treated as a usable identity`() {
        val song = song(channelId = "kugou", album = "Kugou||", audioId = "")

        assertNull(lyricIdentityOf(song))
    }

    @Test
    fun `a qq song whose mid vanished after matching is dropped`() {
        val song = song(
            channelId = "qqmusic",
            album = "QQMusic|",
            audioId = "",
            matchedLyricSource = MusicPlatform.QQ_MUSIC,
            matchedSongId = "   "
        )

        assertNull(lyricIdentityOf(song))
    }

    private fun song(
        channelId: String?,
        album: String,
        audioId: String?,
        subAudioId: String? = null,
        matchedLyricSource: MusicPlatform? = null,
        matchedSongId: String? = null
    ) = SongItem(
        id = 1L,
        name = "Signal",
        artist = "Artist",
        album = album,
        albumId = 2L,
        durationMs = 200_000L,
        coverUrl = null,
        channelId = channelId,
        audioId = audioId,
        subAudioId = subAudioId,
        matchedLyricSource = matchedLyricSource,
        matchedSongId = matchedSongId
    )
}
