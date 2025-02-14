package com.example.belugafitness

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.belugafitness.obstacles.ObstacleDrawingView
import com.example.belugafitness.obstacles.WorkoutSection
import com.example.belugafitness.posedetection.OverlayView
import com.example.belugafitness.posedetection.PoseLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorkoutActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var obstacleDrawingView: ObstacleDrawingView

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var preview: Preview? = null

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var imageAnalyzer: ImageAnalysis? = null

    private var currentDelegate: Int = PoseLandmarkerHelper.DELEGATE_CPU

    private var obstacleId: Int = 0
    private var isCountdownActive: Boolean = false
    private var countdownJob: Job? = null
    var isPoseOutsideObstacle: Boolean? = null
    private var cameraProvider: ProcessCameraProvider? = null
    var isObstacleConditionMet: Boolean? = null

    private lateinit var resultTxt: TextView
    private var camera: Camera? = null
    private var workoutSection: WorkoutSection = WorkoutSection()

    private val PREFS_NAME = "StreakPrefs"
    private val STREAK_KEY = "streak"
    private val LAST_DATE_KEY = "lastDate"

    private lateinit var streakTextView: TextView
    private var streakDoneToday: Boolean = false
    var streak: Int = 0
    private var summaryScreen: Boolean = false

    companion object {
        private const val TAG = "CameraXApp"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
            }.toTypedArray()
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startCamera()
            } else {
                for ((permission, granted) in permissions) {
                    if (!granted) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            finish()
                        } else {
                            finish()
                            guideUserToSettings()
                        }
                    }
                }
            }
        }

    private fun guideUserToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${packageName}")
        }
        startActivity(intent)
        Toast.makeText(this, "Camera permissions are required to proceed.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_detection)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        viewFinder = findViewById(R.id.view_finder)
        overlayView = findViewById(R.id.overlay)
        obstacleDrawingView = findViewById(R.id.obstacleView)
        resultTxt = findViewById(R.id.resultText)

        workoutSection.generateWorkout()

        obstacleDrawingView.apply {
            obstacle = workoutSection.obstaclesList[0]
        }

        poseLandmarkerHelper =
            PoseLandmarkerHelper(
                context = this,
                minPosePresenceConfidence = 0.5f,
                minPoseDetectionConfidence = 0.5f,
                minPoseTrackingConfidence = 0.5f,
                currentDelegate = currentDelegate,
                poseLandmarkerHelperListener = this,
                runningMode = RunningMode.LIVE_STREAM
            )

        cameraExecutor.execute {
            poseLandmarkerHelper
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        streakSetupHelper()
    }

    override fun onResume() {
        super.onResume()

        cameraExecutor.execute {
            if (poseLandmarkerHelper.isClose()) {
                poseLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::poseLandmarkerHelper.isInitialized) {
            currentDelegate = poseLandmarkerHelper.currentDelegate
            cameraExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val cameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            imageAnalyzer =
                ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { image ->
                            detectPose(image)
                        }
                    }

            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = true
            )
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = viewFinder.display.rotation
    }

    private fun requestPermissions() {
        requestMultiplePermissions.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraExecutor.awaitTermination(
            Long.MAX_VALUE,
            TimeUnit.NANOSECONDS
        )
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            overlayView.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )

            isObstacleConditionMet = obstacleDrawingView.obstacle?.checkCondition(
                resultBundle.results[0], overlayView, obstacleDrawingView.height.toFloat(), obstacleDrawingView.width.toFloat()
            )

            if (isObstacleConditionMet == true) {
                if (!isCountdownActive) {
                    resultTxt.text = "OKAY"
                    startCountdownBeforeNextObstacle()
                }
            } else {
                resultTxt.text = workoutSection.obstaclesList[obstacleId].obstacleTxt
                if (isCountdownActive) {
                    stopCountdown()
                }
            }
        }
    }


    private fun startCountdownBeforeNextObstacle() {
        isCountdownActive = true
        countdownJob = lifecycleScope.launch {
            var countdown = 3
            while (countdown > 0 && isCountdownActive) {
                resultTxt.text = "Next in $countdown..."
                delay(1000)
                countdown--

                if (isObstacleConditionMet == false) {
                    stopCountdown()
                    break
                }
            }

            if (countdown == 0 && isCountdownActive) {
                obstacleDrawingView.invalidate()
                if (obstacleId < workoutSection.obstaclesList.size - 1){
                    obstacleId++
                    obstacleDrawingView.obstacle = workoutSection.obstaclesList[obstacleId]
                } else if (!summaryScreen) {
                    summaryScreen = true
                    workoutSummaryLayout()
                }
                resultTxt.text = workoutSection.obstaclesList[obstacleId].obstacleTxt
            }
            isCountdownActive = false
        }
    }

    private fun stopCountdown() {
        isCountdownActive = false
        countdownJob?.cancel()
    }

    private fun workoutSummaryLayout() {
        stopCountdown()
        setContentView(R.layout.workout_summary)
        streakTextView = findViewById(R.id.streak_test_view)
        if (!streakDoneToday) {
            streak++
        }

        val endWorkoutButton: Button = findViewById(R.id.button_ws)
        updateStreakText()
        endWorkoutButton.setOnClickListener {
            val currentDate = getCurrentDate()
            val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            if (currentDate != sharedPreferences.getString(LAST_DATE_KEY, "")) {
                with(sharedPreferences.edit()) {
                    putInt(STREAK_KEY, streak)
                    putString(LAST_DATE_KEY, currentDate)
                    apply()
                }
            }
            finish()
        }
    }

    fun streakSetupHelper() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        streak = sharedPreferences.getInt(STREAK_KEY, 0)

        val lastDate = sharedPreferences.getString(LAST_DATE_KEY, "")
        val today = getCurrentDate()

        if (today == lastDate) {
            streakDoneToday = true
        }
    }

    private fun updateStreakText() {
        if (!streakDoneToday) {
            val leftDrawable = streakTextView.compoundDrawables[0]
            streakTextView.setTextColor(Color.GRAY)
            leftDrawable?.let {
                it.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                streakTextView.setCompoundDrawablesWithIntrinsicBounds(it, null, null, null)
            }
            updateStreakAnimation(streakTextView, streak - 1, streak)
        }
        streakTextView.text = "${streak}"

    }

    fun updateStreakAnimation(streakTextView: TextView, oldNumber: Int, newNumber: Int) {
        Log.i("animation", "plays")
        // Number Animation
        val numberAnimator = ValueAnimator.ofInt(oldNumber, newNumber)
        numberAnimator.duration = 600
        numberAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            streakTextView.text = animatedValue.toString()
        }

        // Color Animations for Text and Drawable
        val colorAnimator = ObjectAnimator.ofArgb(
            streakTextView,
            "textColor",
            Color.GRAY,
            Color.parseColor("#FFC107")
        )
        colorAnimator.duration = 300

        val leftDrawable = streakTextView.compoundDrawables[0]
        val drawableColorAnimator = ValueAnimator.ofArgb(Color.GRAY, Color.parseColor("#FFC107"))
        drawableColorAnimator.duration = 300
        drawableColorAnimator.addUpdateListener { animation ->
            val colorValue = animation.animatedValue as Int
            leftDrawable?.setColorFilter(colorValue, PorterDuff.Mode.SRC_IN)
            streakTextView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, null, null, null)
        }

        // Combine Animations (First number, then color and drawable together)
        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(numberAnimator, AnimatorSet().apply {
            playTogether(colorAnimator, drawableColorAnimator)
        })

        animatorSet.start()

    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }
}