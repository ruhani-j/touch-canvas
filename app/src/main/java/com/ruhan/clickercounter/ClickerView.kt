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

enum class DrawMode { WHITEBOARD, POINTER }

class ClickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mode: DrawMode = DrawMode.WHITEBOARD

    private val density = context.resources.displayMetrics.density

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

    // Laser pointer
    private data class LaserDot(
        val pointerId: Int,
        var x: Float,
        var y: Float,
        var alpha: Float = 1f,
        var animator: ValueAnimator? = null
    )

    private val activeDots = mutableMapOf<Int, LaserDot>()
    private val fadingDots = mutableListOf<LaserDot>()

    private val coreRadius = 14f * density
    private val glowRadius = 34f * density

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1744")
        style = Paint.Style.FILL
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
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
        val pid = event.getPointerId(idx)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                activeDots[pid] = LaserDot(pointerId = pid, x = event.getX(idx), y = event.getY(idx))
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activeDots[id]?.let {
                        it.x = event.getX(i)
                        it.y = event.getY(i)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                activeDots.remove(pid)?.let { dot ->
                    fadingDots.add(dot)
                    startFadeOut(dot)
                }
            }
        }
        return true
    }

    private fun startFadeOut(dot: LaserDot) {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 700
            addUpdateListener {
                dot.alpha = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadingDots.remove(dot)
                    invalidate()
                }
            })
            dot.animator = this
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
            DrawMode.POINTER -> {
                (activeDots.values.toList() + fadingDots.toList()).forEach { dot ->
                    val a = (dot.alpha * 255).toInt()
                    glowPaint.alpha = (a * 0.35f).toInt()
                    canvas.drawCircle(dot.x, dot.y, glowRadius, glowPaint)
                    glowPaint.alpha = a
                    canvas.drawCircle(dot.x, dot.y, coreRadius, glowPaint)
                    centerPaint.alpha = a
                    canvas.drawCircle(dot.x, dot.y, coreRadius * 0.45f, centerPaint)
                }
            }
        }
    }

    fun clear() {
        paths.clear()
        activePointers.clear()
        activeDots.values.forEach { it.animator?.cancel() }
        activeDots.clear()
        fadingDots.forEach { it.animator?.cancel() }
        fadingDots.clear()
        invalidate()
    }
}
