package com.capstone.capstone.ar

import com.google.ar.core.Pose
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Estimate a 2D similarity transform from ARCore planar coordinates to map coordinates.
 * AR planar coordinates use (-x, -z) to match the horizontally flipped reconstruction-map display space.
 * This keeps the reconstructed/yellow path corrected while giving ARCore tracking the same left/right handedness.
 */
class SimilarityPoseAligner {

    data class Correspondence(
        val arX: Float,
        val arY: Float,
        val mapX: Float,
        val mapY: Float
    )

    data class Transform(
        val scale: Float,
        val thetaRad: Float,
        val tx: Float,
        val ty: Float
    ) {
        fun map(arX: Float, arY: Float): Pair<Float, Float> {
            val c = cos(thetaRad)
            val s = sin(thetaRad)
            val mx = scale * (c * arX - s * arY) + tx
            val my = scale * (s * arX + c * arY) + ty
            return mx to my
        }
    }

    private val correspondences = mutableListOf<Correspondence>()
    private var transform: Transform? = null

    fun reset() {
        correspondences.clear()
        transform = null
    }

    fun correspondenceCount(): Int = correspondences.size

    fun currentTransform(): Transform? = transform

    fun addCorrespondence(pose: Pose, mapX: Float, mapY: Float): Boolean {
        val arX = -pose.tx()
        val arY = -pose.tz()
        return addCorrespondence(arX, arY, mapX, mapY)
    }

    fun addCorrespondence(arX: Float, arY: Float, mapX: Float, mapY: Float): Boolean {
        if (correspondences.any { distance(it.arX, it.arY, arX, arY) < 0.02f }) {
            return false
        }
        correspondences += Correspondence(arX, arY, mapX, mapY)
        recomputeTransform()
        return true
    }

    fun mapPositionFrom(pose: Pose): Pair<Float, Float>? {
        val t = transform ?: return null
        return t.map(-pose.tx(), -pose.tz())
    }

    private fun recomputeTransform() {
        // The alignment requested here is defined by two localization pairs:
        // A = a2 - a1 in ARCore coordinates and B = b2 - b1 in reconstruction-map
        // coordinates. Keeping the first two pairs as the calibration pair makes those
        // two reconstructed-map points and transformed ARCore points overlap exactly.
        transform = solveSimilarity(correspondences.take(2))
    }

    private fun solveSimilarity(points: List<Correspondence>): Transform? {
        if (points.size < 2) return null

        val n = points.size.toFloat()
        val arCx = points.sumOf { it.arX.toDouble() }.toFloat() / n
        val arCy = points.sumOf { it.arY.toDouble() }.toFloat() / n
        val mapCx = points.sumOf { it.mapX.toDouble() }.toFloat() / n
        val mapCy = points.sumOf { it.mapY.toDouble() }.toFloat() / n

        var denom = 0f
        var dot = 0f
        var cross = 0f
        for (p in points) {
            val ax = p.arX - arCx
            val ay = p.arY - arCy
            val mx = p.mapX - mapCx
            val my = p.mapY - mapCy
            denom += ax * ax + ay * ay
            dot += ax * mx + ay * my
            cross += ax * my - ay * mx
        }
        if (abs(denom) < 1e-6f) return null

        val theta = atan2(cross, dot)
        val scale = sqrt(dot * dot + cross * cross) / denom
        if (!scale.isFinite() || scale <= 1e-6f) return null

        val c = cos(theta)
        val s = sin(theta)
        val tx = mapCx - scale * (c * arCx - s * arCy)
        val ty = mapCy - scale * (s * arCx + c * arCy)
        return Transform(scale = scale, thetaRad = theta, tx = tx, ty = ty)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
