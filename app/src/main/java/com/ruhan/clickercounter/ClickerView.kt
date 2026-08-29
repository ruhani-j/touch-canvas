package com.ruhan.clickercounter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

enum class DrawMode { WHITEBOARD, POINTER }

class ClickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mode: DrawMode = DrawMode.WHITEBOARD

    private val density = context.resources.displayMetrics.density
    private val initialRadius = 30f * density
    private val radiusIncrement = 15f * density
    private val maxRadius = 120f * density
    private val snapRadius = 60f * density

    var currentColor: Int = Color.parseColor("#1A1A2E")

    // Whiteboard
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val paths = mutableListOf<Pair<Path, Int>>()
    private val activePointers = mutableMapOf<Int, Path>()

    // Pointer
    private data class PointerCircle(
        val id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Int = 200,
        var animator: ValueAnimator? = null
    )
    private val circles = mutableListOf<PointerCircle>()
    private var nextId = 0
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6C63FF")
        style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (mode) {
            DrawMode.WHITEBOARD -> handleWhiteboard(event)
            DrawMode.POINTER -> handlePointer(event)
        }
    }

    private fun handleWhiteboard(event: MotionEvent): Boolean {
        val idx = event.actionIndex
        val pid = event.getPointerId(idx)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val path = Path().apply { moveTo(event.getX(idx), event.getY(idx)) }
                activePointers[pid] = path
                paths.add(Pair(path, currentColor))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    activePointers[event.getPointerId(i)]?.lineTo(event.getX(i), event.getY(i))
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> activePointers.remove(pid)
        }
        return true
    }

    private fun handlePointer(event: MotionEvent): Boolean {
        val idx = event.actionIndex
        val x = event.getX(idx)
        val y = event.getY(idx)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val nearby = circles.firstOrNull { c ->
                    sqrt(((c.x - x) * (c.x - x) + (c.y - y) * (c.y - y)).toDouble()) < snapRadius
                }
                if (nearby != null) {
                    nearby.radius = (nearby.radius + radiusIncrement).coerceAtMost(maxRadius)
                    nearby.alpha = 200
                    nearby.animator?.cancel()
                    startFadeOut(nearby)
                } else {
                    val circle = PointerCircle(id = nextId++, x = x, y = y, radius = initialRadius)
                    circles.add(circle)
                    startFadeOut(circle)
                }
                invalidate()
            }
        }
        return true
    }

    private fun startFadeOut(circle: PointerCircle) {
        ValueAnimator.ofInt(circle.alpha, 0).apply {
            duration = 2000
            addUpdateListener {
                circle.alpha = it.animatedValue as Int
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    circles.remove(circle)
                    invalidate()
                }
            })
            circle.animator = this
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (mode) {
            DrawMode.WHITEBOARD -> paths.forEach { (path, color) ->
                strokePaint.color = color
                canvas.drawPath(path, strokePaint)
            }
            DrawMode.POINTER -> circles.toList().forEach { c ->
                circlePaint.alpha = c.alpha
                canvas.drawCircle(c.x, c.y, c.radius, circlePaint)
            }
        }
    }

    fun clear() {
        paths.clear()
        activePointers.clear()
        circles.forEach { it.animator?.cancel() }
        circles.clear()
        invalidate()
    }
}
