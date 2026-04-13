package com.example.dmsapplication.ui.homeView.helper

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.example.dmsapplication.R

class AlarmHelper(private val context: Context) {
    private val toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
    private var warningPlayer: MediaPlayer? = null
    private var yawnPlayer: MediaPlayer? = null

    init {
        // Khởi tạo MediaPlayer DUY NHẤT một lần khi App bắt đầu
        prepareMediaPlayers()
    }

    private fun prepareMediaPlayers() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Chuẩn bị file cảnh báo nguy hiểm (Nhắm mắt, quay đầu)
            warningPlayer = MediaPlayer.create(context, R.raw.warning).apply {
                setAudioAttributes(audioAttributes)
                isLooping = false // Cực kỳ quan trọng: Phát hết 1 lần rồi tự dừng
            }

            // 2. Chuẩn bị file cảnh báo nghỉ ngơi (Ngáp ngủ)
            yawnPlayer = MediaPlayer.create(context, R.raw.ngap).apply {
                setAudioAttributes(audioAttributes)
                isLooping = false
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Phát âm thanh cảnh báo nguy hiểm khẩn cấp
    fun playAlert() {
        try {
            // Bíp một cái ngắn (Dùng ToneGenerator rất nhẹ)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)

            warningPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    // Nếu đang không phát thì tua về đầu và phát
                    mp.seekTo(0)
                    mp.start()
                }
                // (Nếu mp đang phát rồi thì bỏ qua, cứ để nó phát tiếp cho hết câu)
            } ?: run {
                // Nếu rủi ro bị crash driver, khởi tạo lại âm thanh
                prepareMediaPlayers()
                warningPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Phát âm thanh khuyên nghỉ ngơi khi ngáp 3 lần
    fun playRestAlert() {
        try {
            yawnPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    mp.seekTo(0)
                    mp.start()
                }
            } ?: run {
                prepareMediaPlayers()
                yawnPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dừng âm thanh
    fun stopAlert() {
    }

    // Giải phóng bộ nhớ khi tắt Fragment (Vẫn giữ nguyên để tránh rò rỉ bộ nhớ)
    fun release() {
        try {
            toneGenerator.release()

            warningPlayer?.stop()
            warningPlayer?.release()
            warningPlayer = null

            yawnPlayer?.stop()
            yawnPlayer?.release()
            yawnPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}