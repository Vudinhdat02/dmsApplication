package com.example.dmsapplication.ui.historyView

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.repository.StatsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository(application)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val statsList: StateFlow<List<DriverStats>> = repository
        .getRecentStatsByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncNow() {
        viewModelScope.launch {
            val unsynced = repository.getUnsynced()
            unsynced.forEach { repository.syncToCloud(it) }
        }
    }

    // Trong HistoryViewModel
    init {
        fetchHistoryFromCloud()
    }

    fun fetchHistoryFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Gọi repository để lấy dữ liệu từ Firebase/Server về
            // 2. Sau khi có danh sách từ Cloud, duyệt qua và Insert vào Room (StatsDao)
            repository.refreshStatsFromCloud(userId)
        }
    }
}