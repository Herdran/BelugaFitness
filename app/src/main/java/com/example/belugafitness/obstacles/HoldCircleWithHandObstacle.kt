package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import androidx.core.content.ContextCompat
import com.example.belugafitness.R
import com.example.belugafitness.posedetection.OverlayView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.security.AccessController.getContext
import kotlin.random.Random

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

    private val bubbleColors = listOf(
        "#F8AFE3",
        "#85E3FF",
        "#85E3FF",
        "#88F4E7",
    )

    override val obstacleTxt: String = "Hold the bubble with your hand"

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
        obstacleDrawingViewWidth: Float,
        context: Context
    ) {
        val bubbleDrawable = ContextCompat.getDrawable(context, R.drawable.bubble) as LayerDrawable
        val xPosition = obstacleDrawingViewWidth * xValue
        val yPosition = obstacleDrawingViewHeight * yValue
        val radius = obstacleDrawingViewWidth * rValue

        bubbleDrawable.let { drawable ->
            val solidDrawable = drawable.getDrawable(0) as GradientDrawable
            val newBubbleColor = Color.parseColor(bubbleColors[Random.nextInt(bubbleColors.size)])
            solidDrawable.setColor(newBubbleColor)
            drawable.alpha = 170
            drawable.setBounds((xPosition-radius).toInt(), (yPosition-radius).toInt(), (xPosition + radius).toInt(), (yPosition + radius).toInt())
            drawable.draw(canvas)
        }
        bubbleDrawable.draw(canvas)
    }
}