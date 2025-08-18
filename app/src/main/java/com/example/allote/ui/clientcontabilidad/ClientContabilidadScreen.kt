package com.example.allote.ui.clientcontabilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.allote.data.CurrencySettings
import com.example.allote.data.DocumentoMovimiento
import com.example.allote.data.MovimientoContable
import com.example.allote.ui.clientjobs.DateSelector
import com.example.allote.ui.components.AddMovimientoDialog
import com.example.allote.ui.components.EmptyState
import com.example.allote.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DialogState {
    object None : DialogState()
    object Add : DialogState()
    data class Options(val movimiento: MovimientoContable) : DialogState()
    data class Details(val movimiento: MovimientoContable) : DialogState()
    data class Edit(val movimiento: MovimientoContable) : DialogState()
    data class Delete(val movimiento: MovimientoContable) : DialogState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientContabilidadScreen(
    uiState: ClientContabilidadUiState,
    onConfirmarEliminacionGeneral: (MovimientoContable) -> Unit,
    onCancelarEliminacion: () -> Unit,
    onAddMovimiento: (MovimientoContable, List<DocumentoMovimiento>) -> Unit,
    onUpdateMovimiento: (MovimientoContable, List<DocumentoMovimiento>) -> Unit,
    onDeleteMovimiento: (MovimientoContable) -> Unit,
    onDateChange: (Long?, Long?) -> Unit,
    setFabAction: (() -> Unit) -> Unit,
    onNavigateToDocumentViewer: (Int) -> Unit
) {
    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el diálogo de ayuda
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        setFabAction { dialogState = DialogState.Add }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Contabilidad de ${uiState.clientFullName}") },
            actions = {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                }
            }
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                BalanceCard(
                    balanceInUsd = uiState.saldo,
                    totalDebe = uiState.totalDebe,
                    totalHaber = uiState.totalHaber,
                    settings = uiState.currencySettings
                )
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, "Filtros", Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mostrar Filtros")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Movimiento", modifier = Modifier.weight(2f), style = MaterialTheme.typography.titleSmall)
                    Text("Debe", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                    Text("Haber", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                }
                HorizontalDivider()
            }

            AnimatedVisibility(visible = uiState.movimientos.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(uiState.movimientos, key = { it.id }) { movimiento ->
                        val docCount = uiState.documentosPorMovimiento[movimiento.id] ?: 0

                        MovimientoItem(
                            movimiento = movimiento,
                            docCount = docCount,
                            settings = uiState.currencySettings,
                            onClick = {
                                if (docCount > 0) {
                                    onNavigateToDocumentViewer(movimiento.id)
                                } else {
                                    dialogState = DialogState.Details(movimiento)
                                }
                            },
                            onLongClick = { dialogState = DialogState.Options(movimiento) }
                        )
                        HorizontalDivider()
                    }
                }
            }
            AnimatedVisibility(visible = uiState.movimientos.isEmpty()) {
                EmptyState("Sin Movimientos", "Añade un nuevo movimiento o ajusta los filtros.", Icons.AutoMirrored.Filled.ReceiptLong)
            }
        }
    }

    LaunchedEffect(uiState.movimientos.size) {
        if (uiState.movimientos.isNotEmpty()) {
            listState.animateScrollToItem(index = uiState.movimientos.size - 1)
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            fromDate = uiState.fromDate, toDate = uiState.toDate,
            onDateChange = onDateChange, onDismiss = { showFilterDialog = false }
        )
    }

    when (val currentDialog = dialogState) {
        is DialogState.Add -> AddMovimientoDialog(
            currencySettings = uiState.currencySettings,
            onDismiss = { dialogState = DialogState.None },
            onSave = { movimiento, documentos -> onAddMovimiento(movimiento, documentos); dialogState = DialogState.None }
        )

        is DialogState.Delete -> AlertDialog(
            onDismissRequest = { dialogState = DialogState.None },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro de eliminar '${currentDialog.movimiento.descripcion}'?") },
            confirmButton = { Button(onClick = {
                onDeleteMovimiento(currentDialog.movimiento)
                dialogState = DialogState.None
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { dialogState = DialogState.None }) { Text("Cancelar") } }
        )

        is DialogState.Edit -> AddMovimientoDialog(
            movimientoToEdit = currentDialog.movimiento,
            currencySettings = uiState.currencySettings,
            onDismiss = { dialogState = DialogState.None },
            onSave = { movimiento, documentos -> onUpdateMovimiento(movimiento, documentos); dialogState = DialogState.None }
        )
        is DialogState.Details -> MovimientoDetailsDialog(movimiento = currentDialog.movimiento, onDismiss = { dialogState = DialogState.None }, settings = uiState.currencySettings)
        is DialogState.Options -> {
            val movimiento = currentDialog.movimiento
            OptionsDialog(
                movimiento = movimiento,
                onDismiss = { dialogState = DialogState.None },
                onEdit = { dialogState = DialogState.Edit(movimiento) },
                onDelete = { dialogState = DialogState.Delete(movimiento) },
                onOpenDocument = {
                    dialogState = DialogState.None
                }
            )
        }
        DialogState.None -> {}
    }

    uiState.movimientoAEliminar?.let { movimiento ->
        AlertDialog(
            onDismissRequest = onCancelarEliminacion,
            title = { Text("Atención") },
            text = {
                Text("Este movimiento ya fue aprobado en la contabilidad general. ¿Desea eliminarlo de ambas contabilidades? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmarEliminacionGeneral(movimiento) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar de Ambas")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarEliminacion) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Mi Contabilidad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Qué significa el saldo?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• Saldo Deudor: Es el monto que debes a la empresa.")
                    Text("• Saldo Acreedor: Es el monto que tienes a tu favor (crédito).")
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registro de Pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Puedes registrar los pagos que realizas usando el botón flotante (+).")
                    Text("Importante: Tu pago aparecerá como 'Pendiente' hasta que sea confirmado por el administrador. Una vez confirmado, se reflejará en tu saldo.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
private fun FilterDialog(
    fromDate: Long?,
    toDate: Long?,
    onDateChange: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Filtrar por Fecha", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateSelector("Desde", fromDate, { onDateChange(it, toDate) }, Modifier.weight(1f))
                    DateSelector("Hasta", toDate, { onDateChange(fromDate, it) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    balanceInUsd: Double,
    totalDebe: Double,
    totalHaber: Double,
    settings: CurrencySettings
) {
    val (title, balanceColor) = when {
        totalHaber > totalDebe -> "SALDO ACREEDOR" to Color(0xFF2E7D32) // Verde
        totalDebe > totalHaber -> "SALDO DEUDOR" to MaterialTheme.colorScheme.error // Rojo
        else -> "SALDO" to MaterialTheme.colorScheme.onSurface // Color neutro
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(kotlin.math.abs(balanceInUsd), settings),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovimientoItem(
    movimiento: MovimientoContable,
    settings: CurrencySettings,
    docCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(movimiento.fecha))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(movimiento.descripcion, style = MaterialTheme.typography.bodyLarge)
            if(docCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Attachment, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$docCount Documento(s)", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!movimiento.detallesPago.isNullOrBlank()) {
                Text(
                    text = movimiento.detallesPago,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (movimiento.debe > 0) CurrencyFormatter.format(movimiento.debe, settings) else "",
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        Text(
            text = if (movimiento.haber > 0) CurrencyFormatter.format(movimiento.haber, settings) else "",
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            color = Color(0xFF2E7D32),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun OptionsDialog(
    movimiento: MovimientoContable,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenDocument: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opciones") },
        text = {
            Column {
                Text("Seleccione una opción para este movimiento.")
                if (movimiento.documentoUri != null) {
                    TextButton(onClick = onOpenDocument) {
                        Text("Abrir Documento Adjunto")
                    }
                }
            }
        },
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
                if (movimiento.documentoUri != null) Text("Documento asociado: Sí")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
