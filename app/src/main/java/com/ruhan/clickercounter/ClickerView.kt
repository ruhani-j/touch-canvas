package com.ruhan.clickercounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View

enum class DrawMode { WHITEBOARD, POINTER, COUNTER }

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
    private data class TrailPoint(val x: Float, val y: Float, val time: Long)

    private class LaserTrail(val pointerId: Int) {
        val points = ArrayDeque<TrailPoint>()
        var active = true
        var lastX = 0f
        var lastY = 0f
    }

    private val activeTrails = mutableMapOf<Int, LaserTrail>()
    private val fadingTrails = mutableListOf<LaserTrail>()

    private val trailDurationMs = 2000L
    private val minDistSq = (4f * density) * (4f * density)
    private val dotRadius = 18f * density
    private val laserColor = Color.parseColor("#FF1744")

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = laserColor
    }

    // Tap counter
    private val counterTextColor: Int = run {
        val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val color = ta.getColorStateList(0)?.defaultColor ?: Color.DKGRAY
        ta.recycle()
        color
    }

    private var tapCount = 0

    private val counterNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        color = counterTextColor
    }

    private val counterLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        color = counterTextColor
        alpha = 150
    }

    private var animating = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val now = System.currentTimeMillis()
            fadingTrails.forEach { trail ->
                while (trail.points.isNotEmpty() && now - trail.points.first().time > trailDurationMs) {
                    trail.points.removeFirst()
                }
            }
            fadingTrails.removeAll { it.points.isEmpty() }

            if (fadingTrails.isNotEmpty()) {
                invalidate()
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                animating = false
                invalidate()
            }
        }
    }

    private fun ensureAnimating() {
        if (!animating) {
            animating = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (mode) {
            DrawMode.WHITEBOARD -> handleWhiteboard(event)
            DrawMode.POINTER -> handlePointer(event)
            DrawMode.COUNTER -> handleCounter(event)
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

    private fun handleCounter(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
            event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            tapCount++
            invalidate()
        }
        return true
    }

    private fun handlePointer(event: MotionEvent): Boolean {
        val idx = event.actionIndex
        val pid = event.getPointerId(idx)
        val now = System.currentTimeMillis()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(idx)
                val y = event.getY(idx)
                val trail = LaserTrail(pid).also {
                    it.points.addLast(TrailPoint(x, y, now))
                    it.lastX = x
                    it.lastY = y
                }
                activeTrails[pid] = trail
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activeTrails[id]?.let { trail ->
                        val x = event.getX(i)
                        val y = event.getY(i)
                        val dx = x - trail.lastX
                        val dy = y - trail.lastY
                        if (dx * dx + dy * dy >= minDistSq) {
                            trail.points.addLast(TrailPoint(x, y, now))
                            if (trail.points.size > 400) trail.points.removeFirst()
                            trail.lastX = x
                            trail.lastY = y
                        }
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                activeTrails.remove(pid)?.let { trail ->
                    trail.active = false
                    fadingTrails.add(trail)
                    ensureAnimating()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (mode) {
            DrawMode.WHITEBOARD -> paths.forEach { (path, color) ->
                strokePaint.color = color
                canvas.drawPath(path, strokePaint)
            }
            DrawMode.COUNTER -> {
                val cx = width / 2f
                val numSize = minOf(width, height) * 0.38f
                val baselineY = height / 2f + numSize * 0.35f
                counterNumPaint.textSize = numSize
                canvas.drawText(tapCount.toString(), cx, baselineY, counterNumPaint)
                counterLabelPaint.textSize = numSize * 0.18f
                canvas.drawText("taps", cx, baselineY + numSize * 0.22f, counterLabelPaint)
            }
            DrawMode.POINTER -> {
                val now = System.currentTimeMillis()
                (activeTrails.values.toList() + fadingTrails.toList()).forEach { trail ->
                    val pts = trail.points.toList()
                    if (pts.isEmpty()) return@forEach
                    val headTime = pts.last().time

                    pts.forEach { pt ->
                        val age = if (trail.active) headTime - pt.time else now - pt.time
                        val fraction = (1f - age.toFloat() / trailDurationMs).coerceIn(0f, 1f)
                        if (fraction == 0f) return@forEach
                        val alpha = (fraction * 255).toInt()
                        val r = dotRadius * (0.25f + 0.75f * fraction)

                        trailPaint.color = laserColor
                        trailPaint.alpha = (alpha * 0.3f).toInt()
                        canvas.drawCircle(pt.x, pt.y, r * 2f, trailPaint)
                        trailPaint.alpha = alpha
                        canvas.drawCircle(pt.x, pt.y, r, trailPaint)
                    }
                }
            }
        }
    }

    fun clear() {
        paths.clear()
        activePointers.clear()
        activeTrails.clear()
        fadingTrails.clear()
        tapCount = 0
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        animating = false
        invalidate()
    }
}
