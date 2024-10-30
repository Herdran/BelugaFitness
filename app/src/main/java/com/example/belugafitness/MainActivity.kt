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
import org.opencv.core.Core
import org.opencv.core.Core.absdiff
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.util.Collections


class MainActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView: CameraBridgeViewBase
    private var matInput: Mat? = null
    private var currFrame: Mat? = null
    private var prevFrame: Mat? = null
    private var diff: Mat? = null
    // private lateinit var rgbFrame: Mat
    private var init: Boolean? = false
    private var cnts: MutableList<MatOfPoint> = mutableListOf()
    private var movement_threshold: Double = 65.0

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
        init = false

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
        currFrame = Mat()
        prevFrame = Mat()
        diff = Mat()
        //rgbFrame = Mat()
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        // You can process the frame here if needed
        matInput = inputFrame?.rgba()
        if(!init!!){
            prevFrame = inputFrame?.gray()
            init = true
            return matInput ?: Mat()
        }
        currFrame = inputFrame?.gray()

        absdiff(currFrame, prevFrame, diff)
        Imgproc.threshold(diff, diff, movement_threshold, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.findContours(diff, cnts, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        Imgproc.drawContours(matInput, cnts, -1, Scalar(255.0, 0.0, 0.0), 5)

        for(m in cnts){
            var rectangle = Imgproc.boundingRect(m)
            Imgproc.rectangle(matInput, rectangle, Scalar(0.0,0.0, 255.0), 3)
        }
        prevFrame = currFrame?.clone()
        cnts.clear()

        return matInput ?: Mat()// Return the frame to be displayed
    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(cameraView)
    }
}