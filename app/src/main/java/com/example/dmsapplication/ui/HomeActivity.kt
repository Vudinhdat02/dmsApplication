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
    private val homeFragment = HomeFragment()
    private val historyFragment = HistoryFragment()
    private val dashboardFragment = DashboardFragment()
    private val settingFragment = SettingFragment()
    private var activeFragment: Fragment = homeFragment

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
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, settingFragment, "settings").hide(settingFragment)
                add(R.id.fragment_container, dashboardFragment, "dashboard").hide(dashboardFragment)
                add(R.id.fragment_container, historyFragment, "history").hide(historyFragment)
                add(R.id.fragment_container, homeFragment, "home") // Fragment này sẽ hiển thị đầu tiên
            }.commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_history -> {
                    switchFragment(historyFragment)
                    true
                }
                R.id.nav_dashboard -> {
                    switchFragment(dashboardFragment)
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingFragment)
                    true
                }
                else -> false
            }
        }
        com.example.dmsapplication.worker.SyncWorker.schedulePeriodic(this)
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment) return
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(targetFragment)
            .commit()

        activeFragment = targetFragment
    }
}