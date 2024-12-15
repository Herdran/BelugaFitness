package com.example.belugafitness

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.belugafitness.obstacles.Obstacle
import com.example.belugafitness.obstacles.ObstacleDrawingView
import com.example.belugafitness.obstacles.RectangleFromTopObstacle
import com.example.belugafitness.posedetection.OverlayView
import com.example.belugafitness.posedetection.PoseLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DetectionActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var obstacleDrawingView: ObstacleDrawingView

    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var pauseAnalysis = false
    private var preview: Preview? = null

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var imageAnalyzer: ImageAnalysis? = null

    private var currentDelegate: Int = PoseLandmarkerHelper.DELEGATE_CPU

    private var obstacles: ArrayList<Obstacle> = ArrayList()
    private var obstacleId: Int = 1
    private var isCountdownActive: Boolean = false
    private var countdownJob: Job? = null // To track the countdown Coroutine
    var isPoseOutsideObstacle: Boolean? = null

    private lateinit var resultTxt: TextView

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    this,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        obstacles.add(RectangleFromTopObstacle(0.5f))
        obstacles.add(RectangleFromTopObstacle(0.4f))
        obstacles.add(RectangleFromTopObstacle(0.2f))

        viewFinder = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlay)
        resultTxt = findViewById(R.id.resultText)

        findViewById<Button>(R.id.image_capture_button).setOnClickListener {
            takePhoto()
        }

        obstacleDrawingView = ObstacleDrawingView(
            this, attrs = null
        )

        obstacleDrawingView.apply {
            obstacle = obstacles[0]
        }

        val rootLayout = findViewById<FrameLayout>(android.R.id.content)
        rootLayout.addView(obstacleDrawingView)

        poseLandmarkerHelper =
            PoseLandmarkerHelper(
                context = this,
                minPosePresenceConfidence = 0.8f,
                minPoseDetectionConfidence = 0.8f,
                minPoseTrackingConfidence = 0.8f,
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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        pauseAnalysis = true
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.GERMAN)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-image")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture has failed: ${exception.message}", exception)
                    pauseAnalysis = false
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    pauseAnalysis = false
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val resolutionSelector = ResolutionSelector.Builder().apply {
                setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            }.build()

            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        if (this::poseLandmarkerHelper.isInitialized) {
                            it.setAnalyzer(
                                cameraExecutor
                            ) { imageProxy ->
                                poseLandmarkerHelper.detectLiveStream(
                                    imageProxy,
                                    isFrontCamera = true
                                )
                            }
                        }
                    }

//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Use case binding failed", exception)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = viewFinder.display.rotation
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
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

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
            }.toTypedArray()
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            overlayView.setResults(
                resultBundle.results[0],
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth
            )
            isPoseOutsideObstacle = obstacleDrawingView.obstacle?.checkCondition(
                resultBundle.results[0], overlayView, obstacleDrawingView.height.toFloat()
            )
            Log.i("logcondition", isPoseOutsideObstacle.toString())

            if (isPoseOutsideObstacle == true) {
                if (!isCountdownActive) {
                    resultTxt.text = "OKAY"
                    startCountdownBeforeNextObstacle()
                }
            } else {
                resultTxt.text = "NOT OKAY"
                if (isCountdownActive) {
                    stopCountdown()
                }
            }
        }
    }


    private fun startCountdownBeforeNextObstacle() {
        isCountdownActive = true
        countdownJob = lifecycleScope.launch {
            var countdown = 3 // Seconds to wait
            while (countdown > 0 && isCountdownActive) {
                resultTxt.text = "Next in $countdown..."
                delay(1000) // Wait for 1 second
                countdown--

                if (isPoseOutsideObstacle == false) {
                    stopCountdown()
                    break
                }
            }

            // After countdown ends, switch to the next obstacle if still outside
            if (countdown == 0 && isCountdownActive) {
                obstacleDrawingView.obstacle = obstacles[obstacleId]
                obstacleDrawingView.invalidate()
                if (obstacleId < obstacles.size - 1)
                    obstacleId++
                resultTxt.text = "NOT OKAY" // Reset the message
            }
            isCountdownActive = false
        }
    }

    private fun stopCountdown() {
        isCountdownActive = false
        countdownJob?.cancel() // Cancel the countdown coroutine
    }
}