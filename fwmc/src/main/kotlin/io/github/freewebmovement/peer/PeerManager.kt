package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.interfaces.IApp
import io.github.freewebmovement.peer.system.Time
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.Timer
import java.util.TimerTask


const val PEER_LOOP_DELAY = 5000L

class PeerManager(var app: IApp) {
    private var timer = Timer()
    fun start() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                app.scope.launch {
                    val accounts = app.db.account().getPeers()
                    accounts.forEach { account ->
                        val peers = account.peers
                        peers.forEach { peer ->
                            val timeout = 2000
                            val address = InetAddress.getByName(peer.ip)!!
                            peer.accessible = address.isReachable(timeout)
//                            val time = address.
                            if (peer.accessible) {
                                peer.latestSeen = Time.now()
                            }
                            app.db.peer().update(peer)
                        }

                    }
                }
            }
        }, PEER_LOOP_DELAY)
    }

    fun stop() {
        timer.cancel()
    }
}