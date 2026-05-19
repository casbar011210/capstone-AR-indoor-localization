package com.capstone.capstone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.atan2
import kotlin.math.min

class PoseOverlayView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var yawDeg = 0f
    private val pAxis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val pArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 7f; color = Color.RED; style = Paint.Style.STROKE
    }

    /** 传入四元数（qx,qy,qz,qw），粗略取 yaw 可视化方向 */
    fun setQuaternion(qx: Float, qy: Float, qz: Float, qw: Float) {
        // 以 Z 轴为“朝上”假设：由四元数求 yaw
        val t3 = 2f * (qw * qz + qx * qy)
        val t4 = 1f - 2f * (qy*qy + qz*qz)
        val yawRad = atan2(t3, t4)
        yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val r  = min(cx, cy) - 10f

        // 画 X/Y 简易坐标轴
        pAxis.color = Color.BLUE
        c.drawLine(cx, cy, cx + r, cy, pAxis)     // X
        pAxis.color = Color.GREEN
        c.drawLine(cx, cy, cx, cy - r, pAxis)     // Y

        // 红色箭头表示朝向（按 yaw 旋转）
        c.save()
        c.translate(cx, cy)
        c.rotate(yawDeg)
        c.drawLine(0f, 0f, 0f, -r, pArrow)
        c.drawLine(0f, -r, -12f, -r + 18f, pArrow)
        c.drawLine(0f, -r,  12f, -r + 18f, pArrow)
        c.restore()
    }
}