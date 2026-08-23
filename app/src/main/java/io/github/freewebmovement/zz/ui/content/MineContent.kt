package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.content.mine.AccountScreen
import io.github.freewebmovement.zz.ui.content.mine.WalletsScreen
import io.github.freewebmovement.zz.ui.content.mine.FwmcProfileScreen
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
        PageType.MineFwmcProfile -> FwmcProfileScreen()
        PageType.MineAccounts -> AccountScreen {
            updater(it, ContentType.Stacked)
        }
        PageType.MineWallet -> WalletsScreen()
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
