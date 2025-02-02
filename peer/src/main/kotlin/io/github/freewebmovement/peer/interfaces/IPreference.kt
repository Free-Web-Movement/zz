package io.github.freewebmovement.peer.interfaces

interface IPreference {
    fun <T> save(key: String, value: T)
    fun <T> read(key: String, value: T) : T
}
