package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.example.belugafitness.R
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class RectangleFromLeftObstacle(private val xValue: Float = 0.25f) : Obstacle {
    override fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float
    ): Boolean {
        val obstacleX = obstacleDrawingViewWidth * xValue
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
                if (pointX <= obstacleX) {
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
        val xPosition = obstacleDrawingViewWidth * xValue
        val obstacleDrawable = ContextCompat.getDrawable(context, R.drawable.swordfish_left)

        obstacleDrawable?.let { drawable ->
            val drawableHeight = drawable.intrinsicHeight
            val drawableWidth = drawable.intrinsicWidth
            val canvasHeight = canvas.height

            val numDrawables = canvasHeight / drawableHeight

            for (i in 0 until numDrawables) {
                val yPosition = i * drawableHeight
                drawable.setBounds(xPosition.toInt() - drawableWidth, yPosition, xPosition.toInt(), yPosition + drawableHeight)
                drawable.draw(canvas)
            }
        }
    }
}