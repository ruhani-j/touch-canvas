package com.ruhan.touchcanvas

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class DrawFragment : Fragment() {

    private lateinit var clickerView: TouchCanvasView
    private lateinit var colorPalette: LinearLayout

    private val prefs get() = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val isDark get() = prefs.getBoolean("dark_mode", false)

    private val swatchColors = listOf(
        "#1A1A2E", "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA", "#E91E63",
    )
    private val darkSwatchColors = listOf(
        "#7986CB", "#EF5350", "#42A5F5", "#66BB6A", "#FFA726", "#AB47BC", "#EC407A",
    )
    private var selectedColorIndex = 0
    private var eraserSelected = false

    private val thicknesses = listOf(2f, 4f, 10f)
    private val thicknessDotFractions = listOf(0.22f, 0.48f, 0.82f)
    private var selectedThicknessIndex = 1
    private val thicknessViews = mutableListOf<ThicknessDotView>()

    private inner class ThicknessDotView(
        context: Context,
        val dotFraction: Float,
    ) : View(context) {
        var isThicknessSelected = false

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(cx, cy)
            val density = resources.displayMetrics.density

            bgPaint.color = if (isDark) Color.parseColor("#424242") else Color.parseColor("#E0E0E0")
            canvas.drawCircle(cx, cy, r, bgPaint)

            dotPaint.color = if (eraserSelected) {
                if (isDark) Color.parseColor("#BDBDBD") else Color.parseColor("#9E9E9E")
            } else {
                effectiveSwatchColors()[selectedColorIndex]
            }
            canvas.drawCircle(cx, cy, r * dotFraction, dotPaint)

            if (isThicknessSelected) {
                ringPaint.color = if (isDark) Color.parseColor("#9E9E9E") else Color.WHITE
                ringPaint.strokeWidth = 3f * density
                canvas.drawCircle(cx, cy, r - ringPaint.strokeWidth / 2f, ringPaint)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_draw, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickerView = view.findViewById(R.id.clickerView)
        colorPalette = view.findViewById(R.id.colorPalette)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnClear)

        clickerView.mode = DrawMode.WHITEBOARD
        clickerView.currentStrokeWidthDp = thicknesses[selectedThicknessIndex]
        setupColorPalette()
        if (!eraserSelected) {
            clickerView.currentColor = effectiveSwatchColors()[selectedColorIndex]
        }

        btnClear.setOnClickListener { clickerView.clear() }
    }

    private fun effectiveSwatchColors(): List<Int> {
        val palette = if (isDark) darkSwatchColors else swatchColors
        return palette.map { Color.parseColor(it) }
    }

    private fun setupColorPalette() {
        colorPalette.removeAllViews()
        thicknessViews.clear()
        val density = requireContext().resources.displayMetrics.density
        val swatchSize = (32 * density).toInt()
        val swatchMargin = (5 * density).toInt()
        val colors = effectiveSwatchColors()

        // Thickness pickers
        thicknesses.forEachIndexed { i, thickness ->
            val dotView = ThicknessDotView(requireContext(), thicknessDotFractions[i]).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
                }
                isThicknessSelected = (i == selectedThicknessIndex)
                setOnClickListener {
                    eraserSelected = false
                    clickerView.mode = DrawMode.WHITEBOARD
                    clickerView.currentColor = effectiveSwatchColors()[selectedColorIndex]
                    selectedThicknessIndex = i
                    clickerView.currentStrokeWidthDp = thickness
                    refreshPaletteHighlights()
                }
            }
            thicknessViews.add(dotView)
            colorPalette.addView(dotView)
        }

        // Divider between thickness and color sections
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(swatchSize - swatchMargin * 2, (1 * density).toInt()).apply {
                setMargins(swatchMargin * 2, (4 * density).toInt(), swatchMargin * 2, (4 * density).toInt())
            }
            setBackgroundColor(if (isDark) Color.parseColor("#555555") else Color.parseColor("#CCCCCC"))
        }
        colorPalette.addView(divider)

        // Color swatches
        colors.forEachIndexed { index, color ->
            val swatch = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
                }
                background = makeSwatchDrawable(color, !eraserSelected && index == selectedColorIndex)
                setOnClickListener {
                    eraserSelected = false
                    selectedColorIndex = index
                    clickerView.mode = DrawMode.WHITEBOARD
                    clickerView.currentColor = color
                    refreshPaletteHighlights()
                }
            }
            colorPalette.addView(swatch)
        }

        val eraserBgColor = if (isDark) Color.parseColor("#424242") else Color.parseColor("#E0E0E0")
        val iconTint = if (isDark) Color.parseColor("#BDBDBD") else Color.parseColor("#757575")
        val pad = (7 * density).toInt()

        val eraserView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                setMargins(swatchMargin, swatchMargin + (8 * density).toInt(), swatchMargin, swatchMargin)
            }
            setImageResource(R.drawable.ic_eraser)
            imageTintList = ColorStateList.valueOf(iconTint)
            background = makeSwatchDrawable(eraserBgColor, eraserSelected)
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                eraserSelected = true
                clickerView.mode = DrawMode.ERASER
                refreshPaletteHighlights()
            }
        }
        colorPalette.addView(eraserView)
    }

    private fun makeSwatchDrawable(color: Int, selected: Boolean): GradientDrawable {
        val density = resources?.displayMetrics?.density ?: 1f
        val strokePx = (3 * density).toInt()
        val strokeColor = if (isDark) Color.DKGRAY else Color.WHITE
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) setStroke(strokePx, strokeColor)
        }
    }

    private fun refreshThicknessHighlights() {
        thicknessViews.forEachIndexed { i, v ->
            v.isThicknessSelected = (i == selectedThicknessIndex)
            v.invalidate()
        }
    }

    private fun refreshPaletteHighlights() {
        refreshThicknessHighlights()
        val colors = effectiveSwatchColors()
        val colorStartIndex = thicknessViews.size + 1  // +1 for divider
        colors.forEachIndexed { index, color ->
            colorPalette.getChildAt(colorStartIndex + index)?.background =
                makeSwatchDrawable(color, !eraserSelected && index == selectedColorIndex)
        }
        val eraserBgColor = if (isDark) Color.parseColor("#424242") else Color.parseColor("#E0E0E0")
        (colorPalette.getChildAt(colorStartIndex + colors.size) as? ImageView)?.background =
            makeSwatchDrawable(eraserBgColor, eraserSelected)
    }
}
