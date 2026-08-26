package rs.zz.coin

import io.github.freewebmovement.peer.interfaces.IAddress

class Address() : IAddress {
    external override fun create(): Long
    external override fun destroy(id: Long)
    external override fun prefix(ptr: Long): String
    external override fun toString(ptr: Long): String
    external override fun privateKey(ptr: Long): String
    override fun privateKey(): String {
        return privateKey(id)
    }

    external override fun publicKey(ptr: Long): String
    override fun publicKey(): String {
        return publicKey(id)
    }

    external override fun toJSON(ptr: Long): String;
    external override fun fromJSON(json: String): Long

    companion object {
        init {
            System.loadLibrary("zz_rs") // Load your native library
        }
    }

    private var id: Long = 0

    init {
        id = create()
    }

    override fun prefix(): String {
        return prefix(id)
    }

    override fun destroy() {
        destroy(id)
    }

    override fun toString(): String {
        return toString(id)
    }

    override fun to(): String {
        return toJSON(id)
    }

    override fun from(json: String) {
        if (id.toInt() != 0) {
            destroy(id)
        }
        id = fromJSON(json)
    }

}