package com.capstone.capstone.ar

import androidx.lifecycle.LifecycleCoroutineScope
import com.capstone.capstone.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import android.os.Looper

/**
 * UIUpdater
 * Responsible for managing the timer, status bar text and posture panel updates.
 * All UI operations automatically switch to the main thread to avoid thread conflicts. */
class UiUpdater(
    private val binding: ActivityMainBinding,
    private val scope: LifecycleCoroutineScope
) {
    private var timerJob: Job? = null

    /** Start the timer */
    fun startTimer() {
        stopTimer()
        val start = System.currentTimeMillis()
        timerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - start) / 1000
                val mm = (elapsed / 60).toInt()
                val ss = (elapsed % 60).toInt()
                binding.tvTimer.text = String.format("%02d:%02d", mm, ss)
                delay(1000)
            }
        }
    }

    /** Stop the timer */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        // 确保 UI 操作在主线程执行
        scope.launch(Dispatchers.Main) {
            binding.tvTimer.text = "00:00"
        }
    }

    /** Update the status bar text (thread-safe) */
    fun setStatus(text: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            binding.chipStatus.text = text
        } else {
            scope.launch(Dispatchers.Main) {
                binding.chipStatus.text = text
            }
        }
    }

    /** Update the direction indicator (thread-safe), and no longer display debugging coordinates. */
    fun updatePosePanel(pos: FloatArray, quat: FloatArray) {
        scope.launch(Dispatchers.Main) {
            binding.posePanel.poseOverlay.setQuaternion(quat[0], quat[1], quat[2], quat[3])
        }
    }
}

