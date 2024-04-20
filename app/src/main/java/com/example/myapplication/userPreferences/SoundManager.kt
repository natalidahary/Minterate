package com.example.myapplication.userPreferences

import android.content.Context
import android.media.MediaPlayer
import com.example.myapplication.R

class SoundManager(context: Context) {
    private var introClickMediaPlayer: MediaPlayer
    private var loginClickMediaPlayer: MediaPlayer
    private var clickMediaPlayer: MediaPlayer
    private var wrongClickMediaPlayer: MediaPlayer
    private var isSoundEnabled: Boolean = true

    init {
        // Initialize MediaPlayer instances
        introClickMediaPlayer = MediaPlayer.create(context, R.raw.start)
        loginClickMediaPlayer = MediaPlayer.create(context, R.raw.login)
        clickMediaPlayer = MediaPlayer.create(context, R.raw.click)
        wrongClickMediaPlayer = MediaPlayer.create(context, R.raw.clickwrong)
    }

    fun playClickSound() {
        if (isSoundEnabled) {
            clickMediaPlayer?.apply {
                if (!isPlaying) {
                    start()
                }
            }
        }
    }

    fun playWrongClickSound() {
        if (isSoundEnabled) {
            wrongClickMediaPlayer?.apply {
                if (!isPlaying) {
                    start()
                }
            }
        }
    }

    fun playIntro() {
        introClickMediaPlayer?.apply {
            if (!isPlaying) {
                start()
            }
        }
    }

    fun playLogin() {
        loginClickMediaPlayer?.apply {
            if (!isPlaying) {
                start()
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    fun release() {
        // Release MediaPlayer instances
        introClickMediaPlayer?.release()
        loginClickMediaPlayer?.release()
        clickMediaPlayer?.release()
        wrongClickMediaPlayer?.release()
    }
}