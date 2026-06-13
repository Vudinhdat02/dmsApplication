package com.example.dmsapplication.ui.historyView

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.repository.StatsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository(application)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _selectedDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()
    private val allStatsList = repository.getRecentStatsByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val filteredStatsList: StateFlow<List<DriverStats>> = combine(allStatsList, _selectedDate) { list, selectedDay ->
        val endOfDay = selectedDay + 86400000L - 1 // Cộng thêm 24h - 1ms để tính đến hết ngày
        list.filter { it.timestamp in selectedDay..endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Tính toán tổng số lần mất tập trung của ngày đó
    val dailyTotals: StateFlow<Pair<Int, Int>> = filteredStatsList.map { list ->
        val totalDrowsy = list.sumOf { it.drowsyCount }
        val totalHead = list.sumOf { it.headDistractedCount }
        Pair(totalDrowsy, totalHead)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOldCloudImages(userId)
            repository.refreshStatsFromCloud(userId)
        }
    }

    fun setSelectedDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

}