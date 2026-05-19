package com.capstone.capstone.ar

import androidx.lifecycle.LifecycleCoroutineScope
import com.capstone.capstone.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import android.os.Looper

/**
 * UIUpdater
 * 负责管理计时器、状态栏文字与姿态面板更新。
 * 所有 UI 操作都自动切换到主线程，避免线程冲突。
 */
class UiUpdater(
    private val binding: ActivityMainBinding,
    private val scope: LifecycleCoroutineScope
) {
    private var timerJob: Job? = null

    /** 启动计时器 */
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

    /** 停止计时器 */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        // 确保 UI 操作在主线程执行
        scope.launch(Dispatchers.Main) {
            binding.tvTimer.text = "00:00"
        }
    }

    /** 更新状态栏文字（线程安全） */
    fun setStatus(text: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            binding.chipStatus.text = text
        } else {
            scope.launch(Dispatchers.Main) {
                binding.chipStatus.text = text
            }
        }
    }

    /** 更新方向指示器（线程安全），不再显示调试坐标。 */
    fun updatePosePanel(pos: FloatArray, quat: FloatArray) {
        scope.launch(Dispatchers.Main) {
            binding.posePanel.poseOverlay.setQuaternion(quat[0], quat[1], quat[2], quat[3])
        }
    }
}

