package com.example.allote.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.allote.DateFormatter
import com.example.allote.data.Client
import com.example.allote.data.Job
import java.text.SimpleDateFormat
import java.util.*

private const val STATUS_PENDIENTE = "Pendiente"
private const val STATUS_FINALIZADO = "Finalizado"
private val TIPO_APLICACION_OPTIONS = listOf("Aplicacion liquida", "Aplicacion solida", "Aplicacion mixta", "Aplicaciones varias")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDialogCompose(
    clients: List<Client>,
    initialJob: Job? = null,
    onDismiss: () -> Unit,
    onJobSaved: (Job) -> Unit
) {
    val context = LocalContext.current

    var selectedClient by remember(initialJob, clients) {
        mutableStateOf(clients.find { it.id == initialJob?.clientId } ?: if (clients.size == 1) clients.first() else null)
    }
    var description by remember(initialJob) { mutableStateOf(initialJob?.description ?: "") }
    var surface by remember(initialJob) { mutableStateOf(initialJob?.surface?.toString() ?: "") }
    var notes by remember(initialJob) { mutableStateOf(initialJob?.notes ?: "") }
    var status by remember(initialJob) { mutableStateOf(initialJob?.status ?: STATUS_PENDIENTE) }
    var selectedTipoAplicacion by remember(initialJob) { mutableStateOf(initialJob?.tipoAplicacion ?: TIPO_APLICACION_OPTIONS[0]) }
    var realStartMillis by remember(initialJob) { mutableStateOf(initialJob?.startDate ?: 0L) }

    var clientError by remember { mutableStateOf(false) }
    var surfaceError by remember { mutableStateOf(false) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var tipoDropdownExpanded by remember { mutableStateOf(false) }

    val isFormValid = selectedClient != null && (surface.toDoubleOrNull() ?: 0.0) > 0

    fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (realStartMillis > 0L) {
            cal.timeInMillis = realStartMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                realStartMillis = calendar.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun validateAndSaveJob() {
        clientError = selectedClient == null
        surfaceError = (surface.toDoubleOrNull() ?: 0.0) <= 0

        if (!isFormValid) {
            Toast.makeText(context, "Por favor, corrige los errores", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedClientVal = selectedClient!!
        val formattedDate = initialJob?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val finalDescription = description.trim().ifBlank { "Trabajo sin descripción" }
        val finalNotes = notes.trim().ifBlank { null }

        val jobToSave = initialJob?.copy(
            clientId = selectedClientVal.id,
            clientName = "${selectedClientVal.name} ${selectedClientVal.lastname}",
            description = finalDescription,
            notes = finalNotes,
            status = status,
            startDate = realStartMillis,
            surface = surface.toDouble(),
            tipoAplicacion = selectedTipoAplicacion
        ) ?: Job(
            clientId = selectedClientVal.id,
            clientName = "${selectedClientVal.name} ${selectedClientVal.lastname}",
            description = finalDescription,
            date = formattedDate,
            status = status,
            startDate = realStartMillis,
            surface = surface.toDouble(),
            tipoAplicacion = selectedTipoAplicacion,
            billingStatus = "No Facturado",
            notes = finalNotes
        )
        onJobSaved(jobToSave)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (initialJob == null) "Agregar Trabajo" else "Editar Trabajo",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Cliente y Descripción",
                            icon = Icons.Default.Person,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = clientDropdownExpanded,
                            onExpandedChange = { if (clients.size > 1) clientDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedClient?.let { "${it.name} ${it.lastname}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cliente*") },
                                isError = clientError,
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .clickable(enabled = clients.size > 1) { clientDropdownExpanded = true }
                            )
                            ExposedDropdownMenu(
                                expanded = clientDropdownExpanded,
                                onDismissRequest = { clientDropdownExpanded = false }
                            ) {
                                clients.forEach { client ->
                                    DropdownMenuItem(
                                        text = { Text("${client.name} ${client.lastname}") },
                                        onClick = {
                                            selectedClient = client
                                            clientError = false
                                            clientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción del trabajo") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Detalles",
                            icon = Icons.Default.Tune,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = surface,
                            onValueChange = {
                                surface = it
                                surfaceError = (it.toDoubleOrNull() ?: 0.0) <= 0
                            },
                            label = { Text("Hectáreas*") },
                            isError = surfaceError,
                            leadingIcon = { Icon(Icons.Default.Map, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            "Estado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                onClick = { status = STATUS_PENDIENTE },
                                selected = status == STATUS_PENDIENTE
                            ) { Text(STATUS_PENDIENTE) }
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                onClick = { status = STATUS_FINALIZADO },
                                selected = status == STATUS_FINALIZADO
                            ) { Text(STATUS_FINALIZADO) }
                        }
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = tipoDropdownExpanded,
                            onExpandedChange = { tipoDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedTipoAplicacion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Aplicación") },
                                leadingIcon = { Icon(Icons.Default.Grass, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .clickable { tipoDropdownExpanded = true }
                            )
                            ExposedDropdownMenu(
                                expanded = tipoDropdownExpanded,
                                onDismissRequest = { tipoDropdownExpanded = false }
                            ) {
                                TIPO_APLICACION_OPTIONS.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        selectedTipoAplicacion = option
                                        tipoDropdownExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Programación",
                            icon = Icons.Default.DateRange,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = if (realStartMillis > 0) DateFormatter.formatMillis(realStartMillis) else "Seleccionar fecha (Opcional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha real de inicio") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker() },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notas (Opcional)") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text("Guardar") },
                        icon = { Icon(Icons.Default.Save, null) },
                        onClick = { validateAndSaveJob() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
