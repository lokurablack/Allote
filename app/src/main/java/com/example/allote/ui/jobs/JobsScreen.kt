package com.example.allote.ui.jobs

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.allote.data.Job
import com.example.allote.ui.components.EmptyState
import com.example.allote.ui.components.JobDialogCompose
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    uiState: JobsUiState,
    onFilterChange: (client: String?, status: String?, type: String?, billing: String?) -> Unit,
    onDateChange: (from: Long?, to: Long?) -> Unit,
    onSaveJob: (Job) -> Unit,
    onUpdateJob: (Job) -> Unit,
    onDeleteJob: (Job) -> Unit,
    onJobClick: (Int) -> Unit,
    onViewRecipes: (Int) -> Unit,
) {
    val context = LocalContext.current
    var jobToEditOrAdd by remember { mutableStateOf<Pair<Boolean, Job?>>(Pair(false, null)) }
    var showJobOptionsDialog by remember { mutableStateOf<Job?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trabajos") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mostrar Filtros")
            }
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.allJobs.isEmpty()) {
                EmptyState(
                    title = "No hay trabajos",
                    subtitle = "Crea tu primer trabajo usando el botón '+' en la barra inferior.",
                    icon = Icons.Default.Work
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.filteredJobs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No se encontraron trabajos con esos filtros.", textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        if (uiState.pendingJobs.isNotEmpty()) {
                            item {
                                Text("Pendientes (${"%.2f".format(uiState.pendingHectares)} ha)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(uiState.pendingJobs, key = { it.id }) { job ->
                                JobListItem(job = job, onClick = { onJobClick(job.id) }, onOptionsClick = { showJobOptionsDialog = job })
                            }
                        }
                        if (uiState.finishedJobs.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Finalizados (${"%.2f".format(uiState.finishedHectares)} ha)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(uiState.finishedJobs, key = { it.id }) { job ->
                                JobListItem(job = job, onClick = { onJobClick(job.id) }, onOptionsClick = { showJobOptionsDialog = job })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            uiState = uiState,
            onDismiss = { showFilterDialog = false },
            onFilterChange = onFilterChange,
            onDateChange = onDateChange
        )
    }

    if (jobToEditOrAdd.first) {
        JobDialogCompose(
            clients = uiState.allClients,
            initialJob = jobToEditOrAdd.second,
            onDismiss = { jobToEditOrAdd = Pair(false, null) },
            onJobSaved = { job ->
                if (jobToEditOrAdd.second == null) { // Is a new job
                    onSaveJob(job)
                    Toast.makeText(context, "Trabajo guardado", Toast.LENGTH_SHORT).show()
                } else { // Is an existing job
                    onUpdateJob(job)
                    Toast.makeText(context, "Trabajo actualizado", Toast.LENGTH_SHORT).show()
                }
                jobToEditOrAdd = Pair(false, null)
            }
        )
    }

    if (showJobOptionsDialog != null) {
        val currentJob = showJobOptionsDialog!!
        JobOptionsDialog(
            job = currentJob,
            onDismiss = { showJobOptionsDialog = null },
            onMarkAsFinished = {
                showDatePickerToFinishJob(context, currentJob) { updatedJob -> onUpdateJob(updatedJob) }
                showJobOptionsDialog = null
            },
            onEdit = {
                jobToEditOrAdd = Pair(true, it)
                showJobOptionsDialog = null
            },
            onDelete = {
                onDeleteJob(currentJob)
                Toast.makeText(context, "Trabajo eliminado", Toast.LENGTH_SHORT).show()
                showJobOptionsDialog = null
            },
            onBilling = {
                showBillingDialog(context, currentJob) { updatedJob -> onUpdateJob(updatedJob) }
                showJobOptionsDialog = null
            },
            onRecipes = { onViewRecipes(currentJob.id); showJobOptionsDialog = null }
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Gestión de Trabajos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla muestra una lista de todos los trabajos, separados en 'Pendientes' y 'Finalizados'.")
                Text("• Filtrar: Usa el botón 'Mostrar Filtros' para buscar trabajos por cliente, estado, tipo de aplicación, facturación o rango de fechas.")
                Text("• Ver Detalles: Pulsa sobre un trabajo para ver su panel de detalles, que incluye el pronóstico del tiempo y el menú de acciones.")
                Text("• Opciones: Pulsa el ícono de tres puntos en un trabajo para acceder a acciones rápidas como marcarlo como finalizado, editarlo, eliminarlo o gestionar su facturación.", style = MaterialTheme.typography.bodySmall)
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
fun FilterDialog(
    uiState: JobsUiState,
    onDismiss: () -> Unit,
    onFilterChange: (String?, String?, String?, String?) -> Unit,
    onDateChange: (Long?, Long?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Filtros", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Cliente", listOf("") + uiState.allClients.map { "${it.name} ${it.lastname}" }, uiState.selectedClient, { onFilterChange(it, null, null, null) })
                    FilterDropdown("Estado", listOf("", "Pendiente", "Finalizado"), uiState.selectedStatus, { onFilterChange(null, it, null, null) })
                    FilterDropdown("Aplicación", listOf("", "Aplicacion liquida", "Aplicacion solida", "Aplicacion mixta", "Aplicaciones varias"), uiState.selectedType, { onFilterChange(null, null, it, null) })
                    FilterDropdown("Facturación", listOf("", "No Facturado", "Facturado", "Pagado"), uiState.selectedBilling, { onFilterChange(null, null, null, it) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateSelector("Desde", uiState.fromDate, { onDateChange(it, uiState.toDate) }, Modifier.weight(1f))
                        DateSelector("Hasta", uiState.toDate, { onDateChange(uiState.fromDate, it) }, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

private fun showDatePickerToFinishJob(context: Context, job: Job, onResult: (Job) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(context, { _, y, m, d ->
        cal.set(y, m, d, 0, 0, 0)
        onResult(job.copy(status = "Finalizado", endDate = cal.timeInMillis))
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

private fun showBillingDialog(context: Context, job: Job, onResult: (Job) -> Unit) {
    val options = listOf("No Facturado", "Facturado", "Pagado")
    androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle("Estado de facturación")
        .setItems(options.toTypedArray()) { _, idx ->
            onResult(job.copy(billingStatus = options[idx]))
        }
        .show()
}

@Composable
fun JobOptionsDialog(
    job: Job,
    onDismiss: () -> Unit,
    onMarkAsFinished: (Job) -> Unit,
    onEdit: (Job) -> Unit,
    onDelete: (Job) -> Unit,
    onBilling: (Job) -> Unit,
    onRecipes: (Job) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opciones para: ${job.clientName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Button(onClick = { onMarkAsFinished(job) }, modifier = Modifier.fillMaxWidth()) { Text("Marcar finalizado") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onEdit(job) }, modifier = Modifier.fillMaxWidth()) { Text("Editar") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onBilling(job) }, modifier = Modifier.fillMaxWidth()) { Text("Facturación") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onRecipes(job) }, modifier = Modifier.fillMaxWidth()) { Text("Recetas") }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onDelete(job) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun JobListItem(job: Job, onClick: () -> Unit, onOptionsClick: () -> Unit) {
    val defaultColor = MaterialTheme.colorScheme.outline
    val jobTypeColor = remember(job.tipoAplicacion, defaultColor) {
        when (job.tipoAplicacion?.lowercase()) {
            "aplicacion liquida" -> Color(0xFF0288D1)
            "aplicacion solida" -> Color(0xFFFBC02D)
            "aplicacion mixta" -> Color(0xFFE64A19)
            "aplicaciones varias" -> Color(0xFF388E3C)
            else -> defaultColor
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(80.dp)
                    .background(jobTypeColor, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = job.clientName, style = MaterialTheme.typography.titleMedium)
                Text(text = "${job.surface} has", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!job.description.isNullOrBlank()) {
                    Text(text = job.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            IconButton(onClick = onOptionsClick, modifier = Modifier.padding(end = 4.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "Todos" else selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option.isEmpty()) "Todos" else option, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun DateSelector(label: String, value: Long?, onDateSelected: (Long?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }
    val displayText = value?.let { dateFormat.format(Date(it)) } ?: ""

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (value != null) {
                    IconButton(onClick = { onDateSelected(null) }) { Icon(Icons.Default.Clear, contentDescription = "Limpiar fecha") }
                } else {
                    Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val cal = Calendar.getInstance()
                    value?.let { cal.timeInMillis = it }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
                            onDateSelected(cal.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )
    }
}


