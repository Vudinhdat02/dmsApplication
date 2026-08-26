// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.historyView

import android.app.Application
import androidx.lifecycle.ViewModelProvider

class HistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(application) as T
    }
}