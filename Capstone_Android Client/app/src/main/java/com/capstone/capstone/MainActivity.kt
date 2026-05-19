package com.capstone.capstone

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.capstone.capstone.ar.ArPoseSnapshot
import com.capstone.capstone.ar.FrameUploader
import com.capstone.capstone.ar.NetworkHelper
import com.capstone.capstone.ar.SimilarityPoseAligner
import com.capstone.capstone.ar.UiUpdater
import com.capstone.capstone.databinding.ActivityMainBinding
import com.capstone.capstone.network.PathPoint
import com.capstone.capstone.network.RetrofitClient
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ux.ArFragment
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private enum class Mode { LOCALIZE, MAPPING }

class MainActivity : AppCompatActivity() {

    private val AR_POLL_MS = 120L
    private val MAPPING_INTERVAL_MS = 1000L
    private val JPEG_QUALITY_DEFAULT = 80

    private lateinit var binding: ActivityMainBinding
    private lateinit var arFragment: ArFragment
    private lateinit var uploader: FrameUploader
    private lateinit var uiUpdater: UiUpdater
    private val poseAligner = SimilarityPoseAligner()
    private val arPollHandler = Handler(Looper.getMainLooper())
    private var arPollerStarted = false

    private var mode: Mode = Mode.LOCALIZE
    private var singleLocalizePending = false
    private var lastLocalizeUpdatedDebug = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RetrofitClient.init(applicationContext)
        showServerDialogIfNeeded()

        binding.mapCard.bringToFront()
        binding.mapCard.elevation = 30f

        uiUpdater = UiUpdater(binding, lifecycleScope)
        binding.tvMapStatus.text = "Path not loaded"
        binding.tvArDebugStatus.text = "Ready to localize"
        updateServerSubtitle()
        binding.topBar.setOnLongClickListener {
            showServerDialog(force = true)
            true
        }

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment.setOnSessionConfigurationListener { _, config ->
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        uploader = FrameUploader(
            arFrameProvider = { arFragment.arSceneView?.arFrame },
            onStatus = { msg -> uiUpdater.setStatus(msg) },
            sendMappingImage = { jpeg -> NetworkHelper.sendMappingImage(jpeg, uiUpdater) },
            uploadLocalize = { frame, jpeg ->
                NetworkHelper.uploadLocalize(
                    frame,
                    jpeg,
                    ::handlePoseResponse,
                    ::handleLocalizeFinished,
                    uiUpdater
                )
            },
            onCamPoseText = null
        )

        binding.chipUpload.text = "Single Localize"
        setupUiButtons()
        startArTrackingPoller()
    }

    override fun onPause() {
        super.onPause()
        uploader.stop()
        singleLocalizePending = false
        stopArTrackingPoller()
        NetworkHelper.resetLocalizeState()
        uiUpdater.stopTimer()
    }

    override fun onResume() {
        super.onResume()
        startArTrackingPoller()
    }

    private fun showServerDialogIfNeeded() {
        if (!RetrofitClient.hasSavedBaseUrl(applicationContext)) {
            showServerDialog(force = false)
        }
    }

    private fun updateServerSubtitle() {
        binding.topBar.subtitle = "Server: ${RetrofitClient.getBaseUrl(applicationContext)}"
    }

    private fun showServerDialog(force: Boolean) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint = "http://192.168.1.100:5001/"
            setSingleLine(true)
            setText(RetrofitClient.getBaseUrl(applicationContext))
            selectAll()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Backend Server")
            .setMessage("Enter the backend server URL shown in the Flask server terminal. The phone and server must be on the same Wi-Fi network.")
            .setView(input)
            .setPositiveButton("Save", null)
            .apply {
                if (force) {
                    setNegativeButton("Cancel", null)
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val savedUrl = RetrofitClient.updateBaseUrl(applicationContext, input.text.toString())
                    updateServerSubtitle()
                    Toast.makeText(this, "Server saved: $savedUrl", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    input.error = e.message ?: "Invalid server URL"
                }
            }
        }

