package com.example.resume.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.resume.R

class RoundProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border_soft)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary_slate)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textAlign = Paint.Align.CENTER
        textSize = 28f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 12f
    }
    private val bounds = RectF()

    private var progress = 0
    private var label = ""

    fun setScore(score: Int, label: String) {
        progress = score.coerceIn(0, 100)
        this.label = label
        invalidate()
    }

    fun setAccentColor(color: Int) {
        progressPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val diameter = (width.coerceAtMost(height) - paddingStart - paddingEnd - 18).toFloat()
        val left = (width - diameter) / 2f
        val top = 8f
        bounds.set(left, top, left + diameter, top + diameter)
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)
        canvas.drawArc(bounds, -90f, progress * 3.6f, false, progressPaint)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        canvas.drawText("$progress", centerX, centerY + 2f, scorePaint)
        canvas.drawText(label, centerX, centerY + 24f, labelPaint)
    }
}
