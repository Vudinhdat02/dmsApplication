package com.example.dmsapplication.ui.MainView

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    // Trạng thái: Có đang buồn ngủ hay không?
    private val _isDrowsy = MutableStateFlow(false)
    val isDrowsy = _isDrowsy.asStateFlow()

    fun updateDrowsiness(isDrowsy: Boolean) {
        _isDrowsy.value = isDrowsy
    }
}