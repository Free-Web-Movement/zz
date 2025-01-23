package io.github.freewebmovement.zz.ui.content.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.peer.database.entity.AccountPeer
import kotlinx.coroutines.launch


@Composable
fun AccountListText() {
    val accountList = remember { mutableStateOf<List<AccountPeer>>(ArrayList()) }
    LaunchedEffect(Unit) {
        val scope = MainApplication.instance!!.coroutineScope
        scope.launch {
            accountList.value = MainApplication.getApp().db.account().getPeers()
        }
    }

    // If the list is empty, it means that our coroutine has not completed yet and we just want
    // to show our loading component and nothing else. So we return early.
    if (accountList.value.isEmpty()) {
        Text("No Peer Found!")
        return
    } else {
        accountList.value.forEach {
            Text(it.account.address)
            Text(it.account.intro)
            Text(it.account.nickname)
            Text(it.account.publicKey)
            it.peers.forEach { i ->
                Text(i.accessibilityVerificationCode)
                Text(i.getCode)
                Text(i.baseUrl)
                Text(i.ip)
                Text(i.ipType.toString())
                Text(i.port.toString())
            }
        }
    }
}


@Composable
fun PeerMain() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.padding(horizontal = 2.dp).fillMaxWidth().verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountListText()
    }
}


@Preview
@Composable
private fun Preview() {
    PeerMain()
}

//@SuppressLint("CoroutineCreationDuringComposition")
//@Preview
//@Composable
//private fun Preview1() {
//    var app = MainApplication.instance!!
//    var scope = app.coroutineScope
//    scope.launch {
//        app.db.account().add(Account("do"))
//    }
//
//    PeerMain {
//
//    }
//}
