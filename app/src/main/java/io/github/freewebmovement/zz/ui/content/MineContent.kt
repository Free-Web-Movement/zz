package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.Pages
import io.github.freewebmovement.zz.ui.content.mine.MineMain
import io.github.freewebmovement.zz.ui.content.mine.ProfileEditor

@Composable
fun MinContent(stackedUpdater: (value: ContentType) -> Unit) {
    var page by remember { mutableStateOf(Pages.MineMain) }

    when (page) {
        Pages.MineMain -> MineMain {
            page = it
            stackedUpdater(ContentType.Stacked)
        }

        Pages.MineProfile -> ProfileEditor {
            page = it
            stackedUpdater(ContentType.NonStacked)
        }

        Pages.MineServer -> MineMain {
            page = it
            stackedUpdater(ContentType.Stacked)
        }

        Pages.MineKey ->MineMain {
            page = it
            stackedUpdater(ContentType.Stacked)
        }

        else -> {

        }
    }
}

@Preview
@Composable
private fun Preview() {
    MinContent {
    }
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    MinContent {
    }
}