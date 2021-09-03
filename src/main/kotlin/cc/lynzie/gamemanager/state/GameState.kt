package cc.lynzie.gamemanager.state

import cc.lynzie.gamemanager.GameManager
import org.bukkit.event.Listener
import java.time.Duration
import java.time.Instant

abstract class GameState(gameManager: GameManager, val duration: Duration, val friendlyName: String) : Listener {
    var started = false
        private set
    var ended = false
        private set
    var frozen = false

    lateinit var startTime: Instant
        private set

    /**
     * Checks whether the [GameState] can end, ensuring
     * that the state is not [frozen], and that the time
     * between [startTime] and [Instant.now] either meets or
     * exceeds that of [duration].
     */
    fun isAbleToEnd(): Boolean {
        if (frozen) return false

        val timeElapsed = Duration.between(startTime, Instant.now())
        val remaining = timeElapsed.minus(duration)

        when {
            frozen -> return false
            remaining.isZero -> return true
            remaining.isNegative -> return true
            else -> return false
        }
    }

    abstract fun onStart()
    abstract fun onTick()
    abstract fun onEnd()
}