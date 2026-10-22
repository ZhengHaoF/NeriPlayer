package moe.ouom.neriplayer.platform.lyrics.matching

import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricFormat
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchCandidate
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchConfidence
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchRequest
import moe.ouom.neriplayer.data.model.lyrics.matching.EditableLyricMatchSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins what an identity-sourced candidate is trusted to skip, and what it is not.
 *
 * A platform ID means the recording is known, so title/artist/duration similarity is
 * skipped on purpose. Content sanity is still enforced: a stale hash that resolves to a
 * lyric overflowing the track's duration is downgraded instead of winning outright.
 */
class EditableLyricIdentityRankingTest {

    private val request = EditableLyricMatchRequest(
        keyword = "Signal Artist",
        trackName = "Signal",
        artistName = "Artist",
        durationMs = 200_000L,
        preferWordTimed = false,
        sources = setOf(EditableLyricMatchSource.KUGOU)
    )

    @Test
    fun `identity candidate skips similarity but is ranked first`() {
        val keywordCandidate = candidate("kw", lyrics = "[00:01.00]Hello world")
        val identityCandidate = candidate(
            "id",
            title = "Completely Different Song",
            artist = "Someone Else",
            exactIdentity = true
        )

        val ranked = rankEditableLyricMatches(request, listOf(keywordCandidate, identityCandidate))

        assertEquals(2, ranked.size)
        assertEquals("id", ranked.first().candidate.id)
        assertEquals(EditableLyricMatchConfidence.HIGH, ranked.first().confidence)
        assertEquals(1000, ranked.first().score)
    }

    @Test
    fun `identity candidate whose lyric overflows the track duration is downgraded`() {
        val overflowing = candidate(
            "stale",
            lyrics = "[00:01.00]Hello\n[120:00.00]world",
            exactIdentity = true
        )

        val ranked = rankEditableLyricMatches(request, listOf(overflowing))

        assertEquals(1, ranked.size)
        assertEquals(EditableLyricMatchConfidence.LOW, ranked.single().confidence)
    }

    @Test
    fun `identity candidate still needs non blank lyrics`() {
        val ranked = rankEditableLyricMatches(
            request,
            listOf(candidate("empty", lyrics = "   ", exactIdentity = true))
        )

        assertNull(ranked.firstOrNull())
    }

    @Test
    fun `non identity candidate with an unrelated title is still rejected`() {
        val ranked = rankEditableLyricMatches(
            request,
            listOf(candidate("kw", title = "Unrelated", artist = "Nobody Here"))
        )

        assertNull(ranked.firstOrNull())
    }

    private fun candidate(
        id: String,
        title: String = "Signal",
        artist: String = "Artist",
        lyrics: String = "[00:01.00]Hello world",
        exactIdentity: Boolean = false
    ) = EditableLyricMatchCandidate(
        id = id,
        source = EditableLyricMatchSource.KUGOU,
        title = title,
        artist = artist,
        album = null,
        durationMs = request.durationMs,
        lyrics = lyrics,
        format = EditableLyricFormat.LRC,
        exactIdentity = exactIdentity
    )
}
