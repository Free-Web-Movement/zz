package io.github.freewebmovement.zz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.ui.theme.ZzTheme


class MainActivity : ComponentActivity() {
	private lateinit var ipList: IPList
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		ipList = MainApplication.instance!!.ipList
		setContent {
			ZzTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Greeting(
						ipList,
						modifier = Modifier.padding(innerPadding)
					).toString()
				}
			}
		}
	}
}

@Composable
fun Greeting(ipList: IPList, modifier: Modifier = Modifier) {
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
//
//	FlowRow(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
//		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
//			Icon(Icons.Default.Add, contentDescription = null)
//			Text(text = "Line 1")
//		}
//		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
//			Icon(Icons.Default.Build, contentDescription = null)
//			Text(text = "line 2")
//		}
//		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
//			Icon(Icons.Default.Add, contentDescription = null)
//			Text(text = "Line 1")
//		}
//		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
//			Icon(Icons.Default.Build, contentDescription = null)
//			Text(text = "line 2")
//		}
//	}
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
