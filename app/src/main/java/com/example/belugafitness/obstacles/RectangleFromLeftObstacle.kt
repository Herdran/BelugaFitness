package com.example.belugafitness.obstacles

import android.graphics.Canvas
import android.graphics.Paint
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
        obstacleDrawingViewWidth: Float
    ) {
        val xPosition = obstacleDrawingViewWidth * xValue

        canvas.drawRect(0f, 0f, xPosition, obstacleDrawingViewHeight, paint)
    }
}