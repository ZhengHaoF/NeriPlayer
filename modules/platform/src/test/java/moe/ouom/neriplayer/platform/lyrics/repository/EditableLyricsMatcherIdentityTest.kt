package moe.ouom.neriplayer.platform.lyrics.repository

import kotlinx.coroutines.test.runTest
import moe.ouom.neriplayer.data.model.lyrics.kugou.KugouLyricsPayload
import moe.ouom.neriplayer.data.model.lyrics.kugou.KugouSongSearchResult
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricIdentity
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchConfidence
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchRequest
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchSource
import moe.ouom.neriplayer.data.model.music.SongDetails
import moe.ouom.neriplayer.data.model.music.SongSearchInfo
import moe.ouom.neriplayer.platform.search.api.NativeLyricSearchApi
import moe.ouom.neriplayer.platform.search.api.SearchApi
import moe.ouom.neriplayer.platform.youtube.api.client.YouTubeMusicClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class EditableLyricsMatcherIdentityTest {
    private val kugou = mock(KugouLyricsRepository::class.java)
    private val lrcLib = mock(LrcLibLyricsRepository::class.java)
    private val amll = mock(AmllLyricsRepository::class.java)
    private val youtube = mock(YouTubeMusicClient::class.java)

    private val blankKugouSong = KugouSongSearchResult(
        id = "", hash = "", title = "", artist = "", album = null, durationMs = 0L
    )

    /** What the matcher builds for `Kugou(audioHash = "STALE_HASH", songId = null)`. */
    private val staleIdentitySong = KugouSongSearchResult(
        id = "", hash = "STALE_HASH", title = "Signal", artist = "Artist",
        album = null, durationMs = 200_000L
    )

    private val kugouOnlyRequest = EditableLyricMatchRequest(
        keyword = "Signal Artist",
        trackName = "Signal",
        artistName = "Artist",
        durationMs = 200_000L,
        preferWordTimed = false,
        sources = setOf(EditableLyricMatchSource.KUGOU)
    )

    @Test
    fun `kugou hash skips the keyword search entirely`() = runTest {
        `when`(kugou.getBestLyricPayload(anyKugouSong())).thenReturn(
            KugouLyricsPayload("[00:01.00]Hello world", "[00:01.00]你好")
        )

        val matches = matcher().matchLyrics(
            kugouOnlyRequest.copy(
                identity = EditableLyricIdentity.Kugou(audioHash = "HASH123", songId = "9001")
            )
        )

        assertEquals(1, matches.size)
        assertTrue(matches.single().candidate.exactIdentity)
        assertEquals(EditableLyricMatchConfidence.HIGH, matches.single().confidence)
        verify(kugou).getBestLyricPayload(
            KugouSongSearchResult(
                id = "9001",
                hash = "HASH123",
                title = "Signal",
                artist = "Artist",
                album = null,
                durationMs = 200_000L
            )
        )
        verify(kugou, never()).searchSongs(anyString(), anyInt())
    }

    @Test
    fun `blank keyword still resolves when an identity is available`() = runTest {
        `when`(kugou.getBestLyricPayload(anyKugouSong())).thenReturn(
            KugouLyricsPayload("[00:01.00]Hello world")
        )

        val matches = matcher().matchLyrics(
            kugouOnlyRequest.copy(
                keyword = "",
                trackName = "",
                artistName = "",
                identity = EditableLyricIdentity.Kugou(audioHash = "HASH123", songId = null)
            )
        )

        assertEquals(1, matches.size)
        verify(kugou, never()).searchSongs(anyString(), anyInt())
    }

    @Test
    fun `unusable identity falls back to the keyword search`() = runTest {
        val searched = KugouSongSearchResult("k1", "hash", "Signal", "Artist", "Album", 200_000L)
        `when`(kugou.searchSongs(anyString(), anyInt())).thenReturn(listOf(searched))
        `when`(kugou.getBestLyricPayload(searched)).thenReturn(
            KugouLyricsPayload("[00:01.00]Hello world")
        )

        val matches = matcher().matchLyrics(
            kugouOnlyRequest.copy(
                identity = EditableLyricIdentity.Kugou(audioHash = "   ", songId = "9001")
            )
        )

        assertEquals(1, matches.size)
        verify(kugou, atLeastOnce()).searchSongs(anyString(), anyInt())
    }

    @Test
    fun `a failing identity lookup still reaches the keyword path`() = runTest {
        // runSourceSearch swallows exceptions into an empty list, so the identity lookup has to
        // catch on its own or the elvis fallback below it never runs.
        `when`(kugou.getBestLyricPayload(eqValue(staleIdentitySong)))
            .thenThrow(IllegalStateException("krc download rejected"))
        val searched = KugouSongSearchResult("k1", "hash", "Signal", "Artist", "Album", 200_000L)
        `when`(kugou.searchSongs(anyString(), anyInt())).thenReturn(listOf(searched))
        `when`(kugou.getBestLyricPayload(searched)).thenReturn(
            KugouLyricsPayload("[00:01.00]Hello world")
        )

        val matches = matcher().matchLyrics(
            kugouOnlyRequest.copy(
                identity = EditableLyricIdentity.Kugou(audioHash = "STALE_HASH", songId = null)
            )
        )

        assertEquals(1, matches.size)
        verify(kugou, atLeastOnce()).searchSongs(anyString(), anyInt())
    }

    @Test
    fun `identity outside the requested sources is ignored`() = runTest {
        val matches = matcher().matchLyrics(
            kugouOnlyRequest.copy(
                identity = EditableLyricIdentity.QqMusic(songMid = "001234")
            )
        )

        assertTrue(matches.isEmpty())
        verify(kugou, never()).getBestLyricPayload(anyKugouSong())
    }

    @Test
    fun `qq song mid uses the native detail call without searching`() = runTest {
        val qq = object : NativeLyricSearchApi {
            override suspend fun search(keyword: String, page: Int): List<SongSearchInfo> =
                throw AssertionError("QQ search is outside this test")

            override suspend fun getSongInfo(id: String): SongDetails =
                throw AssertionError("QQ generic details are outside this test")

            override suspend fun getNativeSongInfo(id: String): SongDetails =
                SongDetails(
                    id = id,
                    songName = "Signal",
                    singer = "Artist",
                    album = "Album",
                    coverUrl = null,
                    lyric = "[00:01.00]Hello world",
                    translatedLyric = "[00:01.00]你好"
                )
        }

        val matches = matcher(qq = qq).matchLyrics(
            kugouOnlyRequest.copy(
                sources = setOf(EditableLyricMatchSource.QQ_MUSIC),
                identity = EditableLyricIdentity.QqMusic(songMid = "001234")
            )
        )

        assertEquals(1, matches.size)
        assertEquals(EditableLyricMatchSource.QQ_MUSIC, matches.single().candidate.source)
        assertEquals("[00:01.00]你好", matches.single().candidate.translatedLyrics)
    }

    private fun matcher(
        cloud: SearchApi = unexpectedApi("cloud"),
        qq: SearchApi = unexpectedApi("QQ")
    ) = EditableLyricsMatcher(
        cloudMusicSearchApi = cloud,
        qqMusicSearchApi = qq,
        kugouLyricsClient = kugou,
        lrcLibClient = lrcLib,
        amllTtmlClient = amll,
        youtubeMusicClient = youtube
    )

    /** Mockito matchers return null, which Kotlin rejects for non-null parameters. */
    private fun anyKugouSong(): KugouSongSearchResult =
        any(KugouSongSearchResult::class.java) ?: blankKugouSong

    private fun <T : Any> eqValue(value: T): T {
        org.mockito.ArgumentMatchers.eq(value)
        return value
    }

    private fun unexpectedApi(name: String) = object : SearchApi {
        override suspend fun search(keyword: String, page: Int): List<SongSearchInfo> =
            throw AssertionError("$name search is outside this test")

        override suspend fun getSongInfo(id: String): SongDetails =
            throw AssertionError("$name details are outside this test")
    }
}
