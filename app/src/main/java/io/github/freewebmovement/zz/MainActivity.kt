package io.github.freewebmovement.zz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.ui.BottomBar
import io.github.freewebmovement.zz.ui.IPListView
import io.github.freewebmovement.zz.ui.theme.ZzTheme
import io.github.freewebmovement.zz.ui.viewmodel.BottomBarModel


class MainActivity : ComponentActivity() {
	private lateinit var ipList: IPList
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ipList = MainApplication.instance!!.ipList
		setContent {
			ZzTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Column {
						MainFrame(
							ipList,
							modifier = Modifier.padding(innerPadding)
						).toString()
						val vm: BottomBarModel = viewModel()
						Spacer(modifier = Modifier.weight(1f))
						BottomBar(vm.current) {
							vm.current = it
						}
					}
				}
			}
		}
	}
}

@Composable
fun MainFrame(ipList: IPList, modifier: Modifier = Modifier) {
	val text: String = if (ipList.hasIPs()) {
		"You have ips"
	} else {
		"You have no ips"
	}
	Text(
		text = text,
		modifier = modifier
	)
	Row(
		Modifier.padding(top = 20.dp)
	) {
		IPListView(ipList.ipv6IPs)
		IPListView(ipList.ipv4IPs)
//		ipList.ipv6IPs.forEach { URLButton(it) }
//		ipList.ipv4IPs.forEach { URLButton(it) }
	}
}


