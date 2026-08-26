package io.github.freewebmovement.peer.database.entity

import androidx.room.Embedded
import androidx.room.Relation


data class AccountPeer(
    @Embedded val account: Account,
    @Relation(
        parentColumn = "id",
        entityColumn = "account"
    )
    val peers: List<Peer>
)