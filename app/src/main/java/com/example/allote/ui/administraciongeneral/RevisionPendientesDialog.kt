package com.example.allote.ui.administraciongeneral

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.allote.data.CurrencySettings
import com.example.allote.data.MovimientoContable
import com.example.allote.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RevisionPendientesDialog(
    // --- CAMBIO: Ahora recibe el nuevo modelo y la configuración de moneda ---
    items: List<MovimientoPendienteItemModel>,
    settings: CurrencySettings,
    onDismiss: () -> Unit,
    onAprobar: (MovimientoContable) -> Unit,
    onRechazar: (MovimientoContable) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                Text(
                    text = "Movimientos Pendientes de Revisión",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay movimientos pendientes.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items, key = { it.movimiento.id }) { item ->
                            MovimientoPendienteItem(
                                item = item,
                                settings = settings,
                                onAprobar = { onAprobar(item.movimiento) }, // Pasamos solo el movimiento
                                onRechazar = { onRechazar(item.movimiento) }
                            )
                            HorizontalDivider(
                                Modifier,
                                DividerDefaults.Thickness,
                                DividerDefaults.color
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(8.dp)
                ) {
                    Text("CERRAR")
                }
            }
        }
    }
}

@Composable
private fun MovimientoPendienteItem(
    item: MovimientoPendienteItemModel,
    settings: CurrencySettings,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val monto = item.movimiento.haber - item.movimiento.debe
    val montoColor = if (monto >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.movimiento.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            // --- AÑADIDO: Muestra el nombre del cliente ---
            Text(item.clientName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(dateFormat.format(Date(item.movimiento.fecha)), style = MaterialTheme.typography.bodySmall)
        }
        // --- AÑADIDO: Muestra el monto ---
        Text(
            text = CurrencyFormatter.format(monto, settings),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = montoColor,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Row {
            IconButton(onClick = onAprobar) {
                Icon(Icons.Default.Check, contentDescription = "Aprobar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRechazar) {
                Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}