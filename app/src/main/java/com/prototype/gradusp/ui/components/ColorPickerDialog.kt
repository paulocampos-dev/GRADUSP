package com.prototype.gradusp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert current color to HSV components
    val hsv = colorToHsv(currentColor)
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var alpha by remember { mutableFloatStateOf(currentColor.alpha) }

    // Calculate current color based on HSV values
    val selectedColor = remember(hue, saturation, value, alpha) {
        Color.hsv(hue, saturation, value, alpha)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selecionar Cor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color preview
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(2.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Updated Hue/Saturation selector: brightness and opacity now affect the wheel
                HueSaturationSelector(
                    hue = hue,
                    saturation = saturation,
                    brightness = value, // Pass current brightness
                    alpha = alpha,      // Pass current opacity
                    onHueSaturationChange = { h, s ->
                        hue = h
                        saturation = s
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Brightness slider
                SliderWithLabel(
                    label = "Brilho",
                    value = value,
                    onValueChange = { value = it }
                )

                // Opacity slider
                SliderWithLabel(
                    label = "Opacidade",
                    value = alpha,
                    onValueChange = { alpha = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    }) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

@Composable
fun HueSaturationSelector(
    hue: Float,
    saturation: Float,
    brightness: Float, // New parameter for brightness
    alpha: Float,      // New parameter for opacity
    onHueSaturationChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Track the center of the canvas
        val center = remember { mutableStateOf(Offset.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        center.value = Offset(centerX.toFloat(), centerY.toFloat())

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
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        center.value = Offset(centerX.toFloat(), centerY.toFloat())

                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY

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

            // Draw selector indicator using current hue, saturation, brightness, and opacity
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

@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Convert Color to HSV array [hue, saturation, value]
 */
private fun colorToHsv(color: Color): FloatArray {
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
private fun Color.Companion.hsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
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

@Preview(showBackground = true)
@Composable
fun PreviewColorPickerDialog() {
    ColorPickerDialog(
        currentColor = Color.Blue,
        onColorSelected = {},
        onDismiss = {}
    )
}
