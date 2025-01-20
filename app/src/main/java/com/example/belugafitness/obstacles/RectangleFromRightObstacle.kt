package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.example.belugafitness.R
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class RectangleFromRightObstacle(private val xValue: Float = 0.25f) : Obstacle {

    override val obstacleTxt: String = "Avoid the swordfishes"

    override fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float
    ): Boolean {
        val obstacleX = obstacleDrawingViewWidth * (1.0f - xValue)
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            return false
        }
        for (landmark in landmarks) {
            for (normalizedLandmark in landmark) {
                val pointX = overlayView.returnScaledPointPosition(
                    normalizedLandmark.x(),
                    normalizedLandmark.y()
                ).first
                if (pointX >= obstacleX) {
                    return false
                }
            }
        }
        return true
    }

    override fun draw(
        canvas: Canvas,
        paint: Paint,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float,
        context: Context
    ) {

        val obstacleDrawable = ContextCompat.getDrawable(context, R.drawable.swordfish_right)
        val xPosition = obstacleDrawingViewWidth * (1.0f - xValue)

        obstacleDrawable?.let { drawable ->
            val drawableHeight = drawable.intrinsicHeight
            val drawableWidth = drawable.intrinsicWidth
            val canvasHeight = canvas.height

            val numDrawables = canvasHeight / drawableHeight

            for (i in 0 until numDrawables) {
                val yPosition = i * drawableHeight
                drawable.setBounds(xPosition.toInt(), yPosition, xPosition.toInt() + drawableWidth, yPosition + drawableHeight)
                drawable.draw(canvas)
            }
        }
    }
}