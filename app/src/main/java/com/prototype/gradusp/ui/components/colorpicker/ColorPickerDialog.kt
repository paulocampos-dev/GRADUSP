package com.prototype.gradusp.ui.components.colorpicker

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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

    // Calculate the selected color based on current HSV values
    val selectedColor by remember(hue, saturation, value) {
        derivedStateOf { Color.hsv(hue, saturation, value) }
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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

                // Optimized Hue/Saturation selector
                OptimizedColorWheel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onColorSelected = { h, s ->
                        hue = h
                        saturation = s
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Brightness/Value slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Brilho: ${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onColorSelected(selectedColor) }) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

// Helper function to handle color selection from a touch point
private fun handleColorSelection(
    offset: Offset,
    centerPoint: Offset,
    radius: Float,
    onColorSelected: (Float, Float) -> Unit
) {
    // Calculate distance from center
    val dx = offset.x - centerPoint.x
    val dy = offset.y - centerPoint.y
    val distance = hypot(dx, dy)

    // Calculate angle and convert to hue
    val angle = atan2(dy, dx)
    val newHue = (Math.toDegrees(angle.toDouble()).toFloat() + 360) % 360

    // Calculate saturation based on distance from center
    val newSaturation = (distance / radius).coerceIn(0f, 1f)

    onColorSelected(newHue, newSaturation)
}

@Composable
fun OptimizedColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorSelected: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Center point tracking
    var centerPoint by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        handleColorSelection(offset, centerPoint, radius, onColorSelected)
                    }
                }
                .pointerInput(Unit) {
                    // Add drag gesture support
                    var dragStarted = false
                   detectDragGestures(
                        onDragStart = {
                            dragStarted = true
                        },
                        onDragEnd = {
                            dragStarted = false
                        },
                        onDragCancel = {
                            dragStarted = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (dragStarted) {
                                handleColorSelection(change.position, centerPoint, radius, onColorSelected)
                            }
                        }
                    )
                }
        ) {
            // Store center point and radius for hit testing
            centerPoint = center
            radius = size.minDimension / 2

            // Draw color wheel using fewer segments for better performance
            val segments = 60 // Reduce the number of segments for better performance
            val segmentAngle = 360f / segments
            val segmentSaturationSteps = 8 // Reduced saturation steps

            // Draw the color wheel
            for (i in 0 until segments) {
                val startAngle = i * segmentAngle
                val sweepAngle = segmentAngle

                // Draw saturation bands from outer to inner
                for (j in 0 until segmentSaturationSteps) {
                    val outerRadius = radius * (segmentSaturationSteps - j) / segmentSaturationSteps
                    val innerRadius = radius * (segmentSaturationSteps - j - 1) / segmentSaturationSteps

                    // Calculate the hue for this segment
                    val segmentHue = (startAngle + sweepAngle / 2) % 360

                    // Calculate saturation for this band
                    val segmentSaturation = (segmentSaturationSteps - j) / segmentSaturationSteps.toFloat()

                    // Draw an arc for this segment and saturation band
                    drawArc(
                        color = Color.hsv(segmentHue, segmentSaturation, value),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(
                            center.x - outerRadius,
                            center.y - outerRadius
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            outerRadius * 2,
                            outerRadius * 2
                        )
                    )
                }
            }

            // Draw center white/gray circle
            drawCircle(
                color = Color.hsv(0f, 0f, value),
                radius = radius * 0.1f,
                center = center
            )

            // Draw border around the color wheel
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = 2f)
            )

            // Draw selector indicator
            val hueRadians = Math.toRadians(hue.toDouble())
            val selectorX = center.x + (cos(hueRadians) * radius * saturation).toFloat()
            val selectorY = center.y + (sin(hueRadians) * radius * saturation).toFloat()

            // Draw outline
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(selectorX, selectorY)
            )

            // Draw color indicator
            drawCircle(
                color = Color.hsv(hue, saturation, value),
                radius = 8f,
                center = Offset(selectorX, selectorY)
            )

            // Draw border
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 10f,
                style = Stroke(width = 2f),
                center = Offset(selectorX, selectorY)
            )
        }
    }
}

// Helper extension to convert Color to HSV array [hue, saturation, value]
fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    // Calculate value (brightness)
    val v = max

    // Calculate saturation
    val s = if (max != 0f) delta / max else 0f

    // Calculate hue
    var h = when {
        delta == 0f -> 0f // achromatic case
        r == max -> (g - b) / delta // between yellow & magenta
        g == max -> 2 + (b - r) / delta // between cyan & yellow
        else -> 4 + (r - g) / delta // between magenta & cyan
    } * 60f

    // Normalize hue to 0-360
    if (h < 0) h += 360f

    return floatArrayOf(h, s, v)
}

// Create a Color from HSV values
fun Color.Companion.hsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val h = hue / 60f
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