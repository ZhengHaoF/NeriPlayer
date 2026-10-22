package moe.ouom.neriplayer.data.model.lyrics.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditableLyricIdentityTest {

    @Test
    fun `kugou identity is usable only with a non blank hash`() {
        assertTrue(EditableLyricIdentity.Kugou("AB12", "99").isUsable)
        assertFalse(EditableLyricIdentity.Kugou("", "99").isUsable)
        assertFalse(EditableLyricIdentity.Kugou("   ", "99").isUsable)
    }

    @Test
    fun `kugou identity stays usable when only the hash is known`() {
        assertTrue(EditableLyricIdentity.Kugou("AB12", null).isUsable)
    }

    @Test
    fun `qq identity is usable only with a non blank song mid`() {
        assertTrue(EditableLyricIdentity.QqMusic("001234").isUsable)
        assertFalse(EditableLyricIdentity.QqMusic("").isUsable)
    }

    @Test
    fun `cloud music identity requires a positive song id`() {
        assertTrue(EditableLyricIdentity.CloudMusic(1L).isUsable)
        assertFalse(EditableLyricIdentity.CloudMusic(0L).isUsable)
        assertFalse(EditableLyricIdentity.CloudMusic(-3L).isUsable)
    }

    @Test
    fun `each identity maps to its own match source`() {
        assertEquals(
            EditableLyricMatchSource.KUGOU,
            EditableLyricIdentity.Kugou("AB12", null).source
        )
        assertEquals(
            EditableLyricMatchSource.QQ_MUSIC,
            EditableLyricIdentity.QqMusic("001234").source
        )
        assertEquals(
            EditableLyricMatchSource.CLOUD_MUSIC,
            EditableLyricIdentity.CloudMusic(7L).source
        )
    }

    @Test
    fun `match request without identity still defaults to the automatic source set`() {
        val request = EditableLyricMatchRequest(
            keyword = "晴天 周杰伦",
            trackName = "晴天",
            artistName = "周杰伦"
        )

        assertEquals(null, request.identity)
        assertEquals(DEFAULT_EDITABLE_LYRIC_MATCH_SOURCES, request.sources)
    }

    @Test
    fun `identity defaults are not exact matches`() {
        val candidate = EditableLyricMatchCandidate(
            id = "1",
            source = EditableLyricMatchSource.CLOUD_MUSIC,
            title = "晴天",
            artist = "周杰伦",
            lyrics = "[00:00.00] 重映旧时光"
        )

        assertFalse(candidate.exactIdentity)
    }
}
