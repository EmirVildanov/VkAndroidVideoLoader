package com.example.vkvideoloader.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.vkvideoloader.R
import timber.log.Timber

class ProgressBar(context: Context, private val attrs: AttributeSet) : View(context, attrs) {

    var startProgressSide = 0
    var progressBackgroundColor: Int = R.color.colorPrimaryDark
    var progressFrontColor: Int = R.color.colorAccent
    var maxProgressPoints: Int? = null
    var currentProgressPoints: Int? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, 0, 0)
        try {
            startProgressSide = typedArray.getInt(R.styleable.ProgressBar_startProgressSide, 0)
            progressBackgroundColor = typedArray.getInt(R.styleable.ProgressBar_progressBackgroundColor, R.color.colorPrimaryDark)
            progressFrontColor = typedArray.getInt(R.styleable.ProgressBar_progressFrontColor, R.color.colorAccent)
        } finally {
            typedArray.recycle()
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        val margin = 2f

        val borderPaint = Paint()
        borderPaint.color = ContextCompat.getColor(context, progressBackgroundColor)
        canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        val healthPointPercentage = if (maxProgressPoints == null || currentProgressPoints == null) {
            0f
        } else {
            (currentProgressPoints!! / maxProgressPoints!!.toFloat())
        }
        val healthWidth = ((width - margin * 2) * healthPointPercentage)

        val healthPaint = Paint()
        healthPaint.color = ContextCompat.getColor(context, progressFrontColor)

        canvas?.drawRect(0f + margin, 0f + margin, healthWidth, height - margin * 2, healthPaint)
    }

    fun update(maxPoints: Int, points: Int) {
        if (maxProgressPoints == null) {
            maxProgressPoints = maxPoints
        }
        currentProgressPoints = points
    }
}