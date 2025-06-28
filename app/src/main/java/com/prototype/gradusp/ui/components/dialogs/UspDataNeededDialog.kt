package com.prototype.gradusp.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun UspDataNeededDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dados da USP necessários") },
        text = { Text("Para adicionar uma aula, você precisa primeiro selecionar suas unidades e buscar os dados da USP nas configurações.") },
        confirmButton = {
            Button(onClick = onNavigateToSettings) {
                Text("Ir para Configurações")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
