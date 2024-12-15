package com.example.belugafitness.obstacles

import android.graphics.Canvas
import android.graphics.Paint
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class HoldCircleWithHandObstacle(private val xValue: Float = 0.25f, private val yValue: Float = 0.25f, private val rValue: Float = 0.1f) : Obstacle {
    private val handsLandmarkIndices = listOf(
        15, // left wrist
        16, // right wrist
        17, // left pinky
        18, // right pinky
        19, // left index
        20, // right index
        21, // left thumb
        22  // right thumb
    )

    override fun checkCondition(
        result: PoseLandmarkerResult,
        overlayView: OverlayView,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float
    ): Boolean {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            return false
        }
        val xCirclePosition = obstacleDrawingViewWidth * xValue
        val yCirclePosition = obstacleDrawingViewHeight * yValue
        val radius = obstacleDrawingViewWidth * rValue

        for (landmark in landmarks) {
            for (index in handsLandmarkIndices) {
                val handScaledLandmark = overlayView.returnScaledPointPosition(
                    landmark[index].x(),
                    landmark[index].y()
                )
                val handLandmarkCircleDistance = (handScaledLandmark.first - xCirclePosition) * (handScaledLandmark.first - xCirclePosition) +
                        (handScaledLandmark.second - yCirclePosition) * (handScaledLandmark.second - yCirclePosition)

                if (handLandmarkCircleDistance <= radius * radius){
                    return true
                }
            }
        }
        return false
    }

    override fun draw(
        canvas: Canvas,
        paint: Paint,
        obstacleDrawingViewHeight: Float,
        obstacleDrawingViewWidth: Float
    ) {
        val xPosition = obstacleDrawingViewWidth * xValue
        val yPosition = obstacleDrawingViewHeight * yValue
        val radius = obstacleDrawingViewWidth * rValue

        canvas.drawCircle(xPosition, yPosition, radius, paint)
    }
}