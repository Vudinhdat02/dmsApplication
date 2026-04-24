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

        val todayCalendar = Calendar.getInstance()

        for (stat in statsList) {
            totalDrowsyWeek += stat.drowsyCount
            totalHeadWeek += stat.headDistractedCount
            val currentErrors = stat.drowsyCount + stat.headDistractedCount

            // Gom nhóm theo ngày trong tuần cho biểu đồ
            val statCalendar = Calendar.getInstance()
            statCalendar.timeInMillis = stat.timestamp
            val dayOfWeek = statCalendar.get(Calendar.DAY_OF_WEEK)
            val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            weekCounts[index] += currentErrors.toFloat()

            if (statCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                statCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)) {
                todayErrors += currentErrors
            }
        }

        _weeklyData.value = weekCounts
        _totalDrowsy.value = totalDrowsyWeek
        _totalHead.value = totalHeadWeek

        val dailyScore = (100 - (todayErrors * 1)).coerceIn(0, 100)
        _safetyScore.value = dailyScore

        val totalErrorsWeek = totalDrowsyWeek + totalHeadWeek
        generateAiInsight(weekCounts, dailyScore, todayErrors, totalErrorsWeek)
    }

    private suspend fun generateAiInsight(weekCounts: FloatArray, dailyScore: Int, todayErrors: Int, totalErrorsWeek: Int) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(GroqApiService::class.java)

            val systemMessage = "Bạn là trợ lý AI phân tích an toàn lái xe. Trả lời cực kỳ ngắn gọn (dưới 40 từ), thân thiện và dùng tiếng Việt."
            val userPrompt = """
            Điểm lái xe HÔM NAY: $dailyScore/100 (Số lỗi hôm nay: $todayErrors).
            Tổng lỗi cả tuần: $totalErrorsWeek.
            Biểu đồ vi phạm (T2->CN): ${weekCounts.joinToString(", ")}.
            Hãy nhận xét thái độ lái xe hôm nay và đưa ra lời khuyên ngắn gọn.
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
                ?: "Hôm nay bạn lái xe ổn định, hãy tiếp tục phát huy!"

            _aiInsight.value = aiResult

        } catch (e: Exception) {
            android.util.Log.e("GroqDebug", "Lỗi Groq: ${e.message}")
            _aiInsight.value = "AI đang nghỉ ngơi một chút. Hãy luôn tập trung lái xe nhé!"
        }
    }
}