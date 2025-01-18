package io.github.freewebmovement.zz.ui.common

import androidx.compose.ui.graphics.Color

enum class TabType {
    Sessions,
    Peers,
    Apps,
    Mine,
}


enum class ContentType {
    NonStacked,
    Stacked
}

enum class PageType(i: Int) {
    SessionMain(0x10),
    PeerMain(0x20),
    AppMain(0x30),
    MineMain(0x40),
    MineProfile(0x41),
    MineKey(0x42),
    MineServerIP(0x43),
    MineServerPort (0x44),
    MineServerShare (0x45),
    MineAbout (0x46)
}


enum class MinePages {
    Main,
    Profile,
    Server,
    Key
}

val rainbowColors: List<Color> = listOf(
    Color.Red,
    Color.Magenta,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color.Cyan
)
