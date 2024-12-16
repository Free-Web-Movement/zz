package io.github.freewebmovement.zz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.freewebmovement.zz.net.HttpServer
import io.github.freewebmovement.zz.net.ip
import io.github.freewebmovement.zz.ui.theme.ZzTheme


class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			ZzTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Greeting(
						modifier = Modifier.padding(innerPadding)
					).toString()
				}
			}
		}
		HttpServer.start()
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello Http Server!",
//        modifier = modifier
//    )
//    FlowRow(
//        Modifier.padding(top = 20.dp)
//    ) {
//        ip.v6s().forEach { URLButton(it) }
//        ip.v4s().forEach { URLButton(it) }
//    }

	FlowRow(Modifier.padding(top = 20.dp), horizontalArrangement = Alignment.CenterHorizontally) {
		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(Icons.Default.Add, contentDescription = null)
			Text(text = "Line 1")
		}
		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(Icons.Default.Build, contentDescription = null)
			Text(text = "line 2")
		}
		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(Icons.Default.Add, contentDescription = null)
			Text(text = "Line 1")
		}
		Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(Icons.Default.Build, contentDescription = null)
			Text(text = "line 2")
		}
	}
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


@Composable
fun BottomAppBarExample() {

//    Scaffold(
//        bottomBar = {
//            BottomAppBar(
//                actions = {
//                    IconButton(onClick = { /* do something */ }) {
//                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
//                    }
//                    IconButton(onClick = { /* do something */ }) {
//                        Icon(
//                            Icons.Filled.Edit,
//                            contentDescription = "Localized description",
//                        )
//                    }
//                    IconButton(onClick = { /* do something */ }) {
//                        Icon(
//                            Icons.Filled.Face,
//                            contentDescription = "Localized description",
//                        )
//                    }
//                    IconButton(onClick = { /* do something */ }) {
//                        Icon(
//                            Icons.Filled.Place,
//                            contentDescription = "Localized description",
//                        )
//                    }
//                },
//                ali
//            )
//        },
//    ) { innerPadding ->
//        Text(
//            modifier = Modifier.padding(innerPadding),
//            text = "Example of a scaffold with a bottom app bar."
//        )
//    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
	ZzTheme {
		Greeting()
	}
}