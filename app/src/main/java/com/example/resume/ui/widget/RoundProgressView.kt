package com.example.resume.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.resume.R

class RoundProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Shadow paints
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.card_shadow_dark)
        style = Paint.Style.FILL
    }
    private val shadowArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 14f
    }

    // Track (background ring)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border_soft)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    // Progress (colored arc) — will use gradient
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    // Inner glow / background fill
    private val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Score number text
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textAlign = Paint.Align.CENTER
        textSize = 36f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isFakeBoldText = true
    }

    // Label text below score
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 13f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val bounds = RectF()
    private val shadowBounds = RectF()

    private var progress = 0
    private var label = ""
    private var accentColor = ContextCompat.getColor(context, R.color.primary_slate)
    private var accentColorLight = ContextCompat.getColor(context, R.color.primary_slate_light)

    private var animator: ValueAnimator? = null

    fun setScore(score: Int, label: String) {
        animator?.cancel()
        val start = this.progress
        animator = ValueAnimator.ofInt(start, score.coerceIn(0, 100)).apply {
            duration = 900L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                this@RoundProgressView.progress = anim.animatedValue as Int
                this@RoundProgressView.label = label
                invalidate()
            }
            start()
        }
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        // Determine light variant based on color
        accentColorLight = when (color) {
            ContextCompat.getColor(context, R.color.primary_slate) -> ContextCompat.getColor(context, R.color.primary_slate_light)
            ContextCompat.getColor(context, R.color.secondary_sage) -> ContextCompat.getColor(context, R.color.secondary_sage_light)
            ContextCompat.getColor(context, R.color.accent_gold) -> ContextCompat.getColor(context, R.color.accent_gold_dark)
            else -> Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        }
        updateGradient()
        invalidate()
    }

    private fun updateGradient() {
        progressPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            accentColorLight, accentColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val diameter = (width.coerceAtMost(height) - paddingStart - paddingEnd - 20).toFloat()
        val left = (width - diameter) / 2f
        val top = 10f

        // Shadow offset for 3D effect
        bounds.set(left, top, left + diameter, top + diameter)
        shadowBounds.set(left, top + 3f, left + diameter, top + diameter + 3f)

        // Draw shadow arc
        shadowArcPaint.color = ContextCompat.getColor(context, R.color.card_shadow_dark)
        shadowArcPaint.shader = null
        canvas.drawArc(shadowBounds, 0f, 360f, false, shadowArcPaint)

        // Draw track ring
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)

        // Draw inner circle background (subtle)
        val innerRadius = diameter * 0.62f
        innerCirclePaint.color = ContextCompat.getColor(context, R.color.muted_panel)
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), innerRadius, innerCirclePaint)

        // Draw progress arc with gradient
        updateGradient()
        canvas.drawArc(bounds, -90f, progress * 3.6f, false, progressPaint)

        // Draw score text with shadow for depth
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        // Score shadow
        scorePaint.color = ContextCompat.getColor(context, R.color.card_shadow_dark)
        canvas.drawText("$progress", centerX + 1f, centerY + 3f + 2f, scorePaint)

        // Score text
        scorePaint.color = ContextCompat.getColor(context, R.color.text_primary)
        canvas.drawText("$progress", centerX, centerY + 2f, scorePaint)

        // Label
        canvas.drawText(label, centerX, centerY + 26f, labelPaint)
    }
}

