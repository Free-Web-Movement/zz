package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.content.mine.LocalServerIPInfo
import io.github.freewebmovement.zz.ui.content.mine.LocalServerPort
import io.github.freewebmovement.zz.ui.content.mine.MineMain
import io.github.freewebmovement.zz.ui.content.mine.ProfileEditor

@Composable
fun MinContent(page: PageType, updater: (page: PageType, value: ContentType) -> Unit) {
    when (page) {
        PageType.MineMain -> MineMain {
            updater(it, ContentType.Stacked)
        }

        PageType.MineProfile -> ProfileEditor {
            updater(it, ContentType.NonStacked)
        }
        PageType.MineServerPort -> LocalServerPort {
            updater(it, ContentType.NonStacked)
        }
        PageType.MineServerIP -> LocalServerIPInfo {
            updater(it, ContentType.NonStacked)
        }

        PageType.MineKey ->MineMain {
            updater(it, ContentType.Stacked)
        }

        else -> {

        }
    }
}

@Preview
@Composable
private fun Preview() {
    MinContent(PageType.MineMain) {_,_ ->
    }
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    MinContent(PageType.MineMain) {_,_ ->
    }
}