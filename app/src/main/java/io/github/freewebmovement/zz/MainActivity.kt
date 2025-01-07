package io.github.freewebmovement.zz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.ui.BottomBar
import io.github.freewebmovement.zz.ui.IPListView
import io.github.freewebmovement.zz.ui.common.TabType
import io.github.freewebmovement.zz.ui.theme.ZzTheme
import io.github.freewebmovement.zz.ui.MainActivity as UIMainActivity


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
						val context = LocalContext.current
						Button(
                            onClick = {
                                context.startActivity(Intent(context, UIMainActivity::class.java))
                            }
                        ) {
							Text(text="open")
						}
						Spacer(modifier = Modifier.weight(1f))
						var selectedTab by remember { mutableStateOf(TabType.Chats) }
						BottomBar(
							selectedTab = selectedTab,
							onClickTab = { selectedTab = it },
						)

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


