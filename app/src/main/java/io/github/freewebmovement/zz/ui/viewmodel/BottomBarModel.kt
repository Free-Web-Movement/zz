package io.github.freewebmovement.zz.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class BottomBarModel: ViewModel() {
	var current by mutableIntStateOf(0)
}