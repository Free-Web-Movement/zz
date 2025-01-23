package io.github.freewebmovement.peer.system

class Time {
    companion object {
        fun now(isMilliSecond: Boolean = false): Long {
            return if (isMilliSecond) System.currentTimeMillis() else System.currentTimeMillis() / 1000
        }
    }
}