package io.github.freewebmovement.zz.ui.common
enum class TabType {
    Chats,
    Contacts,
    Apps,
    Mine,
}


enum class ContentType {
    NonStacked,
    Stacked
}

enum class PageType(i: Int) {
    ChatMain(0x10),
    ContactMain(0x20),
    AppMain(0x30),
    MineMain(0x40),
    MineProfile(0x41),
    MineKey(0x42),
    MineServer(0x43)
}


enum class MinePages {
    Main,
    Profile,
    Server,
    Key
}
