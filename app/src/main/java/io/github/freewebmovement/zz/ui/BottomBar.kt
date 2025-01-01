package io.github.freewebmovement.zz.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.ui.theme.black


@Composable
fun TabItem(@DrawableRes iconId: Int, title: String, color: Color, modifier: Modifier) {
	Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
		Icon(
			painterResource(iconId), contentDescription = title, modifier.size(24.dp),
			tint = color
		)
		Text(text = title, fontSize = 12.sp, color = color)
	}
}

@Composable
fun BottomBar(selected: Int, onSelected: (Int) -> Unit) {
	Row(
		modifier = Modifier
			.height(64.dp)
			.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.Bottom
	) {
		TabItem(
			if (selected == 0) R.drawable.ic_chat_bubble_selected else R.drawable.ic_chat_bubble,
			"聊天",
			if (selected == 0) MaterialTheme.colorScheme.primary else
				black,
			Modifier
				.weight(1f)
				.clickable {
					onSelected(0)
				}
		)
		TabItem(
			if (selected == 1) R.drawable.ic_server_selected else R.drawable.ic_server,
			"通讯录",
			if (selected == 1) MaterialTheme.colorScheme.primary else
				black,
			Modifier
				.weight(1f)
				.clickable {
					onSelected(1)
				}
		)
		TabItem(
			if (selected == 2) R.drawable.ic_app_selected else R.drawable.ic_app,
			"应用",
			if (selected == 2) MaterialTheme.colorScheme.primary else
				black,
			Modifier
				.weight(1f)
				.clickable {
					onSelected(2)
				}
		)
		TabItem(
			if (selected == 3) R.drawable.ic_account_selected else R.drawable.ic_account,
			"我的",
			if (selected == 3) MaterialTheme.colorScheme.primary else
				black,
			Modifier
				.weight(1f)
				.clickable {
					onSelected(3)
				}
		)
	}
}


@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
	var current by remember { mutableIntStateOf(0) }
//	val vm: BottomBarModel = viewModel()
	BottomBar(selected = 1) {
		current = it
	}
}

@Preview(showBackground = true)
@Composable
fun PreviewMessage1() {
	var current by remember { mutableIntStateOf(0) }
//	val vm: BottomBarModel = viewModel()
	BottomBar(selected = 2) {
		current = it
	}
}
