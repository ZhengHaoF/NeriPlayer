package moe.ouom.neriplayer.core.player.url

import kotlinx.coroutines.runBlocking
import moe.ouom.neriplayer.core.player.model.SongUrlResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SongUrlUnplayableRetryTest {

    @Test
    fun `resolution does not retry unplayable vip failure`() = runBlocking {
        var attempts = 0

        val result = retrySongUrlResolution(
            delayBeforeRetry = { error("unplayable must not retry") }
        ) {
            attempts += 1
            SongUrlResult.Unplayable
        }

        assertEquals(1, attempts)
        assertSame(SongUrlResult.Unplayable, result)
    }

    @Test
    fun `unplayable after transient failures still stops immediately`() = runBlocking {
        var attempts = 0
        val delays = mutableListOf<Int>()

        val result = retrySongUrlResolution(
            delayBeforeRetry = { delays += it }
        ) { attempt ->
            attempts += 1
            if (attempt == 0) SongUrlResult.Failure else SongUrlResult.Unplayable
        }

        // attempt0 Failure → 允许一次 delay 重试；attempt1 Unplayable → 立即返回，不再 delay
        assertEquals(2, attempts)
        assertEquals(listOf(1), delays)
        assertSame(SongUrlResult.Unplayable, result)
    }
}
