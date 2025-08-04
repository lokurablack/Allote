package com.example.allote.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.DateFormatter
import com.example.allote.data.Client
import com.example.allote.data.Job
import java.text.SimpleDateFormat
import java.util.*

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
    var status by remember(initialJob) { mutableStateOf(initialJob?.status ?: "Pendiente") }

    val tipoAplicacionOptions = listOf("Aplicacion liquida", "Aplicacion solida", "Aplicacion mixta", "Aplicaciones varias")
    var selectedTipoAplicacion by remember(initialJob) { mutableStateOf(initialJob?.tipoAplicacion ?: tipoAplicacionOptions[0]) }

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
                cal.set(year, month, dayOfMonth)
                realStartMillis = cal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initialJob == null) "Agregar Trabajo" else "Editar Trabajo") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Guardar") },
                    icon = { Icon(Icons.Default.Save, null) },
                    onClick = {
                        clientError = selectedClient == null
                        surfaceError = (surface.toDoubleOrNull() ?: 0.0) <= 0

                        if (isFormValid) {
                            val selectedClientVal = selectedClient!!
                            val formattedDate = initialJob?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            val finalDescription = description.trim().ifBlank { "Trabajo sin descripción" }

                            val jobToSave = (initialJob?.copy(
                                clientId = selectedClientVal.id,
                                clientName = "${selectedClientVal.name} ${selectedClientVal.lastname}",
                                description = finalDescription,
                                notes = notes.trim().ifBlank { null },
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
                                notes = notes.trim().ifBlank { null }
                            ))
                            onJobSaved(jobToSave)
                        } else {
                            Toast.makeText(context, "Por favor, corrige los errores", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = clientDropdownExpanded,
                        onExpandedChange = { clientDropdownExpanded = it },
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
                    OutlinedTextField(
                        value = surface,
                        onValueChange = { surface = it; surfaceError = (it.toDoubleOrNull() ?: 0.0) <= 0 },
                        label = { Text("Hectáreas*") },
                        isError = surfaceError,
                        leadingIcon = { Icon(Icons.Default.Map, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Estado", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                            onClick = { status = "Pendiente" },
                            selected = status == "Pendiente"
                        ) { Text("Pendiente") }
                        SegmentedButton(
                            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                            onClick = { status = "Finalizado" },
                            selected = status == "Finalizado"
                        ) { Text("Finalizado") }
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
                            tipoAplicacionOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    selectedTipoAplicacion = option
                                    tipoDropdownExpanded = false
                                })
                            }
                        }
                    }
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

                item {
                    Box(modifier = Modifier.clickable { showDatePicker() }) {
                        OutlinedTextField(
                            value = if (realStartMillis > 0) DateFormatter.formatMillis(realStartMillis) else "Seleccionar fecha (Opcional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha real de inicio") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
