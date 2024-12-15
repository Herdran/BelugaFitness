package com.example.belugafitness.obstacles

import android.graphics.Canvas
import android.graphics.Paint
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

interface Obstacle {
    fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float
    ): Boolean

    fun draw(canvas: Canvas, paint: Paint, width: Float, obstacleDrawingViewHeight: Float)
}