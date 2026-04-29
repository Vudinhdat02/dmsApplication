package com.example.dmsapplication.ui.dashboardView

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.repository.StatsRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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

    private val _aiInsight = MutableStateFlow("Đang phân tích dữ liệu lái xe của bạn...")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    init {
        loadAndAnalyzeData()
    }

    private fun loadAndAnalyzeData() {
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
        var totalPenalty = 0 // Tổng điểm trừ dựa trên GPS

        var morningErrors = 0   // 6h - 12h
        var afternoonErrors = 0 // 12h - 18h
        var eveningErrors = 0   // 18h - 24h
        var nightErrors = 0     // 0h - 6h
        var highSpeedErrors = 0 // > 60km/h

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
                statCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR) && currentErrors > 0) {

                todayErrors += currentErrors

                when (statCalendar.get(Calendar.HOUR_OF_DAY)) {
                    in 6..11 -> morningErrors += currentErrors
                    in 12..17 -> afternoonErrors += currentErrors
                    in 18..23 -> eveningErrors += currentErrors
                    else -> nightErrors += currentErrors
                }

                if (stat.speed >= 60f) {
                    highSpeedErrors += currentErrors
                    totalPenalty += (currentErrors * 5) // Chạy nhanh mà xao nhãng -> Trừ 5 điểm/lỗi
                } else if (stat.speed >= 30f) {
                    totalPenalty += (currentErrors * 3) // Tốc độ trung bình -> Trừ 3 điểm/lỗi
                } else {
                    totalPenalty += (currentErrors * 1) // Tốc độ thấp (đèn đỏ, tắc đường) -> Trừ 1 điểm/lỗi
                }
            }
        }

        _weeklyData.value = weekCounts
        _totalDrowsy.value = totalDrowsyWeek
        _totalHead.value = totalHeadWeek

        val dailyScore = (100 - totalPenalty).coerceIn(0, 100)
        _safetyScore.value = dailyScore

        generateAiInsight(dailyScore, todayErrors, morningErrors, afternoonErrors, eveningErrors, nightErrors, highSpeedErrors)
    }

    private suspend fun generateAiInsight(
        dailyScore: Int, todayErrors: Int,
        morning: Int, afternoon: Int, evening: Int, night: Int, highSpeed: Int
    ) {
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
        }
    }
}