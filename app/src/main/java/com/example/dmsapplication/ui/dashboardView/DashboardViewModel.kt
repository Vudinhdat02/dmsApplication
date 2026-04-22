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

            // KIỂM TRA XEM CÓ PHẢI LÀ DỮ LIỆU CỦA HÔM NAY KHÔNG
            if (statCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                statCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)) {
                todayErrors += currentErrors
            }
        }

        _weeklyData.value = weekCounts
        _totalDrowsy.value = totalDrowsyWeek
        _totalHead.value = totalHeadWeek

        //Dựa trên hôm nay, trừ 1 điểm
        val dailyScore = (100 - (todayErrors * 1)).coerceIn(0, 100)
        _safetyScore.value = dailyScore

        // Gửi tổng lỗi tuần và lỗi hôm nay cho AI phân tích
        val totalErrorsWeek = totalDrowsyWeek + totalHeadWeek
        generateAiInsight(weekCounts, dailyScore, todayErrors, totalErrorsWeek)
    }

    private suspend fun generateAiInsight(weekCounts: FloatArray, dailyScore: Int, todayErrors: Int, totalErrorsWeek: Int) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = com.example.dmsapplication.BuildConfig.GEMINI_API_KEY,
                systemInstruction = content {
                    text("Bạn là trợ lý AI phân tích an toàn lái xe. Hãy nhìn vào điểm số hôm nay và xu hướng tuần để nhận xét. Trả lời cực kỳ ngắn gọn (dưới 40 từ), thân thiện và dùng tiếng Việt.")
                }
            )

            // Tách biệt rõ ràng Hôm Nay và Cả Tuần
            val prompt = """
                Điểm lái xe HÔM NAY: $dailyScore/100 (Số lỗi hôm nay: $todayErrors).
                Tổng lỗi cả tuần: $totalErrorsWeek.
                Biểu đồ vi phạm từ Thứ 2 -> Chủ nhật: ${weekCounts.joinToString(", ")}.
                Hãy nhận xét về thái độ lái xe hôm nay và đưa ra lời khuyên ngắn gọn.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val cleanText = response.text?.replace("*", "")?.trim() ?: "Hôm nay bạn lái xe rất tốt. Hãy tiếp tục giữ vững phong độ nhé!"
            _aiInsight.value = cleanText

        } catch (e: Exception) {
            android.util.Log.e("GeminiDebug", "Lỗi chi tiết từ Google: ", e)
            _aiInsight.value = "Chưa thể kết nối AI lúc này. Hãy chú ý giữ tỉnh táo khi lái xe!"
        }
    }
}