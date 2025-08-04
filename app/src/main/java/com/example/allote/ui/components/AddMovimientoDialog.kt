package com.example.allote.ui.components

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.CurrencySettings
import com.example.allote.data.DocumentoMovimiento
import com.example.allote.data.MovimientoContable
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovimientoDialog(
    movimientoToEdit: MovimientoContable? = null,
    currencySettings: CurrencySettings,
    onDismiss: () -> Unit,
    onSave: (MovimientoContable, List<DocumentoMovimiento>) -> Unit
) {
    val context = LocalContext.current
    var documentos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                Toast.makeText(context, "No se pudo obtener permiso para uno de los archivos.", Toast.LENGTH_SHORT).show()
            }
        }
        documentos = documentos + uris
    }

    var selectedMode by remember { mutableStateOf("Pago") }
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var debeHaber by remember { mutableStateOf("Haber") }
    var tipoPago by remember { mutableStateOf("Efectivo") }
    var numeroCheque by remember { mutableStateOf("") }
    var entidadEmisora by remember { mutableStateOf("") }
    var fechaCobroMillis by remember { mutableStateOf<Long?>(null) }
    var fechaMovimientoMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(movimientoToEdit) {
        if (movimientoToEdit != null) {
            descripcion = movimientoToEdit.descripcion
            fechaMovimientoMillis = movimientoToEdit.fecha
            if (movimientoToEdit.tipoMovimiento == "PAGO" || movimientoToEdit.tipoMovimiento == "COBRO") {
                selectedMode = "Pago"
                monto = movimientoToEdit.haber.takeIf { it > 0 }?.toString() ?: movimientoToEdit.debe.toString()
            } else {
                selectedMode = "Personalizado"
                monto = movimientoToEdit.debe.takeIf { it > 0 }?.toString() ?: movimientoToEdit.haber.toString()
                debeHaber = if (movimientoToEdit.debe > 0) "Debe" else "Haber"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (movimientoToEdit == null) "Agregar Movimiento" else "Editar Movimiento") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") } }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Guardar") },
                    icon = { Icon(Icons.Default.Save, null) },
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull()
                        if (montoDouble == null || montoDouble <= 0) {
                            Toast.makeText(context, "El monto debe ser un número válido", Toast.LENGTH_SHORT).show()
                            return@ExtendedFloatingActionButton
                        }

                        val finalDescripcion = if (selectedMode == "Pago") {
                            when (tipoPago) {
                                "Efectivo" -> "Pago en efectivo"
                                "Transferencia" -> "Pago por transferencia"
                                "E-Cheq" -> "Pago con E-Cheq"
                                "Cheque Común" -> "Pago con Cheque N°$numeroCheque"
                                else -> if (descripcion.isBlank()) "Otro tipo de pago" else descripcion
                            }
                        } else {
                            if (descripcion.isBlank()) {
                                Toast.makeText(context, "La descripción es requerida", Toast.LENGTH_SHORT).show()
                                return@ExtendedFloatingActionButton
                            }
                            descripcion
                        }

                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val detalles = if (selectedMode == "Pago" && (tipoPago == "Cheque Común" || tipoPago == "E-Cheq")) {
                            "Entidad: $entidadEmisora, Fecha Cobro: ${fechaCobroMillis?.let { dateFormat.format(Date(it)) } ?: "N/A"}"
                        } else { null }

                        val movimientoFinal = MovimientoContable(
                            id = movimientoToEdit?.id ?: 0,
                            clientId = movimientoToEdit?.clientId ?: 0,
                            jobId = movimientoToEdit?.jobId,
                            descripcion = finalDescripcion,
                            fecha = fechaMovimientoMillis,
                            debe = if (selectedMode == "Personalizado" && debeHaber == "Debe") montoDouble else 0.0,
                            haber = if (selectedMode == "Pago" || (selectedMode == "Personalizado" && debeHaber == "Haber")) montoDouble else 0.0,
                            tipoMovimiento = if (selectedMode == "Pago") "PAGO" else "AJUSTE",
                            detallesPago = detalles,
                            documentoUri = null
                        )

                        val documentosAGuardar = documentos.mapNotNull { uri ->
                            try {
                                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    cursor.moveToFirst()
                                    val fileName = cursor.getString(nameIndex)
                                    val mimeType = context.contentResolver.getType(uri)
                                    DocumentoMovimiento(movimientoId = 0, uri = uri.toString(), mimeType = mimeType, fileName = fileName)
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }

                        onSave(movimientoFinal, documentosAGuardar)
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

                Box {
                    OutlinedTextField(
                        value = dateFormat.format(Date(fechaMovimientoMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha del Movimiento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = fechaMovimientoMillis
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(y, m, d)
                                        fechaMovimientoMillis = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = selectedMode == "Pago", onClick = { selectedMode = "Pago" }, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) { Text("Pago/Cobro") }
                    SegmentedButton(selected = selectedMode == "Personalizado", onClick = { selectedMode = "Personalizado" }, shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)) { Text("Ajuste") }
                }

                if (selectedMode == "Pago") {
                    PagoForm(
                        currencySymbol = currencySettings.displayCurrency,
                        monto = monto, onMontoChange = { monto = it },
                        tipoPago = tipoPago, onTipoPagoChange = { tipoPago = it },
                        numeroCheque = numeroCheque, onNumeroChequeChange = { numeroCheque = it },
                        entidadEmisora = entidadEmisora, onEntidadEmisoraChange = { entidadEmisora = it },
                        fechaCobroMillis = fechaCobroMillis,
                        onFechaCobroClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d); fechaCobroMillis = cal.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        descripcion = descripcion, onDescripcionChange = { descripcion = it }
                    )
                } else {
                    PersonalizadoForm(
                        currencySymbol = currencySettings.displayCurrency,
                        descripcion = descripcion, onDescripcionChange = { descripcion = it },
                        monto = monto, onMontoChange = { monto = it },
                        debeHaber = debeHaber, onDebeHaberChange = { debeHaber = it }
                    )
                }

                OutlinedButton(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Adjuntar Documentos") }

                if (documentos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(documentos) { uri ->
                            AttachedDocumentChip(uri = uri, onRemove = { documentos = documentos - uri })
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AttachedDocumentChip(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val fileName = remember(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "Archivo"
    }

    InputChip(
        selected = false,
        onClick = {},
        label = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Default.Close, "Quitar", Modifier.size(18.dp).clickable(onClick = onRemove)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoForm(
    currencySymbol: String,
    monto: String, onMontoChange: (String) -> Unit,
    tipoPago: String, onTipoPagoChange: (String) -> Unit,
    numeroCheque: String, onNumeroChequeChange: (String) -> Unit,
    entidadEmisora: String, onEntidadEmisoraChange: (String) -> Unit,
    fechaCobroMillis: Long?, onFechaCobroClick: () -> Unit,
    descripcion: String, onDescripcionChange: (String) -> Unit
) {
    var tipoPagoExpanded by remember { mutableStateOf(false) }
    val tiposDePago = listOf("Efectivo", "Transferencia", "Cheque Común", "E-Cheq", "Otros")
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fechaCobro = fechaCobroMillis?.let { dateFormat.format(Date(it)) } ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = monto,
            onValueChange = onMontoChange,
            label = { Text("Monto (Haber) en $currencySymbol") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(expanded = tipoPagoExpanded, onExpandedChange = { tipoPagoExpanded = it }) {
            OutlinedTextField(
                value = tipoPago,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de Pago") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoPagoExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = tipoPagoExpanded, onDismissRequest = { tipoPagoExpanded = false }) {
                tiposDePago.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onTipoPagoChange(option)
                            tipoPagoExpanded = false
                        }
                    )
                }
            }
        }

        when (tipoPago) {
            "Cheque Común", "E-Cheq" -> {
                OutlinedTextField(value = numeroCheque, onValueChange = onNumeroChequeChange, label = { Text("Número de Cheque/E-Cheq") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = entidadEmisora, onValueChange = onEntidadEmisoraChange, label = { Text("Entidad Emisora") }, modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.clickable(onClick = onFechaCobroClick)) {
                    OutlinedTextField(
                        value = fechaCobro,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de Cobro") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
            "Otros" -> {
                OutlinedTextField(value = descripcion, onValueChange = onDescripcionChange, label = { Text("Descripción del Pago") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PersonalizadoForm(
    currencySymbol: String,
    descripcion: String, onDescripcionChange: (String) -> Unit,
    monto: String, onMontoChange: (String) -> Unit,
    debeHaber: String, onDebeHaberChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = descripcion, onValueChange = onDescripcionChange, label = { Text("Descripción*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = monto,
            onValueChange = onMontoChange,
            label = { Text("Monto* en $currencySymbol") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tipo de Ajuste:", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = debeHaber == "Debe", onClick = { onDebeHaberChange("Debe") })
            Text("Debe")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = debeHaber == "Haber", onClick = { onDebeHaberChange("Haber") })
            Text("Haber")
        }
    }
}