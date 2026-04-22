package com.example.dmsapplication.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.dmsapplication.R
import com.example.dmsapplication.ui.dashboardView.DashboardFragment
import com.example.dmsapplication.ui.historyView.HistoryFragment
import com.example.dmsapplication.ui.homeView.HomeFragment
import com.example.dmsapplication.ui.settingView.SettingFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val rootView = findViewById<ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { replaceFragment(HomeFragment()); true }
                R.id.nav_history -> { replaceFragment(HistoryFragment()); true }
                R.id.nav_dashboard -> { replaceFragment(DashboardFragment()); true }
                R.id.nav_settings -> { replaceFragment(SettingFragment()); true }
                else -> true
            }
        }

        // Khởi động WorkManager định kỳ để dọn ảnh cũ
        com.example.dmsapplication.worker.SyncWorker.schedulePeriodic(this)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}