package com.prototype.gradusp.ui.components.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prototype.gradusp.utils.ColorUtils

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert current color to HSV components
    val hsv = ColorUtils.colorToHsv(currentColor)
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

                // Hue/Saturation selector
                HueSaturationSelector(
                    hue = hue,
                    saturation = saturation,
                    brightness = value,
                    alpha = alpha,
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
                ColorPickerActionButtons(
                    onCancel = onDismiss,
                    onApply = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    }
                )
            }
        }
    }
}