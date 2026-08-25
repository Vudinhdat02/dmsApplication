package com.example.dmsapplication.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.dmsapplication.R
import com.example.dmsapplication.ui.dashboardView.DashboardFragment
import com.example.dmsapplication.ui.historyView.HistoryFragment
import com.example.dmsapplication.ui.homeView.HomeFragment
import com.example.dmsapplication.ui.settingView.SettingFragment
import com.example.dmsapplication.worker.SyncWorker
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var homeFragment: HomeFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var dashboardFragment: DashboardFragment
    private lateinit var settingFragment: SettingFragment
    private lateinit var activeFragment: Fragment

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_HISTORY = "history"
        private const val TAG_DASHBOARD = "dashboard"
        private const val TAG_SETTINGS = "settings"
        private const val STATE_ACTIVE_TAG = "active_fragment_tag"
    }

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

        // FragmentManager restores tagged fragments automatically after a configuration
        // change, including switching the system between light mode and dark mode.
        homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
            ?: HomeFragment()
        historyFragment = supportFragmentManager.findFragmentByTag(TAG_HISTORY) as? HistoryFragment
            ?: HistoryFragment()
        dashboardFragment = supportFragmentManager.findFragmentByTag(TAG_DASHBOARD) as? DashboardFragment
            ?: DashboardFragment()
        settingFragment = supportFragmentManager.findFragmentByTag(TAG_SETTINGS) as? SettingFragment
            ?: SettingFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, settingFragment, TAG_SETTINGS).hide(settingFragment)
                add(R.id.fragment_container, dashboardFragment, TAG_DASHBOARD).hide(dashboardFragment)
                add(R.id.fragment_container, historyFragment, TAG_HISTORY).hide(historyFragment)
                add(R.id.fragment_container, homeFragment, TAG_HOME)
            }.commitNow()
        }

        activeFragment = fragmentForTag(savedInstanceState?.getString(STATE_ACTIVE_TAG))
            ?: listOf(homeFragment, historyFragment, dashboardFragment, settingFragment)
                .firstOrNull { it.isAdded && !it.isHidden }
            ?: homeFragment
        bottomNav.selectedItemId = menuItemFor(activeFragment)

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

        SyncWorker.scheduleImmediate(this)
        SyncWorker.schedulePeriodic(this)
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment && targetFragment.isAdded && !targetFragment.isHidden) return

        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction().apply {
            if (activeFragment.isAdded) hide(activeFragment)
            if (targetFragment.isAdded) {
                show(targetFragment)
            } else {
                add(R.id.fragment_container, targetFragment, tagFor(targetFragment))
            }
        }.commitNow()
        activeFragment = targetFragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::activeFragment.isInitialized) {
            outState.putString(STATE_ACTIVE_TAG, tagFor(activeFragment))
        }
        super.onSaveInstanceState(outState)
    }

    private fun fragmentForTag(tag: String?): Fragment? = when (tag) {
        TAG_HOME -> homeFragment
        TAG_HISTORY -> historyFragment
        TAG_DASHBOARD -> dashboardFragment
        TAG_SETTINGS -> settingFragment
        else -> null
    }

    private fun tagFor(fragment: Fragment): String = when (fragment) {
        historyFragment -> TAG_HISTORY
        dashboardFragment -> TAG_DASHBOARD
        settingFragment -> TAG_SETTINGS
        else -> TAG_HOME
    }

    private fun menuItemFor(fragment: Fragment): Int = when (fragment) {
        historyFragment -> R.id.nav_history
        dashboardFragment -> R.id.nav_dashboard
        settingFragment -> R.id.nav_settings
        else -> R.id.nav_home
    }
}
