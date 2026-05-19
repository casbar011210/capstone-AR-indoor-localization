package com.capstone.capstone.ar

import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlinx.coroutines.*

class FrameUploader(
    private val arFrameProvider: () -> Frame?,
    private val onStatus: (String) -> Unit,
    private val sendMappingImage: suspend (ByteArray) -> Unit,
    private val uploadLocalize: (Frame, ByteArray) -> Unit,
    private val onCamPoseText: ((String) -> Unit)? = null
) {
    private var job: Job? = null

    fun start(
        modeMapping: Boolean,
        intervalMs: Long,
        jpegQuality: Int,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        stop()
        job = parentScope.launch(Dispatchers.Default) {
            while (isActive) {
                val frame = withContext(Dispatchers.Main) { arFrameProvider() }

                if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
                    withContext(Dispatchers.Main) { onStatus("Camera not ready") }
                    delay(300)
                    continue
                }

                withContext(Dispatchers.Main) { updatePoseText(frame) }

                val jpeg = ImageUtils.acquireJpegFromFrame(frame, jpegQuality)
                if (jpeg == null) {
                    withContext(Dispatchers.Main) { onStatus("No Image") }
                    delay(300)
                    continue
                }

                try {
                    if (modeMapping) {
                        sendMappingImage(jpeg)
                    } else {
                        withContext(Dispatchers.Main) { uploadLocalize(frame, jpeg) }
                    }
                } catch (e: Exception) {
                    Log.e("FrameUploader", "Upload failed", e)
                    withContext(Dispatchers.Main) {
                        onStatus("Upload ERR: ${e.message}")
                    }
                }

                delay(intervalMs)
            }
        }
    }

    fun captureLocalizeOnce(
        jpegQuality: Int,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        stop()
        job = parentScope.launch(Dispatchers.Default) {
            val frame = withContext(Dispatchers.Main) { arFrameProvider() }
            if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
                withContext(Dispatchers.Main) { onStatus("Camera not ready") }
                return@launch
            }

            withContext(Dispatchers.Main) { updatePoseText(frame) }

            val jpeg = ImageUtils.acquireJpegFromFrame(frame, jpegQuality)
            if (jpeg == null) {
                withContext(Dispatchers.Main) { onStatus("No Image") }
                return@launch
            }

            try {
                withContext(Dispatchers.Main) { uploadLocalize(frame, jpeg) }
            } catch (e: Exception) {
                Log.e("FrameUploader", "Single localize failed", e)
                withContext(Dispatchers.Main) {
                    onStatus("Upload ERR: ${e.message}")
                }
            }
        }
    }

    private fun updatePoseText(frame: Frame) {
        val pose = frame.camera.pose
        val t = FloatArray(3)
        pose.getTranslation(t, 0)
        val q = FloatArray(4)
        pose.getRotationQuaternion(q, 0)
        onCamPoseText?.invoke(
            "cam(pos)=[%.3f, %.3f, %.3f]\ncam(quat)=[%.3f, %.3f, %.3f, %.3f]".format(
                t[0], t[1], t[2], q[0], q[1], q[2], q[3]
            )
        )
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
