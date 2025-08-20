package com.example.allote.ui.components

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Payment
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

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Attachment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
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
    descripcion: String, onDescripcionChange: (String) -> Unit,
    notas: String, onNotasChange: (String) -> Unit
) {
    var tipoPagoExpanded by remember { mutableStateOf(false) }
    val tiposDePago = listOf("Efectivo", "Transferencia", "Cheque Común", "E-Cheq", "Otros")
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fechaCobro = fechaCobroMillis?.let { dateFormat.format(Date(it)) } ?: ""

    // Información de Pago Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Información de Pago",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val montoError = monto.isNotBlank() && monto.toDoubleOrNull() == null

            OutlinedTextField(
                value = monto,
                onValueChange = onMontoChange,
                label = { Text("Monto (Haber) en $currencySymbol") },
                leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = montoError,
                supportingText = if (montoError) {
                    { Text("Ingresa un monto válido", color = MaterialTheme.colorScheme.error) }
                } else null,
                trailingIcon = if (montoError) {
                    { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    focusedLabelColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            )

            ExposedDropdownMenuBox(expanded = tipoPagoExpanded, onExpandedChange = { tipoPagoExpanded = it }) {
                OutlinedTextField(
                    value = tipoPago,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Pago") },
                    leadingIcon = { Icon(Icons.Outlined.Payment, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoPagoExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
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

            AnimatedVisibility(
                visible = tipoPago == "Cheque Común" || tipoPago == "E-Cheq",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = numeroCheque,
                        onValueChange = onNumeroChequeChange,
                        label = { Text("Número de Cheque/E-Cheq") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entidadEmisora,
                        onValueChange = onEntidadEmisoraChange,
                        label = { Text("Entidad Emisora") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.clickable(onClick = onFechaCobroClick)) {
                        OutlinedTextField(
                            value = fechaCobro,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha de Cobro") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
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
            }

            AnimatedVisibility(
                visible = tipoPago == "Otros",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = onDescripcionChange,
                    label = { Text("Descripción del Pago") },
                    leadingIcon = { Icon(Icons.Outlined.Description, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = notas,
                onValueChange = onNotasChange,
                label = { Text("Notas (Opcional)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ajuste Personalizado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val descripcionError = descripcion.isBlank()
            val montoError = monto.isNotBlank() && monto.toDoubleOrNull() == null

            OutlinedTextField(
                value = descripcion,
                onValueChange = onDescripcionChange,
                label = { Text("Descripción*") },
                leadingIcon = { Icon(Icons.Outlined.Description, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = descripcionError,
                supportingText = if (descripcionError) {
                    {
                        Text(
                            "La descripción es obligatoria",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null,
                trailingIcon = if (descripcionError) {
                    { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (descripcionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    focusedLabelColor = if (descripcionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = monto,
                onValueChange = onMontoChange,
                label = { Text("Monto* en $currencySymbol") },
                leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = montoError,
                supportingText = if (montoError) {
                    { Text("Ingresa un monto válido", color = MaterialTheme.colorScheme.error) }
                } else null,
                trailingIcon = if (montoError) {
                    { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    focusedLabelColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Tipo de Ajuste:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = debeHaber == "Debe",
                            onClick = { onDebeHaberChange("Debe") },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text("💸 Debe", modifier = Modifier.padding(end = 16.dp))
                        RadioButton(
                            selected = debeHaber == "Haber",
                            onClick = { onDebeHaberChange("Haber") },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text("💰 Haber")
                    }
                }
            }
        }
    }
}

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
    var fechaMovimientoMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notas by remember { mutableStateOf("") } // State for optional notes

    LaunchedEffect(movimientoToEdit) {
        if (movimientoToEdit != null) {
            descripcion = movimientoToEdit.descripcion
            fechaMovimientoMillis = movimientoToEdit.fecha
            if (movimientoToEdit.tipoMovimiento == "PAGO" || movimientoToEdit.tipoMovimiento == "COBRO") {
                selectedMode = "Pago"
                monto = movimientoToEdit.haber.takeIf { it > 0 }?.toString() ?: movimientoToEdit.debe.toString()
                notas = movimientoToEdit.detallesPago ?: "" // Populate notes
            } else {
                selectedMode = "Personalizado"
                monto = movimientoToEdit.debe.takeIf { it > 0 }?.toString() ?: movimientoToEdit.haber.toString()
                debeHaber = if (movimientoToEdit.debe > 0) "Debe" else "Haber"
            }
        }
    }

    val isFormValid = monto.toDoubleOrNull() != null && monto.toDoubleOrNull()!! > 0 &&
            (selectedMode == "Pago" || descripcion.isNotBlank())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (movimientoToEdit == null) "Agregar Movimiento" else "Editar Movimiento",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Cerrar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    val fabBackgroundColor by animateColorAsState(
                        targetValue = if (isFormValid)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )

                    ExtendedFloatingActionButton(
                        text = {
                            Text(
                                "Guardar",
                                color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                if (isFormValid) Icons.Default.Check else Icons.Default.Save,
                                null,
                                tint = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
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
                                    else -> descripcion.ifBlank { "Otro tipo de pago" }
                                }
                            } else {
                                if (descripcion.isBlank()) {
                                    Toast.makeText(context, "La descripción es requerida", Toast.LENGTH_SHORT).show()
                                    return@ExtendedFloatingActionButton
                                }
                                descripcion
                            }

                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val detalles = if (selectedMode == "Pago") {
                                val checkDetails = if (tipoPago == "Cheque Común" || tipoPago == "E-Cheq") {
                                    "Entidad: $entidadEmisora, Fecha Cobro: ${fechaCobroMillis?.let { dateFormat.format(Date(it)) } ?: "N/A"}"
                                } else {
                                    ""
                                }
                                // Combine check details and notes, separated by a period.
                                listOf(checkDetails, notas).filter { it.isNotBlank() }.joinToString(". ")
                            } else {
                                null
                            }


                            val movimientoFinal = MovimientoContable(
                                id = movimientoToEdit?.id ?: 0,
                                clientId = movimientoToEdit?.clientId ?: 0,
                                jobId = movimientoToEdit?.jobId,
                                descripcion = finalDescripcion,
                                fecha = fechaMovimientoMillis,
                                debe = if (selectedMode == "Personalizado" && debeHaber == "Debe") montoDouble else 0.0,
                                haber = if (selectedMode == "Pago" || (selectedMode == "Personalizado" && debeHaber == "Haber")) montoDouble else 0.0,
                                tipoMovimiento = if (selectedMode == "Pago") "PAGO" else "AJUSTE",
                                detallesPago = detalles?.takeIf { it.isNotBlank() }, // Save combined details
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
                        },
                        expanded = isFormValid,
                        containerColor = fabBackgroundColor
                    )
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fecha Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Fecha del Movimiento",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                            Box(modifier = Modifier.clickable {
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
                            }) {
                                OutlinedTextField(
                                    value = dateFormat.format(Date(fechaMovimientoMillis)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Fecha del Movimiento") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // Tipo de Movimiento Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Tipo de Movimiento",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = selectedMode == "Pago",
                                    onClick = { selectedMode = "Pago" },
                                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                                ) {
                                    Row {
                                        Text("💳 Pago/Cobro")
                                    }
                                }
                                SegmentedButton(
                                    selected = selectedMode == "Personalizado",
                                    onClick = { selectedMode = "Personalizado" },
                                    shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                                ) {
                                    Row {
                                        Text("⚙️ Ajuste")
                                    }
                                }
                            }
                        }
                    }

                    // Formulario dinámico
                    if (selectedMode == "Pago") {
                        PagoForm(
                            currencySymbol = currencySettings.displayCurrency,
                            monto = monto, onMontoChange = { newMonto -> monto = newMonto },
                            tipoPago = tipoPago, onTipoPagoChange = { newTipoPago -> tipoPago = newTipoPago },
                            numeroCheque = numeroCheque, onNumeroChequeChange = { newNumeroCheque -> numeroCheque = newNumeroCheque },
                            entidadEmisora = entidadEmisora, onEntidadEmisoraChange = { newEntidadEmisora -> entidadEmisora = newEntidadEmisora },
                            fechaCobroMillis = fechaCobroMillis,
                            onFechaCobroClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d); fechaCobroMillis = cal.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            descripcion = descripcion, onDescripcionChange = { newDescripcion -> descripcion = newDescripcion },
                            notas = notas, onNotasChange = { newNotas -> notas = newNotas }
                        )
                    } else {
                        PersonalizadoForm(
                            currencySymbol = currencySettings.displayCurrency,
                            descripcion = descripcion, onDescripcionChange = { newDescripcion -> descripcion = newDescripcion },
                            monto = monto, onMontoChange = { newMonto -> monto = newMonto },
                            debeHaber = debeHaber, onDebeHaberChange = { newDebeHaber -> debeHaber = newDebeHaber }
                        )
                    }

                    // Documentos Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row {
                                Text(
                                    text = "Documentos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (documentos.isNotEmpty()) {
                                    Text(
                                        text = "${documentos.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { launcher.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.AttachFile, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adjuntar Documentos")
                            }

                            AnimatedVisibility(
                                visible = documentos.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(documentos) { uri ->
                                        AttachedDocumentChip(uri = uri, onRemove = { documentos = documentos - uri })
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}
