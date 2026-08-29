package com.ruhan.clickercounter

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var clickerView: ClickerView
    private lateinit var modeToggle: MaterialButtonToggleGroup
    private lateinit var btnClear: MaterialButton
    private lateinit var btnThemeToggle: ImageButton

    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clickerView = findViewById(R.id.clickerView)
        modeToggle = findViewById(R.id.modeToggle)
        btnClear = findViewById(R.id.btnClear)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)

        updateThemeIcon(isDark)

        modeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                clickerView.mode = when (checkedId) {
                    R.id.btnWhiteboard -> DrawMode.WHITEBOARD
                    R.id.btnPointer -> DrawMode.POINTER
                    else -> DrawMode.WHITEBOARD
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

    private fun updateThemeIcon(isDark: Boolean) {
        btnThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_sun else R.drawable.ic_moon
        )
    }
}
