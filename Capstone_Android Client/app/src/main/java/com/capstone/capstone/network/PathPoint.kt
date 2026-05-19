package com.capstone.capstone.network

data class PathPoint(
    val x: Float,
    val y: Float,
    val quaternion: List<Float>? = null,
    val timestamp: String? = null
)
