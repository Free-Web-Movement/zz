package io.github.freewebmovement.zz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    FlowRow(
        Modifier.padding(top = 20.dp)
    ) {
        ip.v6s().forEach { URLButton(it) }
        ip.v4s().forEach { URLButton(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZzTheme {
    }
}