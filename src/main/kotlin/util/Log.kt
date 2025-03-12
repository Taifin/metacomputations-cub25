package util

import java.time.LocalDateTime

object Log {
    var enabled = false

    fun log(str: String) {
        if (enabled) System.err.println("[DEBUG ${LocalDateTime.now()}]: $str")
    }
}
