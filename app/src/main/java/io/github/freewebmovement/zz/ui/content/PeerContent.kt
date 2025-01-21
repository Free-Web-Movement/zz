package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.content.peer.AddPeer
import io.github.freewebmovement.zz.ui.content.peer.PeerMain

@Composable
fun PeerContent(page: PageType, updater: (page: PageType, value: ContentType) -> Unit) {
    when (page) {
        PageType.PeerAdd -> AddPeer()
        else -> {
            PeerMain {
                updater(it, ContentType.Stacked)
            }
        }
    }
}