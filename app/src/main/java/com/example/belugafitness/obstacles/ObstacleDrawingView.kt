package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ObstacleDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    private val yValue: Float = 0.25f // Default Y from top
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val screenHeight = height.toFloat()
        val yPosition = screenHeight * yValue

        canvas.drawLine(0f, yPosition, width.toFloat(), yPosition, paint)

        // canvas.drawRect(50f, yValue, width - 50f, yValue + 100f, paint)
    }
}