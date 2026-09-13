package moe.ouom.neriplayer.core.api.qqmusic

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QQMusicVkeyRateLimiterTest {

    @Test
    fun `first acquire does not wait`() = runBlocking {
        var now = 0L
        var slept = Duration.ZERO
        val limiter = QQMusicVkeyRateLimiter(
            minIntervalMs = 334L,
            nowMs = { now },
            sleeper = { slept += it },
        )

        limiter.acquire()

        assertEquals(Duration.ZERO, slept)
    }

    @Test
    fun `second acquire waits remaining interval`() = runBlocking {
        var now = 0L
        val sleeps = mutableListOf<Duration>()
        val limiter = QQMusicVkeyRateLimiter(
            minIntervalMs = 334L,
            nowMs = { now },
            sleeper = { delay ->
                sleeps += delay
                now += delay.inWholeMilliseconds
            },
        )

        limiter.acquire()
        limiter.acquire()

        assertEquals(listOf(334.milliseconds), sleeps)
    }

    @Test
    fun `acquire after interval elapsed does not wait`() = runBlocking {
        var now = 0L
        var slept = Duration.ZERO
        val limiter = QQMusicVkeyRateLimiter(
            minIntervalMs = 334L,
            nowMs = { now },
            sleeper = { slept += it },
        )

        limiter.acquire()
        now += 400L
        limiter.acquire()

        assertEquals(Duration.ZERO, slept)
    }

    @Test
    fun `concurrent acquires are serialized with min interval`() = runBlocking {
        var now = 0L
        val sleepLog = mutableListOf<Duration>()
        val limiter = QQMusicVkeyRateLimiter(
            minIntervalMs = 334L,
            nowMs = { now },
            sleeper = { delay ->
                sleepLog += delay
                now += delay.inWholeMilliseconds
            },
        )

        val jobs = (1..4).map { async { limiter.acquire() } }
        jobs.awaitAll()

        // 首请求不等待；后续 3 次各等满间隔
        assertEquals(3, sleepLog.size)
        assertTrue(sleepLog.all { it == 334.milliseconds })
    }

    @Test
    fun `minIntervalMsForQps rounds up to not exceed requested qps`() {
        assertEquals(334L, minIntervalMsForQps(3.0))
        assertEquals(1000L, minIntervalMsForQps(1.0))
        assertEquals(500L, minIntervalMsForQps(2.0))
    }
}
