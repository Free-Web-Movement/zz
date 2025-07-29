package rs.zz.coin

import io.github.freewebmovement.peer.interfaces.IAddress

class Address() : IAddress {
    external override fun create(): Long
    external override fun destroy(id: Long)
    external override fun prefix(ptr: Long): String

    companion object {
        init {
            System.loadLibrary("zz_rs") // Load your native library
        }
    }

    private var id : Long = 0

    init {
        id = create()
    }

    override fun prefix() : String {
        return prefix(id)
    }

    override fun destroy() {
        destroy(id)
    }
}