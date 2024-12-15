package com.example.belugafitness.obstacles

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class RectangleFromTopObstacle(private val yValue: Float = 0.25f) : Obstacle {
    override fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float
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
        width: Float,
        obstacleDrawingViewHeight: Float
    ) {
        val yPosition = obstacleDrawingViewHeight * yValue

        Log.i("HEIGHT", yPosition.toString())
        canvas.drawRect(0f, 0f, width, yPosition, paint)
    }
}