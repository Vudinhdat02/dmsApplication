// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.homeView.helper

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.example.dmsapplication.R
import java.util.LinkedList

class AlarmHelper(private val context: Context) {
    private val toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
    private var warningPlayer: MediaPlayer? = null
    private var yawnPlayer: MediaPlayer? = null
    private enum class AlertType { WARNING, REST }
    private val alertQueue = LinkedList<AlertType>()
    private var isPlaying = false
    init {
        prepareMediaPlayers()
    }
    private fun prepareMediaPlayers() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            warningPlayer = MediaPlayer.create(context, R.raw.warning).apply {
                setAudioAttributes(audioAttributes)
                isLooping = false
                setOnCompletionListener { onPlaybackComplete() }
            }
            yawnPlayer = MediaPlayer.create(context, R.raw.ngap).apply {
                setAudioAttributes(audioAttributes)
                isLooping = false
                setOnCompletionListener { onPlaybackComplete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun onPlaybackComplete() {
        isPlaying = false
        if (alertQueue.isNotEmpty()) {
            playNext()
        }
    }
    private fun playNext() {
        val next = alertQueue.poll() ?: return
        isPlaying = true
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        val player = when (next) {
            AlertType.WARNING -> warningPlayer
            AlertType.REST    -> yawnPlayer
        }
        player?.let { mp ->
            mp.seekTo(0)
            mp.start()
        }
    }
    fun playAlert() {
        try {
            if (alertQueue.lastOrNull() == AlertType.WARNING) return
            alertQueue.add(AlertType.WARNING)
            if (!isPlaying) {
                playNext()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun playRestAlert() {
        try {
            if (alertQueue.lastOrNull() == AlertType.REST) return
            alertQueue.add(AlertType.REST)
            if (!isPlaying) {
                playNext()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun stopAlert() {
        try {
            alertQueue.clear()
            toneGenerator.stopTone()
            listOfNotNull(warningPlayer, yawnPlayer)
                .filter { it.isPlaying }
                .forEach { player ->
                    player.pause()
                    player.seekTo(0)
                }
            isPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun release() {
        try {
            alertQueue.clear()
            isPlaying = false
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