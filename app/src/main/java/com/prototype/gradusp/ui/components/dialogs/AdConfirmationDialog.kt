package com.prototype.gradusp.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.ui.components.base.DialogActions
import com.prototype.gradusp.ui.components.base.GraduspDialog

@Composable
fun AdConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmWatchAd: () -> Unit,
    onSkipAd: () -> Unit
) {
    GraduspDialog(
        title = "Suporte o Desenvolvedor",
        onDismiss = onDismiss,
        actions = {
            DialogActions(
                onCancel = onSkipAd,
                onConfirm = onConfirmWatchAd,
                cancelText = "Não agora",
                confirmText = "Assistir Anúncio"
            )
        }
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Para continuar usando o recurso de cores personalizadas, por favor, assista a um pequeno anúncio para apoiar o desenvolvedor.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Você também pode pular o anúncio e usar a cor personalizada imediatamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
