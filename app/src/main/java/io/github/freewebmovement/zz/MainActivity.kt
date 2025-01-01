package io.github.freewebmovement.zz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.ui.BottomBar
import io.github.freewebmovement.zz.ui.theme.ZzTheme
import io.github.freewebmovement.zz.ui.viewmodel.BottomBarModel


class MainActivity : ComponentActivity() {
	private lateinit var ipList: IPList
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
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
	val text: String = if (ipList.hasPublicIP()) {
		"You have public ip"
	} else {
		"You have no public ip"
	}
	Text(
		text = text,
		modifier = modifier
	)
	Row(
		Modifier.padding(top = 20.dp)
	) {
		ipList.ipv6IPs.forEach { URLButton(it) }
		ipList.ipv4IPs.forEach { URLButton(it) }
		ipList.hasPublicIP()
	}

//	BottomBar(selected = 1)
}


@Composable
fun URLButton(url: String) {
	val context = LocalContext.current
	val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(url)) }
	Button(
		onClick = { context.startActivity(intent) }
	) {
		Text(text = url)
	}
}
