package com.example.belugafitness.obstacles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ObstacleDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 5f
        style = Paint.Style.FILL
        alpha = 128
    }

    var obstacle: Obstacle? = RectangleFromTopObstacle(0.25f)


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val screenHeight = height.toFloat()
        val screenWidth = width.toFloat()

        obstacle?.draw(canvas, paint, screenHeight, screenWidth)
    }
}