package com.example.allote.ui.administraciongeneral

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.allote.data.CurrencySettings
import com.example.allote.data.DocumentoMovimiento
import com.example.allote.data.MovimientoContable
import com.example.allote.ui.components.BalanceCard
import com.example.allote.ui.components.DialogState
import com.example.allote.ui.components.EmptyState
import com.example.allote.ui.components.MovimientoDetailsDialog
import com.example.allote.ui.components.OptionsDialog
import com.example.allote.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdministracionGeneralScreen(
    uiState: AdministracionGeneralUiState,
    viewModel: AdministracionGeneralViewModel,
    currencySettings: CurrencySettings,
    onNavigateToDocumentViewer: (Int) -> Unit,
    setFabAction: (() -> Unit) -> Unit
) {
    var showRevisionDialog by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    var showRangoPersonalizadoDialog by remember { mutableStateOf(false) }
    var documentosParaDialog by remember { mutableStateOf<List<DocumentoMovimiento>>(emptyList()) }
    var movimientoParaEditar by remember { mutableStateOf<MovimientoContable?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) } // Estado para el diálogo de ayuda


    LaunchedEffect(Unit) {
        setFabAction { dialogState = DialogState.Add }
    }

    LaunchedEffect(movimientoParaEditar) {
        movimientoParaEditar?.let { movimiento ->
            documentosParaDialog = viewModel.getDocumentosParaMovimiento(movimiento.id)
            dialogState = DialogState.Edit(movimiento)
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contabilidad General") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                    FiltroFechaMenu { tipo ->
                        if (tipo == TipoFiltroFecha.RANGO_PERSONALIZADO) {
                            showRangoPersonalizadoDialog = true
                        } else {
                            viewModel.cambiarTipoFiltro(tipo)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = uiState.conteoPendientes > 0) {
                NotificacionPendientes(
                    count = uiState.conteoPendientes,
                    onClick = { showRevisionDialog = true }
                )
            }

            BalanceCard(
                title = "SALDO DEL PERÍODO (${uiState.filtroActual.tipo.displayName})",
                balanceInUsd = uiState.saldoDelPeriodo,
                settings = currencySettings
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Movimiento", modifier = Modifier.weight(2f), style = MaterialTheme.typography.titleSmall)
                Text("EGR", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                Text("ING", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val movimientosAgrupados = uiState.movimientosVisibles.groupBy {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.movimiento.fecha // <-- Accedemos a la fecha a través de .movimiento
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    if (movimientosAgrupados.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Sin Movimientos",
                                subtitle = "No hay movimientos para el período seleccionado."
                            )
                        }
                    } else {
                        movimientosAgrupados.forEach { (mes, movimientosDelMes) ->
                            stickyHeader { MesHeader(mes) }
                            items(movimientosDelMes, key = { it.movimiento.id }) { itemModel ->
                                val docCount = uiState.documentosPorMovimiento[itemModel.movimiento.id] ?: 0
                                MovimientoGeneralItem(
                                    item = itemModel,
                                    docCount = docCount,
                                    settings = currencySettings,
                                    onClick = {
                                        if (docCount > 0) {
                                            onNavigateToDocumentViewer(itemModel.movimiento.id)
                                        } else {
                                            dialogState = DialogState.Details(itemModel.movimiento)
                                        }
                                    },
                                    onLongClick = { dialogState = DialogState.Options(itemModel.movimiento) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRevisionDialog) {
        RevisionPendientesDialog(
            items = uiState.movimientosPendientes, // Pasa la nueva lista
            settings = uiState.currencySettings, // Pasa la configuración de moneda
            onDismiss = { showRevisionDialog = false },
            onAprobar = viewModel::aprobarMovimiento,
            onRechazar = viewModel::rechazarMovimientoPendiente
        )
    }

    if (showRangoPersonalizadoDialog) {
        RangoPersonalizadoDialog(
            onDismiss = { showRangoPersonalizadoDialog = false },
            onConfirm = { inicio, fin ->
                viewModel.cambiarRangoPersonalizado(inicio, fin)
                showRangoPersonalizadoDialog = false
            }
        )
    }

    when (val currentDialog = dialogState) {
        is DialogState.Add -> AddGeneralMovimientoDialog(
            currencySettings = currencySettings,
            onDismiss = { dialogState = DialogState.None },
            onSave = { desc, monto, tipo, fecha, notas, docs ->
                viewModel.saveMovimiento(
                    movimientoExistente = null, // Es nuevo
                    descripcion = desc,
                    monto = monto,
                    tipo = tipo,
                    fecha = fecha,
                    notas = notas,
                    documentos = docs
                )
                dialogState = DialogState.None
            }
        )
        is DialogState.Edit -> AddGeneralMovimientoDialog(
            movimientoToEdit = currentDialog.movimiento,
            documentosExistentes = documentosParaDialog,
            currencySettings = currencySettings,
            onDismiss = {
                dialogState = DialogState.None
                movimientoParaEditar = null
            },
            onSave = { desc, monto, tipo, fecha, notas, docs ->
                viewModel.saveMovimiento(
                    movimientoExistente = currentDialog.movimiento, // Es uno existente
                    descripcion = desc,
                    monto = monto,
                    tipo = tipo,
                    fecha = fecha,
                    notas = notas,
                    documentos = docs
                )
                dialogState = DialogState.None
                movimientoParaEditar = null
            }
        )
        is DialogState.Delete -> AlertDialog(
            onDismissRequest = { dialogState = DialogState.None },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro de eliminar '${currentDialog.movimiento.descripcion}'?") },
            confirmButton = { Button(onClick = { viewModel.eliminarMovimientoGeneral(currentDialog.movimiento); dialogState = DialogState.None }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { dialogState = DialogState.None }) { Text("Cancelar") } }
        )
        is DialogState.Details -> MovimientoDetailsDialog(
            movimiento = currentDialog.movimiento,
            onDismiss = { dialogState = DialogState.None },
            settings = currencySettings
        )
        is DialogState.Options -> OptionsDialog(
            movimiento = currentDialog.movimiento,
            onDismiss = { dialogState = DialogState.None },
            onEdit = {
                movimientoParaEditar = currentDialog.movimiento
                dialogState = DialogState.None
            },
            onDelete = {
                dialogState = DialogState.Delete(currentDialog.movimiento)
            }
        )
        DialogState.None -> {}
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Contabilidad General") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sección Principal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Funciones Principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• Filtro de Fechas: Usa el menú superior para cambiar el período de tiempo (día, semana, mes, etc.). El saldo y la lista se actualizarán.")
                    Text("• Agregar Movimiento: Usa el botón flotante (+) para registrar un nuevo ingreso o egreso general.")
                    Text("• Ver/Editar/Eliminar: Haz una pulsación larga sobre un movimiento para editarlo o eliminarlo. Una pulsación corta muestra detalles o documentos adjuntos.")
                }
                HorizontalDivider()
                // Sección de Aprobaciones
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aprobación de Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cuando un cliente registra un pago, aparecerá una notificación en la parte superior. Debes pulsarla para aprobar o rechazar ese movimiento antes de que se incluya en el saldo general.")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovimientoGeneralItem(
    item: MovimientoGeneralItemModel,
    docCount: Int, // <-- AÑADIDO
    settings: CurrencySettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(item.movimiento.fecha))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(item.movimiento.descripcion, style = MaterialTheme.typography.bodyLarge)
            if (!item.clientName.isNullOrBlank()) {
                Text(
                    text = item.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // --- AÑADIDO: Muestra el indicador de documentos ---
            if (docCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Attachment, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$docCount Documento(s)", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (item.movimiento.debe > 0) CurrencyFormatter.format(item.movimiento.debe, settings) else "",
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
            text = if (item.movimiento.haber > 0) CurrencyFormatter.format(item.movimiento.haber, settings) else "",
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
private fun NotificacionPendientes(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tienes $count movimientos de clientes pendientes de revisión.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MesHeader(mes: String) {
    Text(
        text = mes,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}