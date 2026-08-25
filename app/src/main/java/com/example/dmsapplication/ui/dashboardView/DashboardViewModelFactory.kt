package com.example.dmsapplication.ui.dashboardView

import android.app.Application
import androidx.lifecycle.ViewModelProvider
class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(application) as T
    }
}