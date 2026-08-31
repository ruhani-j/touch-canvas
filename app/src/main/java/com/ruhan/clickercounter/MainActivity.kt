package com.ruhan.clickercounter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var clickerView: ClickerView
    private lateinit var modeToggle: MaterialButtonToggleGroup
    private lateinit var btnClear: MaterialButton
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var colorPalette: LinearLayout

    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }
    private var isDark = false

    private val swatchColors = listOf(
        "#1A1A2E",
        "#E53935",
        "#1E88E5",
        "#43A047",
        "#FB8C00",
        "#8E24AA",
        "#E91E63",
    )
    private var selectedSwatchIndex = 0

    private fun invertColor(color: Int): Int =
        Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color))

    private fun effectiveSwatchColors(): List<Int> = swatchColors.map { hex ->
        val c = Color.parseColor(hex)
        if (isDark) invertColor(c) else c
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clickerView = findViewById(R.id.clickerView)
        modeToggle = findViewById(R.id.modeToggle)
        btnClear = findViewById(R.id.btnClear)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        colorPalette = findViewById(R.id.colorPalette)

        updateThemeIcon(isDark)
        setupColorPalette()
        clickerView.currentColor = effectiveSwatchColors()[selectedSwatchIndex]

        modeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                clickerView.mode = when (checkedId) {
                    R.id.btnWhiteboard -> DrawMode.WHITEBOARD
                    R.id.btnEraser -> DrawMode.ERASER
                    R.id.btnPointer -> DrawMode.POINTER
                    R.id.btnCounter -> DrawMode.COUNTER
                    else -> DrawMode.WHITEBOARD
                }
                colorPalette.visibility = if (clickerView.mode == DrawMode.WHITEBOARD) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        btnClear.setOnClickListener { clickerView.clear() }

        btnThemeToggle.setOnClickListener {
            val nowDark = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", nowDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        modeToggle.check(R.id.btnWhiteboard)
    }

    private fun setupColorPalette() {
        val density = resources.displayMetrics.density
        val swatchSize = (32 * density).toInt()
        val swatchMargin = (5 * density).toInt()
        val colors = effectiveSwatchColors()

        colors.forEachIndexed { index, color ->
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)
                }
                background = makeSwatchDrawable(color, index == selectedSwatchIndex)
                setOnClickListener {
                    clickerView.currentColor = color
                    updateSwatchSelection(index)
                }
            }
            colorPalette.addView(swatch)
        }
    }

    private fun makeSwatchDrawable(color: Int, selected: Boolean): GradientDrawable {
        val strokePx = (3 * resources.displayMetrics.density).toInt()
        val strokeColor = if (isDark) Color.DKGRAY else Color.WHITE
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) {
                setStroke(strokePx, strokeColor)
            }
        }
    }

    private fun updateSwatchSelection(newIndex: Int) {
        effectiveSwatchColors().forEachIndexed { index, color ->
            val swatch = colorPalette.getChildAt(index)
            swatch.background = makeSwatchDrawable(color, index == newIndex)
        }
        selectedSwatchIndex = newIndex
    }

    private fun updateThemeIcon(isDark: Boolean) {
        btnThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_sun else R.drawable.ic_moon
        )
    }
}
