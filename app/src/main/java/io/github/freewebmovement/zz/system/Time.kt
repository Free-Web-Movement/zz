package io.github.freewebmovement.zz.system

class Time {
    companion object {
        fun now() : Long {
            return System.currentTimeMillis() / 1000
        }
    }

}