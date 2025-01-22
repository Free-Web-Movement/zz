package io.github.freewebmovement.zz.ui.content.peer

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.ui.common.PageType
import kotlinx.coroutines.launch


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun PeerMain(updatePage: ((value: PageType) -> Unit)) {
//    Column(
//        modifier = Modifier.padding(horizontal = 2.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        var scope = MainApplication.instance!!.coroutineScope;
//        var accounts: List<Account>
//        scope.launch {
//            accounts = MainApplication.instance!!.db.account().getAll()
//            if (accounts.isNotEmpty()) {
//                accounts.forEach {
//                    Text(it.address)
//                }
//            } else {
//                Text(
//                    "PeerMain"
//                )
//            }
//        }
//    }
}


@Preview
@Composable
private fun Preview() {
    PeerMain {

    }
}
