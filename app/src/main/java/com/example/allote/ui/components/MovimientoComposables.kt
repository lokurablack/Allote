package com.example.allote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.allote.data.CurrencySettings
import com.example.allote.data.MovimientoContable
import com.example.allote.utils.CurrencyFormatter

@Composable
fun BalanceCard(
    title: String,
    balanceInUsd: Double,
    settings: CurrencySettings
) {
    val balanceColor = if (balanceInUsd >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f), // Hacer la tarjeta más angosta
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(balanceInUsd, settings),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun OptionsDialog(
    movimiento: MovimientoContable,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opciones") },
        text = { Text("Seleccione una opción para el movimiento '${movimiento.descripcion}'.") },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MovimientoDetailsDialog(movimiento: MovimientoContable, settings: CurrencySettings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(movimiento.descripcion) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo: ${movimiento.tipoMovimiento}")
                if (movimiento.debe > 0) {
                    Text("Monto (Debe): ${CurrencyFormatter.format(movimiento.debe, settings)}")
                }
                if (movimiento.haber > 0) {
                    Text("Monto (Haber): ${CurrencyFormatter.format(movimiento.haber, settings)}")
                }
                if (!movimiento.detallesPago.isNullOrBlank()) Text("Detalles: ${movimiento.detallesPago}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}