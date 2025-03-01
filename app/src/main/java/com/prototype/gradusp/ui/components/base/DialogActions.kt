package com.prototype.gradusp.ui.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard dialog actions (Cancel/Confirm)
 */
@Composable
fun DialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = "Cancelar",
    confirmText: String = "Confirmar"
) {
    Row(
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onConfirm) {
            Text(confirmText)
        }
    }
}