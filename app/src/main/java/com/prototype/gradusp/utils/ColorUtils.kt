package com.prototype.gradusp.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Utility class for color operations
 */
object ColorUtils {
    /**
     * Convert Color to HSV array [hue, saturation, value]
     */
    fun colorToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val min = minOf(r, g, b)
        val max = maxOf(r, g, b)

        val v = max
        val delta = max - min

        val s = if (max != 0f) delta / max else 0f

        val h = when {
            max == min -> 0f // achromatic
            r == max -> (g - b) / delta // between yellow & magenta
            g == max -> 2 + (b - r) / delta // between cyan & yellow
            else -> 4 + (r - g) / delta // between magenta & cyan
        } * 60f

        return floatArrayOf(
            (if (h < 0) h + 360 else h), // Normalize hue to 0-360
            s,
            v
        )
    }

    /**
     * Create a Color from HSV values
     */
    fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
        val h = hue / 60
        val i = h.toInt()
        val f = h - i
        val p = value * (1 - saturation)
        val q = value * (1 - saturation * f)
        val t = value * (1 - saturation * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }

        return Color(r, g, b, alpha)
    }

    /**
     * Calculate if a color should have light or dark text
     * @return true if the color is light and should have dark text
     */
    fun isLightColor(color: Color): Boolean {
        return color.luminance() > 0.5f
    }

    /**
     * Get a contrasting text color for the given background color
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        return if (isLightColor(backgroundColor)) Color.Black else Color.White
    }
}

/**
 * Extension function to create a Color from HSV values
 */
fun Color.Companion.hsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    return ColorUtils.hsvToColor(hue, saturation, value, alpha)
}