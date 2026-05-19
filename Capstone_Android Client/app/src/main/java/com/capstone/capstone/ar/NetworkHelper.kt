package com.capstone.capstone.ar

import android.util.Log
import com.capstone.capstone.network.RetrofitClient
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicBoolean

data class ArPoseSnapshot(val tx: Float, val ty: Float, val tz: Float, val qx: Float, val qy: Float, val qz: Float, val qw: Float) {
    fun toPose(): Pose = Pose(floatArrayOf(tx, ty, tz), floatArrayOf(qx, qy, qz, qw))
}

object NetworkHelper {

    private val localizeInFlight = AtomicBoolean(false)

    fun resetLocalizeState() {
        localizeInFlight.set(false)
    }

    suspend fun sendMappingImage(jpeg: ByteArray, uiUpdater: UiUpdater) {
        withContext(Dispatchers.IO) {
            try {
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "map_${System.currentTimeMillis()}.jpg",
                    jpeg.toRequestBody("image/jpeg".toMediaType())
                )

                val response = RetrofitClient.apiService.uploadMappingImage(part).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        uiUpdater.setStatus("Uploaded")
                    } else {
                        uiUpdater.setStatus("HTTP ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NET", "Mapping upload failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    uiUpdater.setStatus("Net ERR: ${e.message}")
                }
            }
        }
    }

    fun uploadLocalize(
        frame: Frame,
        jpeg: ByteArray,
        onPoseResponse: (JsonObject, ArPoseSnapshot) -> Unit,
        onFinished: (Boolean, String) -> Unit,
        uiUpdater: UiUpdater
    ) {
        if (!localizeInFlight.compareAndSet(false, true)) {
            val msg = "Localize pending"
            uiUpdater.setStatus(msg)
            onFinished(false, msg)
            return
        }

        try {
            val pose = frame.camera.pose
            val t = FloatArray(3).also { pose.getTranslation(it, 0) }
            val q = FloatArray(4).also { pose.getRotationQuaternion(it, 0) }

            val snapshot = ArPoseSnapshot(t[0], t[1], t[2], q[0], q[1], q[2], q[3])
            val metaBody = """
                {
                  "timestamp_ns": ${frame.timestamp},
                  "translation_m": [${t[0]}, ${t[1]}, ${t[2]}],
                  "rotation_qxyzw": [${q[0]}, ${q[1]}, ${q[2]}, ${q[3]}]
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())

            val imgPart = MultipartBody.Part.createFormData(
                "file",
                "frame_${System.currentTimeMillis()}.jpg",
                jpeg.toRequestBody("image/jpeg".toMediaType())
            )

            RetrofitClient.apiService.localize(imgPart, metaBody)
                .enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        localizeInFlight.set(false)
                        if (!response.isSuccessful) {
                            val msg = "HTTP ${response.code()}"
                            uiUpdater.setStatus(msg)
                            onFinished(false, msg)
                            return
                        }

                        val body = response.body()
                        if (body == null) {
                            val msg = "Empty body"
                            uiUpdater.setStatus(msg)
                            onFinished(false, msg)
                            return
                        }

                        Log.d("NET", "✅ Localize response: $body")

                        try {
                            onPoseResponse(body, snapshot)
                            uiUpdater.setStatus("Localized OK")
                            onFinished(true, "Localized OK")
                        } catch (e: Exception) {
                            Log.e("NET", "❌ handlePoseResponse() failed", e)
                            uiUpdater.setStatus("Parse ERR")
                            onFinished(false, "Parse ERR")
                        }
                    }

                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                        localizeInFlight.set(false)
                        val msg = "Net ERR: ${t.message ?: t.javaClass.simpleName}"
                        uiUpdater.setStatus(msg)
                        onFinished(false, msg)
                        Log.e("NET", "localize failed", t)
                    }
                })
        } catch (e: Exception) {
            localizeInFlight.set(false)
            val msg = "ERR: ${e.message}"
            uiUpdater.setStatus(msg)
            onFinished(false, msg)
            Log.e("NET", "uploadLocalize outer failed", e)
        }
    }

    suspend fun sendRebuildDone(uiUpdater: UiUpdater) {
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.finishRebuild("true").execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        uiUpdater.setStatus("Rebuild started")
                    } else {
                        uiUpdater.setStatus("HTTP ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NET", "Rebuild signal failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    uiUpdater.setStatus("Done ERR: ${e.message}")
                }
            }
        }
    }
}
