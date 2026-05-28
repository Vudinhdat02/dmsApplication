package com.example.dmsapplication.ui.dashboardView

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.repository.StatsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository(application)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _weeklyData = MutableStateFlow(FloatArray(7))
    val weeklyData: StateFlow<FloatArray> = _weeklyData.asStateFlow()

    private val _safetyScore = MutableStateFlow(100)
    val safetyScore: StateFlow<Int> = _safetyScore.asStateFlow()

    private val _totalDrowsy = MutableStateFlow(0)
    val totalDrowsy: StateFlow<Int> = _totalDrowsy.asStateFlow()

    private val _totalHead = MutableStateFlow(0)
    val totalHead: StateFlow<Int> = _totalHead.asStateFlow()

    // Giữ nguyên nội dung AI cũ, không tự xóa khi load lại data
    private val _aiInsight = MutableStateFlow("Nhấn nút bên dưới để nhận phân tích từ AI.")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    // Trạng thái loading của nút AI
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Lưu snapshot số liệu mới nhất để dùng khi người dùng bấm nút
    private var latestDailyScore = 100
    private var latestTodayErrors = 0
    private var latestMorningErrors = 0
    private var latestAfternoonErrors = 0
    private var latestEveningErrors = 0
    private var latestNightErrors = 0
    private var latestHighSpeedErrors = 0

    init {
        loadData()
    }

    // Chỉ load dữ liệu thống kê, KHÔNG gọi AI
    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getRecentStatsByUser(userId).collect { statsList ->
                processStats(statsList)
            }
        }
    }

    private suspend fun processStats(statsList: List<DriverStats>) {
        val weekCounts = FloatArray(7)
        var totalDrowsyWeek = 0
        var totalHeadWeek = 0

        var todayErrors = 0
        var totalPenalty = 0

        var morningErrors = 0
        var afternoonErrors = 0
        var eveningErrors = 0
        var nightErrors = 0
        var highSpeedErrors = 0

        val todayCalendar = Calendar.getInstance()

        for (stat in statsList) {
            totalDrowsyWeek += stat.drowsyCount
            totalHeadWeek += stat.headDistractedCount
            val currentErrors = stat.drowsyCount + stat.headDistractedCount

            val statCalendar = Calendar.getInstance()
            statCalendar.timeInMillis = stat.timestamp
            val dayOfWeek = statCalendar.get(Calendar.DAY_OF_WEEK)
            val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            weekCounts[index] += currentErrors.toFloat()

            if (statCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                statCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR) &&
                currentErrors > 0
            ) {
                todayErrors += currentErrors

                when (statCalendar.get(Calendar.HOUR_OF_DAY)) {
                    in 6..11  -> morningErrors   += currentErrors
                    in 12..17 -> afternoonErrors += currentErrors
                    in 18..23 -> eveningErrors   += currentErrors
                    else      -> nightErrors     += currentErrors
                }

                if (stat.speed >= 60f) {
                    highSpeedErrors += currentErrors
                    totalPenalty += (currentErrors * 5)
                } else if (stat.speed >= 30f) {
                    totalPenalty += (currentErrors * 3)
                } else {
                    totalPenalty += (currentErrors * 1)
                }
            }
        }

        _weeklyData.value = weekCounts
        _totalDrowsy.value = totalDrowsyWeek
        _totalHead.value = totalHeadWeek

        val dailyScore = (100 - totalPenalty).coerceIn(0, 100)
        _safetyScore.value = dailyScore

        // Lưu lại snapshot số liệu mới nhất, chờ người dùng bấm nút
        latestDailyScore     = dailyScore
        latestTodayErrors    = todayErrors
        latestMorningErrors  = morningErrors
        latestAfternoonErrors = afternoonErrors
        latestEveningErrors  = eveningErrors
        latestNightErrors    = nightErrors
        latestHighSpeedErrors = highSpeedErrors
    }

    // Hàm này được gọi từ Fragment khi người dùng BẤM NÚT
    fun requestAiAnalysis() {
        if (_isAiLoading.value) return // Tránh bấm nhiều lần liên tiếp
        viewModelScope.launch(Dispatchers.IO) {
            generateAiInsight(
                latestDailyScore,
                latestTodayErrors,
                latestMorningErrors,
                latestAfternoonErrors,
                latestEveningErrors,
                latestNightErrors,
                latestHighSpeedErrors
            )
        }
    }

    private suspend fun generateAiInsight(
        dailyScore: Int, todayErrors: Int,
        morning: Int, afternoon: Int, evening: Int, night: Int, highSpeed: Int
    ) {
        _isAiLoading.value = true
        _aiInsight.value = "Đang phân tích dữ liệu lái xe của bạn..."

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(GroqApiService::class.java)

            val systemMessage = "Bạn là trợ lý AI phân tích an toàn lái xe. Trả lời cực kỳ ngắn gọn (khoảng 40 từ), thân thiện, dùng tiếng Việt. KHÔNG chào hỏi, đi thẳng vào nhận xét."

            val userPrompt = """
            Điểm an toàn hôm nay: $dailyScore/100.
            Tổng số lần mất tập trung (ngáp/quay đầu): $todayErrors.
            Mất tập trung ở tốc độ cao (trên 60km/h): $highSpeed lần.
            Phân bố lỗi theo khung giờ: Sáng($morning), Chiều($afternoon), Tối($evening), Đêm($night).
            Hãy chỉ ra khung giờ tài xế hay mệt mỏi nhất, cảnh báo về tốc độ (nếu có) và đưa ra 1 lời khuyên thực tế.
            """.trimIndent()

            val request = GroqRequest(
                model = "llama-3.1-8b-instant",
                messages = listOf(
                    Message("system", systemMessage),
                    Message("user", userPrompt)
                )
            )

            val authHeader = "Bearer ${com.example.dmsapplication.BuildConfig.GROQ_API_KEY}"
            val response = service.getChatCompletion(authHeader, request)
            val aiResult = response.choices.firstOrNull()?.message?.content?.replace("*", "")?.trim()
                ?: "Hôm nay bạn lái xe khá ổn định. Hãy tiếp tục duy trì sự tập trung nhé!"

            _aiInsight.value = aiResult

        } catch (e: Exception) {
            android.util.Log.e("GroqDebug", "Lỗi Groq: ${e.message}")
            _aiInsight.value = "Hệ thống AI đang nghỉ ngơi một chút. Hãy luôn tập trung lái xe nhé!"
        } finally {
            _isAiLoading.value = false
        }
    }
}