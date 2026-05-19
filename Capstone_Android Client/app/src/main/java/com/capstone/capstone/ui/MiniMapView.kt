package com.capstone.capstone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.capstone.capstone.network.PathPoint
import kotlin.math.max
import kotlin.math.min

class MiniMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val mappingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 150, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val localizePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 210, 60)
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val mappingPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(110, 180, 255)
        style = Paint.Style.FILL
    }

    private val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 255, 120)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }

    private val fitPaddingRatio = 0.18f
    private var mappingTrace: List<PointF> = emptyList()
    private var localizeTrace: List<PointF> = emptyList()
    private var currentPosition: PointF? = null

    fun setMappingTrace(points: List<PathPoint>) {
        mappingTrace = points.map { PointF(it.x, it.y) }
        invalidate()
    }

    fun setLocalizeTrace(points: List<PathPoint>) {
        localizeTrace = points.map { PointF(it.x, it.y) }
        invalidate()
    }

    fun setCurrentPosition(x: Float, y: Float) {
        currentPosition = PointF(x, y)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        val bounds = computeBounds() ?: return
        val contentW = width - paddingLeft - paddingRight
        val contentH = height - paddingTop - paddingBottom
        if (contentW <= 0 || contentH <= 0) return

        val rawW = max(bounds.maxX - bounds.minX, 1f)
        val rawH = max(bounds.maxY - bounds.minY, 1f)
        val marginX = rawW * fitPaddingRatio
        val marginY = rawH * fitPaddingRatio
        val minX = bounds.minX - marginX
        val minY = bounds.minY - marginY
        val worldW = rawW + marginX * 2f
        val worldH = rawH + marginY * 2f

        val scale = min(contentW / worldW, contentH / worldH)
        val drawnW = worldW * scale
        val drawnH = worldH * scale
        val offsetX = paddingLeft + (contentW - drawnW) / 2f
        val offsetY = paddingTop + (contentH - drawnH) / 2f

        drawPolyline(canvas, localizeTrace, localizePaint, minX, minY, scale, offsetX, offsetY)

        val radius = dp(2.5f)
        mappingTrace.forEach { pt ->
            val p = worldToScreen(pt.x, pt.y, minX, minY, scale, offsetX, offsetY)
            canvas.drawCircle(p.first, p.second, radius, mappingPointPaint)
        }

        currentPosition?.let { pt ->
            val p = worldToScreen(pt.x, pt.y, minX, minY, scale, offsetX, offsetY)
            canvas.drawCircle(p.first, p.second, dp(4.5f), currentPaint)
        }
    }

    private fun drawPolyline(
        canvas: Canvas,
        pts: List<PointF>,
        paint: Paint,
        minX: Float,
        minY: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        if (pts.size < 2) return
        for (i in 0 until pts.size - 1) {
            val p1 = worldToScreen(pts[i].x, pts[i].y, minX, minY, scale, offsetX, offsetY)
            val p2 = worldToScreen(pts[i + 1].x, pts[i + 1].y, minX, minY, scale, offsetX, offsetY)
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, paint)
        }
    }

    private fun worldToScreen(
        x: Float,
        y: Float,
        minX: Float,
        minY: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        val sx = offsetX + (x - minX) * scale
        val sy = offsetY + (y - minY) * scale
        return sx to sy
    }

    private fun computeBounds(): Bounds? {
        val all = buildList {
            addAll(mappingTrace)
            addAll(localizeTrace)
            currentPosition?.let { add(it) }
        }
        if (all.isEmpty()) return null
        val minX = all.minOf { it.x }
        val maxX = all.maxOf { it.x }
        val minY = all.minOf { it.y }
        val maxY = all.maxOf { it.y }
        return Bounds(minX, minY, maxX, maxY)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private data class Bounds(
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    )
}
