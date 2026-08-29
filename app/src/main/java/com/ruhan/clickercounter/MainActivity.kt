package com.ruhan.clickercounter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var clickerView: ClickerView
    private lateinit var modeToggle: MaterialButtonToggleGroup
    private lateinit var btnClear: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clickerView = findViewById(R.id.clickerView)
        modeToggle = findViewById(R.id.modeToggle)
        btnClear = findViewById(R.id.btnClear)

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

        modeToggle.check(R.id.btnWhiteboard)
    }
}
