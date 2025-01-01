package io.github.freewebmovement.zz.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun IPListView(ips: List<String>) {
	Column {
		ips.forEach { IPView(it) }
	}
}

@Composable
fun IPView(url: String) {
	val context = LocalContext.current
	val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(url)) }
	Button(
		onClick = { context.startActivity(intent) }
	) {
		Text(text = url)
	}
}