        dialog.setCancelable(force)
        dialog.show()
    }

    private fun setupUiButtons() {
        binding.chipMode.setOnCheckedChangeListener { _, checked ->
            mode = if (checked) Mode.MAPPING else Mode.LOCALIZE
            if (!checked) {
                updateAlignmentStatus()
                binding.chipUpload.text = "Single Localize"
            } else {
                binding.chipUpload.text = "Upload"
            }
            binding.chipMode.text = "Mode Switch"
            uiUpdater.setStatus(if (checked) "Mapping mode" else "Localize mode")
        }

        binding.chipUpload.setOnCheckedChangeListener { _, checked ->
            if (mode == Mode.LOCALIZE) {
                if (!checked) {
                    if (!singleLocalizePending) {
                        uploader.stop()
                        NetworkHelper.resetLocalizeState()
                        uiUpdater.stopTimer()
                        uiUpdater.setStatus("Idle")
                    }
                    return@setOnCheckedChangeListener
                }

                if (singleLocalizePending) {
                    binding.chipUpload.isChecked = false
                    return@setOnCheckedChangeListener
                }

                singleLocalizePending = true
                lastLocalizeUpdatedDebug = false
                binding.chipUpload.isEnabled = false
                binding.chipStatus.text = "Localize once"
                NetworkHelper.resetLocalizeState()
                uiUpdater.startTimer()
                uiUpdater.setStatus("Localize request sent")
                binding.tvArDebugStatus.text = "Localizing… keep the phone steady"
                uploader.captureLocalizeOnce(
                    jpegQuality = JPEG_QUALITY_DEFAULT,
                    parentScope = lifecycleScope
                )
                return@setOnCheckedChangeListener
            }

            if (checked) {
                binding.chipStatus.text = "Running"
                NetworkHelper.resetLocalizeState()
                uiUpdater.startTimer()
                uploader.start(
                    modeMapping = true,
                    intervalMs = MAPPING_INTERVAL_MS,
                    jpegQuality = JPEG_QUALITY_DEFAULT,
                    parentScope = lifecycleScope
                )
                uiUpdater.setStatus("Uploading")
            } else {
                uploader.stop()
                NetworkHelper.resetLocalizeState()
                uiUpdater.stopTimer()
                uiUpdater.setStatus("Idle")
            }
        }

        binding.chipDone.setOnClickListener {
            lifecycleScope.launch {
                if (mode == Mode.MAPPING) {
                    uiUpdater.setStatus("Rebuilding…")
                    NetworkHelper.sendRebuildDone(uiUpdater)
                } else {
                    uiUpdater.setStatus("Localize mode: no target action")
                }
            }
        }

        binding.btnLoadPath.setOnClickListener {
            loadPathHistory()
        }
    }

    private fun loadPathHistory() {
        binding.tvMapStatus.text = "Loading path…"
        RetrofitClient.apiService.getPath().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (!response.isSuccessful) {
                    binding.tvMapStatus.text = "Load failed: HTTP ${response.code()}"
                    return
                }

                val body = response.body()
                if (body == null) {
                    binding.tvMapStatus.text = "No path data"
                    return
                }

                Log.d("PATH", "raw /path = $body")

                val pathObj = body.objOrNull("path")
                val mappingArray =
                    body.arrayOrNull("mapping")
                        ?: body.arrayOrNull("mapping_points")
                        ?: body.arrayOrNull("points")
                        ?: pathObj?.arrayOrNull("mapping")
                        ?: pathObj?.arrayOrNull("mapping_points")
                        ?: pathObj?.arrayOrNull("points")

                val historyArray =
                    body.arrayOrNull("history")
                        ?: pathObj?.arrayOrNull("history")

                val currentObj =
                    body.objOrNull("current")
                        ?: pathObj?.objOrNull("current")

                val mappingPoints = parsePathPoints(mappingArray)
                val historyPoints = parsePathPoints(historyArray)

                binding.miniMap.setMappingTrace(mappingPoints)

                if (historyPoints.isNotEmpty()) {
                    binding.miniMap.setLocalizeTrace(historyPoints)
                }

                if (currentObj != null && currentObj.has("x") && currentObj.has("y")) {
                    val cx = currentObj.get("x").asFloat
                    val cy = currentObj.get("y").asFloat
                    binding.miniMap.setCurrentPosition(cx, cy)
                }

                binding.tvMapStatus.text = "Path loaded · ${mappingPoints.size} map points"
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.e("PATH", "loadPathHistory failed", t)
                binding.tvMapStatus.text = "Load failed: ${t.message ?: t.javaClass.simpleName}"
            }
        })
    }

    private fun JsonObject.objOrNull(key: String): JsonObject? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonObject) return null
        return el.asJsonObject
    }

    private fun JsonObject.arrayOrNull(key: String): JsonArray? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonArray) return null
        return el.asJsonArray
    }

    private fun handlePoseResponse(json: JsonObject, arSnapshot: ArPoseSnapshot) {
        runOnUiThread {
            try {
                Log.d("POSE", "handlePoseResponse raw=$json")
                val mapPos = parseMapPosition(json)
                    ?: throw IllegalArgumentException("Missing map position in camera_position/position/x,y")
                val mapX = mapPos.first
                val mapY = mapPos.second

                binding.chipStatus.text = "Phone localized"
                binding.miniMap.setCurrentPosition(mapX, mapY)

                val added = poseAligner.addCorrespondence(arSnapshot.toPose(), mapX, mapY)
                if (added) {
                    Log.d("ALIGN", "Added AR->map point #${poseAligner.correspondenceCount()} at map=[$mapX,$mapY]")
                }
                updateAlignmentStatus(added, arSnapshot)
                lastLocalizeUpdatedDebug = true

                val quat = parseQuaternion(json)
                uiUpdater.updatePosePanel(floatArrayOf(mapX, mapY, 0f), quat)

                val pathPoints = parseLocalizePathPoints(json)
                if (pathPoints.isNotEmpty()) {
                    binding.miniMap.setLocalizeTrace(pathPoints)
                }

                uiUpdater.setStatus(
                    "Phone localized"
                )
            } catch (e: Exception) {
                Log.e("POSE", "handlePoseResponse failed", e)
                lastLocalizeUpdatedDebug = true
                binding.tvArDebugStatus.text = "Could not read the localization result"
                binding.chipStatus.text = "Parse error"
                uiUpdater.setStatus("Localization error")
            }
        }
    }

    private fun handleLocalizeFinished(success: Boolean, message: String) {
        runOnUiThread {
            singleLocalizePending = false
            binding.chipUpload.isEnabled = true
            binding.chipUpload.isChecked = false
            binding.chipStatus.text = if (success) "Localize done" else "Localize failed"
            uiUpdater.stopTimer()
            if (!success) {
                binding.tvArDebugStatus.text = "Localization failed. Try another angle."
            } else if (!lastLocalizeUpdatedDebug) {
                binding.tvArDebugStatus.text = "Localization finished"
            }
        }
    }

    private fun startArTrackingPoller() {
        if (arPollerStarted) return
        arPollerStarted = true
        arPollHandler.post(arPollRunnable)
    }

    private fun stopArTrackingPoller() {
        arPollerStarted = false
        arPollHandler.removeCallbacks(arPollRunnable)
    }

    private val arPollRunnable = object : Runnable {
        override fun run() {
            tryUpdateFromArPose()
            if (arPollerStarted) {
                arPollHandler.postDelayed(this, AR_POLL_MS)
            }
        }
    }

    private fun tryUpdateFromArPose() {
        if (mode != Mode.LOCALIZE) return
        val frame = arFragment.arSceneView?.arFrame ?: return
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return
        val mapped = poseAligner.mapPositionFrom(camera.pose) ?: return
        binding.miniMap.setCurrentPosition(mapped.first, mapped.second)

        if (poseAligner.currentTransform() != null) {
            binding.tvArDebugStatus.text = "Live tracking active"
        }
    }

    private fun updateAlignmentStatus(added: Boolean? = null, snap: ArPoseSnapshot? = null) {
        val count = poseAligner.correspondenceCount()
        val transform = poseAligner.currentTransform()
        binding.tvArDebugStatus.text = if (transform == null) {
            "Localize ${count}/2 points to enable live tracking"
        } else {
            "Alignment ready · live tracking enabled"
        }
    }


    private fun parseMapPosition(json: JsonObject): Pair<Float, Float>? {
        json.objOrNull("camera_position")?.let { obj ->
            if (obj.has("x") && obj.has("y")) return obj.get("x").asFloat to obj.get("y").asFloat
            if (obj.has("px") && obj.has("py")) return obj.get("px").asFloat to obj.get("py").asFloat
        }

        json.arrayOrNull("camera_position")?.let { arr ->
            if (arr.size() >= 2) return arr[0].asFloat to arr[1].asFloat
        }

        json.objOrNull("position")?.let { obj ->
            if (obj.has("x") && obj.has("y")) return obj.get("x").asFloat to obj.get("y").asFloat
        }

        json.arrayOrNull("position")?.let { arr ->
            if (arr.size() >= 2) return arr[0].asFloat to arr[1].asFloat
        }

        if (json.has("x") && json.has("y")) {
            return json.get("x").asFloat to json.get("y").asFloat
        }
        return null
    }

    private fun parseQuaternion(json: JsonObject): FloatArray {
        json.arrayOrNull("quaternion")?.let { return readFloatArray(it, 4) }
        json.arrayOrNull("rotation_qxyzw")?.let { return readFloatArray(it, 4) }
        json.objOrNull("quaternion")?.let { obj ->
            val out = FloatArray(4)
            val keys = listOf("x", "y", "z", "w")
            for ((i, k) in keys.withIndex()) {
                if (obj.has(k)) out[i] = obj.get(k).asFloat
            }
            return out
        }
        return FloatArray(4)
    }

    private fun parseLocalizePathPoints(json: JsonObject): List<PathPoint> {
        json.objOrNull("path")?.arrayOrNull("points")?.let { return parsePathPoints(it) }
        json.arrayOrNull("points")?.let { return parsePathPoints(it) }
        json.arrayOrNull("history")?.let { return parsePathPoints(it) }
        json.objOrNull("path")?.arrayOrNull("history")?.let { return parsePathPoints(it) }
        return emptyList()
    }

    private fun parsePathPoints(pointsArray: JsonArray?): List<PathPoint> {
        if (pointsArray == null) return emptyList()
        return pointsArray.mapNotNull { el ->
            runCatching {
                val obj = el.asJsonObject
                PathPoint(
                    x = obj.get("x").asFloat,
                    y = obj.get("y").asFloat,
                    quaternion = obj.getAsJsonArray("quaternion")?.map { it.asFloat },
                    timestamp = if (obj.has("timestamp")) obj.get("timestamp").asString else null
                )
            }.getOrNull()
        }
    }

    private fun readFloatArray(arr: JsonArray?, expected: Int): FloatArray {
        val out = FloatArray(expected)
        if (arr == null) return out
        for (i in 0 until minOf(arr.size(), expected)) out[i] = arr[i].asFloat
        return out
    }
}
