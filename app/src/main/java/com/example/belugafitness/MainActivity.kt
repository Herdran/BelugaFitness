package com.example.belugafitness

import android.os.Bundle
import android.Manifest
import android.widget.Toast
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraActivity
import java.util.Collections


class MainActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView: CameraBridgeViewBase
    private var matInput: Mat? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 1
            )
        }


        // Initialize the camera view
        cameraView = findViewById(R.id.cameraView)
        cameraView.visibility = CameraBridgeViewBase.VISIBLE
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT) // Use front camera
        cameraView.setCvCameraViewListener(this)

        if (OpenCVLoader.initDebug()){
            cameraView.enableView()
        }

    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            try {
                cameraView.enableView() // Enable the camera view
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to enable camera view: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::cameraView.isInitialized) {
            cameraView.disableView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraView.isInitialized) {
            cameraView.disableView()
        }
    }


    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        matInput = inputFrame?.rgba() // Get the current frame in RGBA format
        // You can process the frame here if needed
        return matInput ?: Mat() // Return the frame to be displayed
    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(cameraView)
    }
}