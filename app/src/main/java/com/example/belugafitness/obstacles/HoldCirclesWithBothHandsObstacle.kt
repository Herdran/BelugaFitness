package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.example.belugafitness.R
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.random.Random

class HoldCirclesWithBothHandsObstacle(private val xValueRight: Float = 0.25f, private val yValueRight: Float = 0.25f, private val xValueLeft: Float = 0.25f, private val yValueLeft: Float = 0.25f,private val rValue: Float = 0.1f) : Obstacle  {
    private val leftHandLandmarkIndices = listOf(
        16, // left wrist
        18, // left pinky
        20, // left index
        22  // left thumb
    )

    private val rightHandLandmarkIndices = listOf(
        15, // right wrist
        17, // right pinky
        19, // right index
        21, // right thumb
    )


    override val obstacleTxt: String = "Hold right hand on red and left hand on yellow starfish"

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
        val xCircleLeftPosition = obstacleDrawingViewWidth * xValueLeft
        val yCircleLeftPosition = obstacleDrawingViewHeight * yValueLeft
        val xCircleRightPosition = obstacleDrawingViewWidth * xValueRight
        val yCircleRightPosition = obstacleDrawingViewHeight * yValueRight
        val radius = obstacleDrawingViewWidth * rValue
        var leftCondition = false

        for (landmark in landmarks) {
            // Check left hand
            for (index in leftHandLandmarkIndices) {
                val handScaledLandmark = overlayView.returnScaledPointPosition(
                    landmark[index].x(),
                    landmark[index].y()
                )
                val handLandmarkCircleDistance = (handScaledLandmark.first - xCircleLeftPosition) * (handScaledLandmark.first - xCircleLeftPosition) +
                        (handScaledLandmark.second - yCircleLeftPosition) * (handScaledLandmark.second - yCircleLeftPosition)

                if (handLandmarkCircleDistance <= radius * radius){
                    leftCondition = true
                    break
                }
            }

            if (!leftCondition){
                return false
            }

            // Check right hand
            for (index in rightHandLandmarkIndices) {
                val handScaledLandmark = overlayView.returnScaledPointPosition(
                    landmark[index].x(),
                    landmark[index].y()
                )
                val handLandmarkCircleDistance = (handScaledLandmark.first - xCircleRightPosition) * (handScaledLandmark.first - xCircleRightPosition) +
                        (handScaledLandmark.second - yCircleRightPosition) * (handScaledLandmark.second - yCircleRightPosition)

                if (handLandmarkCircleDistance <= radius * radius && leftCondition){
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
        obstacleDrawingViewWidth: Float,
        context: Context
    ) {
        val rightDrawable = ContextCompat.getDrawable(context, R.drawable.starfish)
        val leftDrawable = ContextCompat.getDrawable(context, R.drawable.starfish_yellow)
        val xLeftPosition = obstacleDrawingViewWidth * xValueLeft
        val yLeftPosition = obstacleDrawingViewHeight * yValueLeft
        val xRightPosition = obstacleDrawingViewWidth * xValueRight
        val yRightPosition = obstacleDrawingViewHeight * yValueRight
        val radius = obstacleDrawingViewWidth * rValue

        leftDrawable.let { drawable ->
            drawable?.alpha = 170
            drawable?.setBounds((xLeftPosition-radius).toInt(), (yLeftPosition-radius).toInt(), (xLeftPosition + radius).toInt(), (yLeftPosition + radius).toInt())
            drawable?.draw(canvas)
        }

        rightDrawable.let { drawable ->
            drawable?.alpha = 170
            drawable?.setBounds((xRightPosition-radius).toInt(), (yRightPosition-radius).toInt(), (xRightPosition + radius).toInt(), (yRightPosition + radius).toInt())
            drawable?.draw(canvas)
        }

        leftDrawable?.draw(canvas)
        rightDrawable?.draw(canvas)
    }
}