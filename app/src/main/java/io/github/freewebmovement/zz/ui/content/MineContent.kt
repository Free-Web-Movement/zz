package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.content.mine.MineMain
import io.github.freewebmovement.zz.ui.content.mine.ProfileEditor

@Composable
fun MinContent(stackedUpdater: (value: ContentType) -> Unit) {
    var page by remember { mutableStateOf(PageType.MineMain) }

    when (page) {
        PageType.MineMain -> MineMain {
            page = it
            stackedUpdater(ContentType.Stacked)
        }

        PageType.MineProfile -> ProfileEditor {
            page = it
            stackedUpdater(ContentType.NonStacked)
        }

        PageType.MineServer -> MineMain {
            page = it
            stackedUpdater(ContentType.Stacked)
        }

        PageType.MineKey ->MineMain {
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