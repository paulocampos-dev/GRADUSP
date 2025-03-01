package com.prototype.gradusp.ui.components.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

@Composable
fun HueSaturationSelector(
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float,
    onHueSaturationChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Track the center of the canvas
        val center = remember { mutableStateOf(Offset.Zero) }

        // Define the handler function outside the pointerInput blocks so both can access it
        val calculateColorFromTouch = { offset: Offset ->
            val centerX = center.value.x
            val centerY = center.value.y

            val dx = offset.x - centerX
            val dy = offset.y - centerY

            // Calculate the angle (hue)
            val angle = atan2(dy, dx)
            val hueValue = (angle / (2 * PI) * 360f).toFloat()
            val adjustedHue = (hueValue + 360) % 360 // Normalize to 0-360

            // Calculate the distance (saturation)
            val radius = minOf(centerX, centerY)
            val distance = hypot(dx, dy)
            val saturationValue = (distance / radius).coerceIn(0f, 1f)

            onHueSaturationChange(adjustedHue, saturationValue)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // Update center point
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    center.value = Offset(centerX.toFloat(), centerY.toFloat())

                    // Handle tap gestures
                    detectTapGestures { offset ->
                        calculateColorFromTouch(offset)
                    }
                }
                .pointerInput(Unit) {
                    // Handle drag gestures
                    detectDragGestures { change, _ ->
                        calculateColorFromTouch(change.position)
                    }
                }
        ) {
            val radius = minOf(size.width, size.height) / 2
            val cx = size.width / 2
            val cy = size.height / 2
            center.value = Offset(cx, cy)

            // Draw the color wheel with the current brightness and opacity
            for (angle in 0 until 360 step 1) {
                val angleRad = angle * PI / 180
                val hsColor = Color.hsv(angle.toFloat(), 1f, brightness, alpha)

                drawLine(
                    color = hsColor,
                    start = Offset(
                        x = cx + (cos(angleRad) * radius * 0.2f).toFloat(),
                        y = cy + (sin(angleRad) * radius * 0.2f).toFloat()
                    ),
                    end = Offset(
                        x = cx + (cos(angleRad) * radius).toFloat(),
                        y = cy + (sin(angleRad) * radius).toFloat()
                    ),
                    strokeWidth = 8f
                )
            }

            // Draw center circle (saturation = 0) with current brightness/opacity
            drawCircle(
                color = Color.hsv(0f, 0f, brightness, alpha),
                radius = radius * 0.2f,
                center = Offset(cx, cy)
            )

            // Draw border around the circle
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = 2f)
            )

            // Draw selector indicator
            val hueRad = hue * PI / 180
            val selectorX = cx + (cos(hueRad) * radius * saturation).toFloat()
            val selectorY = cy + (sin(hueRad) * radius * saturation).toFloat()

            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(selectorX, selectorY)
            )

            drawCircle(
                color = Color.hsv(hue, saturation, brightness, alpha),
                radius = 10f,
                center = Offset(selectorX, selectorY)
            )

            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 12f,
                style = Stroke(width = 2f),
                center = Offset(selectorX, selectorY)
            )
        }
    }
}