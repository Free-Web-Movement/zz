package io.github.freewebmovement.zz.system.database.entity

import androidx.room.Embedded
import androidx.room.Relation


data class PeerMessages (
    @Embedded val peer: Peer,
    @Relation(
        parentColumn = "id",
        entityColumn = "peer"
    )
    val messages: List<Message>
)