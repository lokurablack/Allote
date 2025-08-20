package com.example.allote.ui.administraciongeneral

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.CurrencySettings
import com.example.allote.data.DocumentoMovimiento
import com.example.allote.data.MovimientoContable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TipoMovimientoGeneral { INGRESO, EGRESO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGeneralMovimientoDialog(
    movimientoToEdit: MovimientoContable? = null,
    documentosExistentes: List<DocumentoMovimiento> = emptyList(),
    currencySettings: CurrencySettings,
    onDismiss: () -> Unit,
    onSave: (descripcion: String, monto: Double, tipo: TipoMovimientoGeneral, fecha: Long, notas: String?, documentos: List<Uri>) -> Unit
) {
    val context = LocalContext.current

    var tipo by remember { mutableStateOf(TipoMovimientoGeneral.INGRESO) }
    var montoText by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fechaMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var notas by remember { mutableStateOf("") }
    var documentosMostrados by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(movimientoToEdit, documentosExistentes) {
        if (movimientoToEdit != null) {
            val montoEnUsd = maxOf(movimientoToEdit.debe, movimientoToEdit.haber)

            val montoParaMostrar =
                if (currencySettings.displayCurrency == "ARS" && currencySettings.exchangeRate > 0) {
                    montoEnUsd * currencySettings.exchangeRate
                } else {
                    montoEnUsd
                }

            // --- ¡ESTA ES LA CORRECCIÓN! ---
            // Se mueve la carga de documentos fuera del 'if' de la moneda.
            // Ahora se ejecutará siempre que estemos editando.
            documentosMostrados = documentosExistentes.map { Uri.parse(it.uri) }

            tipo =
                if (movimientoToEdit.debe > 0) TipoMovimientoGeneral.EGRESO else TipoMovimientoGeneral.INGRESO
            montoText = montoParaMostrar.toString()
            descripcion = movimientoToEdit.descripcion
            fechaMillis = movimientoToEdit.fecha
            notas = movimientoToEdit.detallesPago ?: ""
        }
    }

    val isFormValid = (montoText.toDoubleOrNull() ?: 0.0) > 0 && descripcion.isNotBlank()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* Ignorar */
                }
            }
            documentosMostrados = (documentosMostrados + uris).distinct()
        }

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
                            val monto = montoText.toDoubleOrNull()
                            if (monto != null && monto > 0 && descripcion.isNotBlank()) {
                                onSave(
                                    descripcion,
                                    monto,
                                    tipo,
                                    fechaMillis,
                                    notas.takeIf { it.isNotBlank() },
                                    documentosMostrados
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Por favor, completa la descripción y un monto válido.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                                    selected = tipo == TipoMovimientoGeneral.INGRESO,
                                    onClick = { tipo = TipoMovimientoGeneral.INGRESO },
                                    shape = RoundedCornerShape(
                                        topStart = 10.dp,
                                        bottomStart = 10.dp
                                    ),
                                    label = {
                                        Row {
                                            Text("💰 Ingreso")
                                        }
                                    }
                                )
                                SegmentedButton(
                                    selected = tipo == TipoMovimientoGeneral.EGRESO,
                                    onClick = { tipo = TipoMovimientoGeneral.EGRESO },
                                    shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                                    label = {
                                        Row {
                                            Text("💸 Egreso")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Información Básica Card
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
                                text = "Información Básica",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val montoError =
                                montoText.isNotBlank() && montoText.toDoubleOrNull() == null
                            val descripcionError = descripcion.isBlank()

                            OutlinedTextField(
                                value = montoText,
                                onValueChange = { montoText = it },
                                label = { Text("Monto en ${currencySettings.displayCurrency}") },
                                leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = montoError,
                                supportingText = if (montoError) {
                                    {
                                        Text(
                                            "Ingresa un monto válido",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null,
                                trailingIcon = if (montoError) {
                                    {
                                        Icon(
                                            Icons.Default.Error,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = if (montoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            )

                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción") },
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
                                    {
                                        Icon(
                                            Icons.Default.Error,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (descripcionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = if (descripcionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // Fecha y Detalles Card
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
                                text = "Fecha y Detalles",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            EditableDateSelector(
                                label = "Fecha del Movimiento",
                                fechaSeleccionada = fechaMillis,
                                onFechaSeleccionada = { newFecha -> fechaMillis = newFecha }
                            )

                            OutlinedTextField(
                                value = notas,
                                onValueChange = { notas = it },
                                label = { Text("Notas (Opcional)") },
                                leadingIcon = { Icon(Icons.Outlined.Notes, null) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
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
                                if (documentosMostrados.isNotEmpty()) {
                                    Text(
                                        text = "${documentosMostrados.size}",
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
                                visible = documentosMostrados.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    documentosMostrados.forEach { uri ->
                                        DocumentoChip(
                                            uri = uri,
                                            onRemove = {
                                                documentosMostrados = documentosMostrados - uri
                                            }
                                        )
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

@Composable
private fun DocumentoChip(uri: Uri, onRemove: () -> Unit) {
    val fileName = "Documento adjunto"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Attachment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EditableDateSelector(
    label: String,
    fechaSeleccionada: Long,
    onFechaSeleccionada: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = fechaSeleccionada }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(selectedYear, selectedMonth, selectedDayOfMonth)
            onFechaSeleccionada(cal.timeInMillis)
        }, year, month, day
    )

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fechaFormateada = dateFormat.format(Date(fechaSeleccionada))

    Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
        OutlinedTextField(
            value = fechaFormateada,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AddGeneralMovimientoDialogPreview() {
    AddGeneralMovimientoDialog(
        currencySettings = CurrencySettings(displayCurrency = "USD", exchangeRate = 1.0),
        onDismiss = {},
        onSave = { _, _, _, _, _, _ -> }
    )
}

@Preview
@Composable
fun DocumentoChipPreview() {
    DocumentoChip(uri = Uri.parse("content://com.android.providers.media.documents/document/image%3A23"), onRemove = {})
}

@Preview
@Composable
fun EditableDateSelectorPreview() {
    EditableDateSelector(
        label = "Fecha del Movimiento",
        fechaSeleccionada = System.currentTimeMillis(),
        onFechaSeleccionada = {}
    )
}