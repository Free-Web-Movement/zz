package io.github.freewebmovement.zz.ui.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.freewebmovement.zz.ui.common.ContentType
import io.github.freewebmovement.zz.ui.common.PageType
import io.github.freewebmovement.zz.ui.content.user.AccountScreen
import io.github.freewebmovement.zz.ui.content.user.WalletsScreen
import io.github.freewebmovement.zz.ui.content.user.FwmcProfileScreen
import io.github.freewebmovement.zz.ui.content.user.MineMain
import io.github.freewebmovement.zz.ui.content.user.WeightTableScreen
import io.github.freewebmovement.zz.ui.content.user.ConnectionsScreen
import io.github.freewebmovement.zz.ui.content.user.ProfileEditor
import io.github.freewebmovement.zz.ui.content.user.UserProfileEditor

@Composable
fun MinContent(
    page: PageType,
    updater: (page: PageType, value: ContentType) -> Unit,
    onOpenChat: (String, String) -> Unit = { _, _ -> },
) {
    when (page) {
        PageType.MineMain -> MineMain {
            updater(it, ContentType.Stacked)
        }
        PageType.MineProfile -> ProfileEditor {
            updater(it, ContentType.NonStacked)
        }
        PageType.MineUserEdit -> UserProfileEditor {
            updater(it, ContentType.NonStacked)
        }
        PageType.MineFwmcProfile -> FwmcProfileScreen()
        PageType.MineAccounts -> AccountScreen(
            updatePage = { updater(it, ContentType.Stacked) },
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineWallet -> WalletsScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineSettings -> io.github.freewebmovement.zz.ui.content.user.SettingsScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineServer -> ServerScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineStaticFile -> StaticFileScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineWeights -> WeightsScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineWeightTable -> WeightTableScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
        )
        PageType.MineConnections -> ConnectionsScreen(
            onBack = { updater(PageType.MineMain, ContentType.NonStacked) },
            onChat = onOpenChat,
        )
        else -> {
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MinContent(PageType.MineMain, { _, _ -> })
}

@Preview(locale = "en")
@Composable
private fun Preview_en() {
    MinContent(PageType.MineMain, { _, _ -> })
}
