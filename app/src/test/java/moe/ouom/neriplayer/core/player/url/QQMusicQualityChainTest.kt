package moe.ouom.neriplayer.core.player.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QQMusicQualityChainTest {

    @Test
    fun `anonymous chain only keeps free tiers`() {
        val chain = buildQQMusicQualityCandidates("F000", isLoggedIn = false)

        assertEquals(listOf("M500", "C400"), chain)
    }

    @Test
    fun `logged in preferred free tier keeps full lower chain`() {
        val chain = buildQQMusicQualityCandidates("M500", isLoggedIn = true)

        assertEquals(listOf("M500", "C400"), chain)
    }

    @Test
    fun `logged in preferred lossless keeps higher tiers first`() {
        val chain = buildQQMusicQualityCandidates("F000", isLoggedIn = true)

        assertEquals(listOf("F000", "O800", "M800", "C600", "M500", "C400"), chain)
    }

    @Test
    fun `unknown preferred falls back to free tiers when anonymous`() {
        val chain = buildQQMusicQualityCandidates("bogus", isLoggedIn = false)

        assertTrue(chain.all { it in setOf("M500", "C400") })
        assertTrue(chain.isNotEmpty())
    }
}
