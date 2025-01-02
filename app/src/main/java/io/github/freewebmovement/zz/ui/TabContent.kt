package io.github.freewebmovement.zz.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer


/**
 * 当选中状态变化时，如何显示隐藏Tab，默认实现[DefaultTabDisplay]
 */
typealias TabDisplay = @Composable (content: @Composable () -> Unit, selected: Boolean) -> Unit

@Composable
internal fun DefaultTabDisplay(
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val scale = if (selected) 1f else 0f
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}


internal class TabContainerState {
    private var _selectedTab by mutableStateOf<Any?>(null)

    fun selectTab(tab: Any) {
        _selectedTab = tab
    }

    @Composable
    fun Tab(
        tab: Any,
        eager: Boolean,
        display: TabDisplay?,
        content: @Composable () -> Unit,
    ) {
        val selected by remember(tab) { derivedStateOf { tab == _selectedTab } }

        var load by remember(tab) { mutableStateOf(false) }
        if (eager || selected) {
            load = true
        }

        if (load) {
            if (display != null) {
                display(content, selected)
            } else {
                DefaultTabDisplay(
                    selected = selected,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun TabContainer(
    modifier: Modifier = Modifier,
    selectedTab: Any,
    content: @Composable TabContainerScope.() -> Unit,
) {
    val state = remember { TabContainerState() }

    LaunchedEffect(state, selectedTab) {
        state.selectTab(selectedTab)
    }

    Box(modifier = modifier) {
        remember(state) { TabContainerScope(state) }.content()
    }
}

class TabContainerScope internal constructor(
    private val state: TabContainerState,
) {
    @Composable
    fun Tab(
        tab: Any,
        eager: Boolean = false,
        display: TabDisplay? = null,
        content: @Composable () -> Unit,
    ) {
        state.Tab(
            tab = tab,
            eager = eager,
            display = display,
            content = content,
        )
    }
}