package io.github.freewebmovement.zz.ui.common

import androidx.compose.ui.graphics.Color

enum class TabType {
    Sessions,
    Contacts,
    Peers,
    Mine,
}


enum class ContentType {
    NonStacked,
    Stacked
}

enum class PageType(i: Int) {
    SessionMain(0x10),
    ContactMain(0x15),
    PeerMain(0x20),

    MineMain(0x40),
    MineProfile(0x41),
    MineUserEdit(0x42),
    MineAbout(0x46),
    MineFwmcProfile(0x47),
    MineWallet(0x48), MineAccounts(0x49), MineSettings(0x4A), MineServer(0x4B),
    MineStaticFile(0x4C), MineWeights(0x4D), MineWeightTable(0x4E),
    MineConnections(0x4F)
}

val rainbowColors: List<Color> = listOf(
    Color.Red,
    Color.Magenta,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color.Cyan
)
