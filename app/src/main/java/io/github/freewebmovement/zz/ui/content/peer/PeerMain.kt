package io.github.freewebmovement.zz.ui.content.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import io.github.freewebmovement.zz.system.database.entity.Account
import kotlinx.coroutines.launch


@Composable
fun AccountListText() {
    val accountList = remember { mutableStateOf<List<Account>>(ArrayList()) }
    LaunchedEffect(Unit) {
        val scope = MainApplication.instance!!.coroutineScope
        scope.launch {
            accountList.value = MainApplication.instance!!.db.account().getAll()
        }
    }

    // If the list is empty, it means that our coroutine has not completed yet and we just want
    // to show our loading component and nothing else. So we return early.
    if (accountList.value.isEmpty()) {
        Text("No Peer Found!")
        return
    } else {
        accountList.value.forEach {
            Text(it.address)
        }
    }
}


@Composable
fun PeerMain() {
    Column(
        modifier = Modifier.padding(horizontal = 2.dp),
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
