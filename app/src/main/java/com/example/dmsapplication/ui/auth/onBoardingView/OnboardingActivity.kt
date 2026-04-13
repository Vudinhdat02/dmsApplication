package com.example.dmsapplication.ui.OnboardingView

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.dmsapplication.R
import com.example.dmsapplication.ui.auth.loginView.LoginActivity
import com.example.dmsapplication.ui.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val images = listOf(R.drawable.pic_welcome, R.drawable.pic_welcome2, R.drawable.pic_welcome3)
    private val auth = FirebaseAuth.getInstance()

    private val sliderRunnable = object : Runnable {
        override fun run() {
            val nextItem = (viewPager.currentItem + 1) % images.size
            viewPager.setCurrentItem(nextItem, true)
            sliderHandler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        if (auth.currentUser != null) {
            // 1. Đã đăng nhập -> Vào thẳng Home
            navigateToHome()
            return
        } else if (!isFirstRun) {
            // 2. Đã xem Onboarding nhưng chưa đăng nhập -> Vào thẳng Login
            navigateToLogin()
            return
        }
        // 3. Nếu chưa xem Onboarding lần nào -> Tiếp tục chạy code bên dưới để hiện UI Onboarding

        enableEdgeToEdge()
        setContentView(R.layout.fragment_onboarding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBoarding)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val btnStart = findViewById<Button>(R.id.btnStart)
        viewPager = findViewById(R.id.viewPagerBackground)

        viewPager.adapter = ImageAdapter(images)
        sliderHandler.postDelayed(sliderRunnable, 3000)

        btnStart.setOnClickListener {
            // Đánh dấu đã xem Onboarding
            prefs.edit().putBoolean("is_first_run", false).apply()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}