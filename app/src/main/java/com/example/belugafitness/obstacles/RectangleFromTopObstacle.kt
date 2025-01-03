package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.belugafitness.R
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class RectangleFromTopObstacle(private val yValue: Float = 0.25f) : Obstacle {
    override fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float
    ): Boolean {

        val obstacleY = obstacleDrawingViewHeight * yValue
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            return false
        }
        for (landmark in landmarks) {
            for (normalizedLandmark in landmark) {
                val pointY = overlayView.returnScaledPointPosition(
                    normalizedLandmark.x(),
                    normalizedLandmark.y()
                ).second
                if (pointY <= obstacleY) {
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
        val obstacleDrawable = ContextCompat.getDrawable(context, R.drawable.crab)
        val yPosition = obstacleDrawingViewHeight * yValue

        obstacleDrawable?.let { drawable ->
            val drawableHeight = drawable.intrinsicHeight
            val drawableWidth = drawable.intrinsicWidth
            val canvasWidth = canvas.width
            drawable.setBounds(-(drawableWidth-canvasWidth)/2, (yPosition - drawableHeight).toInt(), (obstacleDrawingViewWidth + (drawableWidth-canvasWidth)/2).toInt(), yPosition.toInt())
            drawable.draw(canvas)
        }
    }
}