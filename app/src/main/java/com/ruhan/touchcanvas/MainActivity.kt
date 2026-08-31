package com.ruhan.touchcanvas

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnThemeToggle: ImageButton

    private lateinit var drawFragment: DrawFragment
    private lateinit var laserFragment: LaserFragment
    private lateinit var counterFragment: CounterFragment

    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }
    private var isDark = false

    override fun onCreate(savedInstanceState: Bundle?) {
        isDark = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)

        drawFragment = supportFragmentManager.findFragmentByTag("draw") as? DrawFragment ?: DrawFragment()
        laserFragment = supportFragmentManager.findFragmentByTag("laser") as? LaserFragment ?: LaserFragment()
        counterFragment = supportFragmentManager.findFragmentByTag("counter") as? CounterFragment ?: CounterFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, counterFragment, "counter")
                .add(R.id.fragmentContainer, laserFragment, "laser")
                .add(R.id.fragmentContainer, drawFragment, "draw")
                .hide(laserFragment)
                .hide(counterFragment)
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val (toShow, toHide1, toHide2) = when (item.itemId) {
                R.id.nav_draw -> Triple(drawFragment, laserFragment, counterFragment)
                R.id.nav_laser -> Triple(laserFragment, drawFragment, counterFragment)
                R.id.nav_counter -> Triple(counterFragment, drawFragment, laserFragment)
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .show(toShow)
                .hide(toHide1)
                .hide(toHide2)
                .commit()
            true
        }

        updateThemeIcon(isDark)

        btnThemeToggle.setOnClickListener {
            val nowDark = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", nowDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun updateThemeIcon(isDark: Boolean) {
        btnThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_sun else R.drawable.ic_moon
        )
    }
}
