package com.example.belugafitness

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.mediapipe.examples.poselandmarker.OverlayView
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var overlayView: OverlayView
    private lateinit var button: Button
    private lateinit var imageView: ImageView


    private val baseOptionsBuilder =
        BaseOptions.builder().setModelAssetPath("assets/pose_landmarker_full.task")

    private val optionsBuilder =
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setNumPoses(1)
            .setRunningMode(RunningMode.IMAGE)
    private val options = optionsBuilder.build()

    private var poseLandmarker: PoseLandmarker? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    uri?.let { mediaUri ->

                        val source = ImageDecoder.createSource(
                            contentResolver, mediaUri
                        )
                        ImageDecoder.decodeBitmap(source)
                            .copy(Bitmap.Config.ARGB_8888, false)?.let { bitmap ->

                                val mpImage = BitmapImageBuilder(bitmap.scaleDown(512f)).build()

                                val result = poseLandmarker?.detect(mpImage)

                                result?.let {
                                    overlayView.setResults(
                                        it, mpImage.height,
                                        mpImage.width
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    imageView.load(bitmap)
                                }
                            }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        poseLandmarker = PoseLandmarker.createFromOptions(this, options)

        overlayView = findViewById(R.id.overlay)
        button = findViewById(R.id.pick_image)
        imageView = findViewById(R.id.image_view)

        button.setOnClickListener {
            getContent.launch(arrayOf("image/*"))
        }
    }

    private fun Bitmap.scaleDown(targetWidth: Float): Bitmap {
        if (targetWidth >= width) return this
        val scaleFactor = targetWidth / width
        return Bitmap.createScaledBitmap(
            this,
            (width * scaleFactor).toInt(),
            (height * scaleFactor).toInt(),
            false
        )
    }
}