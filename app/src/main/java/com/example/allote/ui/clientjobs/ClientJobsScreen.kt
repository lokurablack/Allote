package com.example.allote.ui.clientjobs

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.allote.data.Job
import com.example.allote.ui.components.JobDialogCompose
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientJobsScreen(
    uiState: ClientJobsUiState,
    onStatusChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onBillingChange: (String) -> Unit,
    onDateChange: (Long?, Long?) -> Unit,
    onUpdateJob: (Job) -> Unit,
    onDeleteJob: (Job) -> Unit,
    onSaveJob: (Job) -> Unit,
    onJobClick: (Int) -> Unit,
    onViewRecipes: (Int) -> Unit,
    onNavigateToContabilidad: (Int) -> Unit,
    setFabAction: (() -> Unit) -> Unit
) {
    val context = LocalContext.current

    var jobForActions by remember { mutableStateOf<Job?>(null) }
    var jobToEditOrAdd by remember { mutableStateOf<Pair<Boolean, Job?>>(Pair(false, null)) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        setFabAction {
            jobToEditOrAdd = Pair(true, null)
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trabajos de ${uiState.clientFullName}") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                    IconButton(onClick = { uiState.client?.id?.let(onNavigateToContabilidad) }) {
                        Icon(Icons.Default.AccountBalance, "Ver Contabilidad")
                    }
                }
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mostrar Filtros")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.filteredJobs.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No se encontraron trabajos") } }
                    } else {
                        items(uiState.filteredJobs, key = { it.id }) { job ->
                            ClientJobListItem(
                                job = job,
                                onClick = { onJobClick(job.id) },
                                onOptionsClick = { jobForActions = job }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        ClientJobsFilterDialog(
            uiState = uiState,
            onDismiss = { showFilterDialog = false },
            onStatusChange = onStatusChange,
            onTypeChange = onTypeChange,
            onBillingChange = onBillingChange,
            onDateChange = onDateChange
        )
    }

    jobForActions?.let { job ->
        JobActionsDialog(
            job = job,
            onDismiss = { jobForActions = null },
            onEdit = {
                jobForActions = null
                jobToEditOrAdd = Pair(true, it)
            },
            onFinish = { onUpdateJob(it) },
            onUpdateBilling = { onUpdateJob(it) },
            onDelete = { onDeleteJob(it) },
            onViewRecipes = { onViewRecipes(it.id) },
            context = context
        )
    }

    if (jobToEditOrAdd.first) {
        JobDialogCompose(
            clients = listOfNotNull(uiState.client),
            initialJob = jobToEditOrAdd.second,
            onDismiss = { jobToEditOrAdd = Pair(false, null) },
            onJobSaved = { savedJob ->
                if (jobToEditOrAdd.second == null) {
                    onSaveJob(savedJob)
                    Toast.makeText(context, "Trabajo agregado", Toast.LENGTH_SHORT).show()
                } else {
                    onUpdateJob(savedJob)
                    Toast.makeText(context, "Trabajo actualizado", Toast.LENGTH_SHORT).show()
                }
                jobToEditOrAdd = Pair(false, null)
            }
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Trabajos del Cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla muestra todos los trabajos asociados a este cliente.")
                Text("• Añadir Trabajo: Usa el botón flotante (+) para crear un nuevo trabajo para este cliente.")
                Text("• Ver Detalles: Pulsa sobre un trabajo para ver su panel de detalles (clima, acciones, etc.).")
                Text("• Atajo a Contabilidad: Usa el ícono de balanza en la esquina superior derecha para acceder rápidamente a la cuenta corriente de este cliente.", style = MaterialTheme.typography.bodySmall)
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
fun ClientJobListItem(
    job: Job,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val jobTypeColor = remember(job.tipoAplicacion) {
        when (job.tipoAplicacion?.lowercase()) {
            "aplicacion liquida" -> Color(0xFF0288D1)
            "aplicacion solida" -> Color(0xFFFBC02D)
            "aplicacion mixta" -> Color(0xFFE64A19)
            "aplicaciones varias" -> Color(0xFF388E3C)
            else -> Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(6.dp).height(100.dp).background(jobTypeColor))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!job.description.isNullOrBlank()) {
                    Text(job.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text("Trabajo sin descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Superficie: ${job.surface} ha", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (job.status == "Pendiente") Icons.Default.Timelapse else Icons.Default.CheckCircle,
                        contentDescription = "Estado",
                        modifier = Modifier.size(16.dp),
                        tint = if (job.status == "Pendiente") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(job.status, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onOptionsClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
            }
        }
    }
}

@Composable
fun JobActionsDialog(
    job: Job,
    onDismiss: () -> Unit,
    onEdit: (Job) -> Unit,
    onFinish: (Job) -> Unit,
    onUpdateBilling: (Job) -> Unit,
    onDelete: (Job) -> Unit,
    onViewRecipes: (Job) -> Unit,
    context: Context
) {
    var showBillingSubDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column {
                Text(
                    text = job.description ?: "Opciones del Trabajo",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ListItem(
                        headlineContent = { Text("Editar Trabajo") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable { onEdit(job) }
                    )
                    if (job.status != "Finalizado") {
                        ListItem(
                            headlineContent = { Text("Marcar como Finalizado") },
                            leadingContent = { Icon(Icons.Default.Check, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showDatePickerToFinishJob(context, job) { updatedJob ->
                                    onFinish(updatedJob)
                                    Toast.makeText(context, "Trabajo finalizado", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Actualizar Facturación") },
                        leadingContent = { Icon(Icons.Default.RequestQuote, contentDescription = null) },
                        modifier = Modifier.clickable { showBillingSubDialog = true }
                    )
                    ListItem(
                        headlineContent = { Text("Ver Recetas") },
                        leadingContent = { Icon(Icons.Default.Science, contentDescription = null) },
                        modifier = Modifier.clickable { onViewRecipes(job); onDismiss() }
                    )
                    ListItem(
                        headlineContent = { Text("Eliminar Trabajo") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { onDelete(job); onDismiss() }
                    )
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }

    if(showBillingSubDialog){
        AlertDialog(
            onDismissRequest = { showBillingSubDialog = false },
            title = { Text("Estado de facturación") },
            text = {
                Column {
                    listOf("Facturado", "No Facturado", "Pagado").forEach { option ->
                        Text(option, modifier = Modifier.fillMaxWidth().clickable {
                            onUpdateBilling(job.copy(billingStatus = option))
                            showBillingSubDialog = false
                            onDismiss()
                        }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {}
        )
    }
}

private fun showDatePickerToFinishJob(context: Context, job: Job, onUpdate: (Job) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(context, { _, y, m, d ->
        cal.set(y, m, d)
        onUpdate(job.copy(status = "Finalizado", endDate = cal.timeInMillis))
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable
fun ClientJobsFilterDialog(
    uiState: ClientJobsUiState,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onBillingChange: (String) -> Unit,
    onDateChange: (Long?, Long?) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Filtros", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterDropdown("Estado", listOf("", "Pendiente", "Finalizado"), uiState.selectedStatus, onStatusChange)
                    FilterDropdown("Aplicación", listOf("", "Aplicacion liquida", "Aplicacion solida", "Aplicacion mixta", "Aplicaciones varias"), uiState.selectedType, onTypeChange)
                    FilterDropdown("Facturación", listOf("", "No Facturado", "Facturado", "Pagado"), uiState.selectedBilling, onBillingChange)
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

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value?.let { dateFormat.format(Date(it)) } ?: "",
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