package moe.ouom.neriplayer.core.api.qqmusic

import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 取址类接口的最小请求间隔闸门（约 3 QPS）。
 *
 * 串行化并发取址，并保证两次真实请求之间的间隔不低于 [minIntervalMs]，
 * 避免音质降级链、外层 Failure 重试与并发播放把 `CgiGetVkey` 打成限流。
 * 仅节流本进程内的取址调用，不改变服务端语义。
 */
internal class QQMusicVkeyRateLimiter(
    private val minIntervalMs: Long = DEFAULT_VKEY_MIN_INTERVAL_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val sleeper: suspend (Duration) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private var lastAcquireAtMs: Long? = null

    /**
     * 等待到可以发起下一次取址请求。
     * 在锁内等待，保证并发调用按到达顺序串行放行，而不是各自算完间隔后齐发。
     */
    suspend fun acquire(): Unit = mutex.withLock {
        val now = nowMs()
        val last = lastAcquireAtMs
        if (last != null) {
            val remainingMs = minIntervalMs - (now - last)
            if (remainingMs > 0L) {
                sleeper(remainingMs.milliseconds)
            }
        }
        lastAcquireAtMs = nowMs()
    }

    companion object {
        /** 对齐实施方案建议的 3 QPS；334ms 取整保证不超过 3 次/秒。 */
        const val DEFAULT_VKEY_MIN_INTERVAL_MS: Long = 334L
    }
}

/** 将 QPS 换算为最小间隔毫秒（供测试与文档对齐）。 */
internal fun minIntervalMsForQps(qps: Double): Long {
    require(qps > 0) { "qps must be positive" }
    return ceil(1000.0 / qps).toLong()
}
