package com.example.allote.ui.administraciongeneral

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

            val montoParaMostrar = if (currencySettings.displayCurrency == "ARS" && currencySettings.exchangeRate > 0) {
                montoEnUsd * currencySettings.exchangeRate
            } else {
                montoEnUsd
            }

            // --- ¡ESTA ES LA CORRECCIÓN! ---
            // Se mueve la carga de documentos fuera del 'if' de la moneda.
            // Ahora se ejecutará siempre que estemos editando.
            documentosMostrados = documentosExistentes.map { Uri.parse(it.uri) }

            tipo = if (movimientoToEdit.debe > 0) TipoMovimientoGeneral.EGRESO else TipoMovimientoGeneral.INGRESO
            montoText = montoParaMostrar.toString()
            descripcion = movimientoToEdit.descripcion
            fechaMillis = movimientoToEdit.fecha
            notas = movimientoToEdit.detallesPago ?: ""
        }
    }

    val isFormValid = (montoText.toDoubleOrNull() ?: 0.0) > 0 && descripcion.isNotBlank()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { /* Ignorar */ }
        }
        documentosMostrados = (documentosMostrados + uris).distinct()
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
                            Toast.makeText(context, "Por favor, completa la descripción y un monto válido.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    expanded = isFormValid
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
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tipo == TipoMovimientoGeneral.INGRESO,
                        onClick = { tipo = TipoMovimientoGeneral.INGRESO },
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        label = { Text("Ingreso") }
                    )
                    SegmentedButton(
                        selected = tipo == TipoMovimientoGeneral.EGRESO,
                        onClick = { tipo = TipoMovimientoGeneral.EGRESO },
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                        label = { Text("Egreso") }
                    )
                }

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto en ${currencySettings.displayCurrency}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Detalle / Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                EditableDateSelector(
                    label = "Fecha del Movimiento",
                    fechaSeleccionada = fechaMillis,
                    onFechaSeleccionada = { fechaMillis = it }
                )

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Adjuntar Documentos")
                }

                if (documentosMostrados.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DocumentoChip(uri: Uri, onRemove: () -> Unit) {
    val fileName = "Documento adjunto"

    InputChip(
        selected = false,
        onClick = { /* Opcional: abrir el documento */ },
        label = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        avatar = { Icon(Icons.Default.Attachment, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Quitar")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
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
            modifier = Modifier.fillMaxWidth(),
            enabled = false // Se deshabilita para que no capture clics y los maneje el Box
        )
    }
}