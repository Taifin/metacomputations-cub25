package util

import java.time.LocalDateTime

object Log {
    fun log(str: String) {
         System.err.println("[DEBUG ${LocalDateTime.now()}]: $str")
    }
}
