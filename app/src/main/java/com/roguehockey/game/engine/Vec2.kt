package com.roguehockey.game.engine

import kotlin.math.sqrt

data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus() = Vec2(-x, -y)

    fun dot(other: Vec2): Float = x * other.x + y * other.y

    fun lengthSquared(): Float = x * x + y * y

    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 1e-5f) Vec2(x / len, y / len) else ZERO
    }

    fun clampLength(maxLen: Float): Vec2 {
        val lenSq = lengthSquared()
        return if (lenSq > maxLen * maxLen && lenSq > 1e-6f) {
            val scale = maxLen / sqrt(lenSq)
            Vec2(x * scale, y * scale)
        } else {
            this
        }
    }

    fun distanceTo(other: Vec2): Float = (this - other).length()

    fun reflect(normal: Vec2): Vec2 {
        val d = 2f * dot(normal)
        return Vec2(x - d * normal.x, y - d * normal.y)
    }

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}
