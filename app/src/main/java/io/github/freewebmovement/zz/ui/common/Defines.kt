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
    MineAbout(0x46),
    MineFwmcProfile(0x47),
    MineWallet(0x48), MineAccounts(0x49)
}

val rainbowColors: List<Color> = listOf(
    Color.Red,
    Color.Magenta,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color.Cyan
)
