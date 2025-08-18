package com.example.allote.ui.jobs

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.Job
import com.example.allote.ui.components.EmptyState
import com.example.allote.ui.components.JobDialogCompose
import java.text.SimpleDateFormat
import java.util.*

// --- Main Screen Composable ---

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
    var isCalendarView by remember { mutableStateOf(false) } // New state for view toggle

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gestión de Trabajos") },
                actions = {
                    IconButton(onClick = { isCalendarView = !isCalendarView }) {
                        Icon(
                            if (isCalendarView) Icons.Default.List else Icons.Default.CalendarToday,
                            contentDescription = if (isCalendarView) "Vista Lista" else "Vista Calendario"
                        )
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        JobsHeader(
                            totalJobs = uiState.allJobs.size,
                            pendingHectares = uiState.pendingHectares,
                            onFilterClick = { showFilterDialog = true }
                        )
                    }

                    if (isCalendarView) {
                        // Calendar View
                        item {
                            JobsCalendarView(
                                jobs = uiState.filteredJobs,
                                onJobClick = onJobClick,
                                onOptionsClick = { showJobOptionsDialog = it }
                            )
                        }
                    } else {
                        // Original List View
                        if (uiState.filteredJobs.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No se encontraron trabajos con esos filtros.", textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            if (uiState.pendingJobs.isNotEmpty()) {
                                item {
                                    SectionTitle("Pendientes", uiState.pendingJobs.size)
                                }
                                items(uiState.pendingJobs, key = { it.id }) { job ->
                                    JobListItem(
                                        job = job,
                                        onClick = { onJobClick(job.id) },
                                        onOptionsClick = { showJobOptionsDialog = job }
                                    )
                                }
                            }
                            if (uiState.finishedJobs.isNotEmpty()) {
                                item {
                                    SectionTitle("Finalizados", uiState.finishedJobs.size)
                                }
                                items(uiState.finishedJobs, key = { it.id }) { job ->
                                    JobListItem(
                                        job = job,
                                        onClick = { onJobClick(job.id) },
                                        onOptionsClick = { showJobOptionsDialog = job }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ... existing dialogs code ...

    if (showFilterDialog) {
        FilterDialog(
            uiState = uiState,
            onDismiss = { showFilterDialog = false },
            onFilterChange = onFilterChange,
            onDateChange = onDateChange,
            onClearFilters = {
                onFilterChange("", "", "", "")
                onDateChange(null, null)
            }
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

    showJobOptionsDialog?.let { currentJob ->
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

// --- New Calendar View Component ---

@Composable
private fun JobsCalendarView(
    jobs: List<Job>,
    onJobClick: (Int) -> Unit,
    onOptionsClick: (Job) -> Unit
) {
    val calendar = Calendar.getInstance()
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }

    val jobsForMonth = remember(jobs, year, month) {
        jobs.filter { job ->
            val jobDate = Calendar.getInstance().apply {
                timeInMillis = when (job.status) {
                    "Finalizado" -> job.endDate ?: job.startDate ?: System.currentTimeMillis()
                    else -> job.startDate ?: System.currentTimeMillis()
                }
            }
            jobDate.get(Calendar.YEAR) == year && jobDate.get(Calendar.MONTH) == month
        }.sortedBy { it.startDate }
    }

    val jobsByDate = remember(jobsForMonth) {
        jobsForMonth.groupBy { job ->
            val jobDate = Calendar.getInstance().apply {
                timeInMillis = when (job.status) {
                    "Finalizado" -> job.endDate ?: job.startDate ?: System.currentTimeMillis()
                    else -> job.startDate ?: System.currentTimeMillis()
                }
            }
            "${jobDate.get(Calendar.DAY_OF_MONTH)}-${jobDate.get(Calendar.MONTH)}-${jobDate.get(Calendar.YEAR)}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Calendar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (month == 0) {
                        month = 11
                        year--
                    } else {
                        month--
                    }
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes Anterior")
                }
                Text(
                    text = "${getMonthName(month)} $year",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    if (month == 11) {
                        month = 0
                        year++
                    } else {
                        month++
                    }
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes Siguiente")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem("Pendiente", MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                LegendItem("Finalizado", MaterialTheme.colorScheme.primary)
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Days of week header
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
                daysOfWeek.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val daysInMonth = getDaysInMonth(year, month)
            val firstDayOfMonth = getFirstDayOfWeek(year, month)
            val totalCells = if ((daysInMonth + firstDayOfMonth - 1) <= 35) 35 else 42

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(((totalCells / 7) * 48).dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(totalCells) { index ->
                    val dayNumber = index - firstDayOfMonth + 2
                    val isValidDay = dayNumber > 0 && dayNumber <= daysInMonth

                    if (isValidDay) {
                        val dateKey = "$dayNumber-$month-$year"
                        val dayJobs = jobsByDate[dateKey] ?: emptyList()

                        CalendarDay(
                            day = dayNumber,
                            jobs = dayJobs,
                            onDayClick = { clickedJobs ->
                                if (clickedJobs.isNotEmpty()) {
                                    onJobClick(clickedJobs.first().id)
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Show jobs for selected date (if any)
            if (jobsForMonth.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trabajos del mes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                jobsForMonth.forEach { job ->
                    JobSummaryItem(
                        job = job,
                        onClick = { onJobClick(job.id) },
                        onOptionsClick = { onOptionsClick(job) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    jobs: List<Job>,
    onDayClick: (List<Job>) -> Unit
) {
    val pendingJobs = jobs.filter { it.status == "Pendiente" }
    val finishedJobs = jobs.filter { it.status == "Finalizado" }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onDayClick(jobs) },
        contentAlignment = Alignment.Center
    ) {
        // Background circle if there are jobs
        if (jobs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            pendingJobs.isNotEmpty() && finishedJobs.isNotEmpty() ->
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            pendingJobs.isNotEmpty() ->
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            finishedJobs.isNotEmpty() ->
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        },
                        shape = CircleShape
                    )
            )
        }

        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (jobs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
            color = when {
                jobs.isNotEmpty() -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Job indicators (small dots)
        if (jobs.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (pendingJobs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
                if (finishedJobs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JobSummaryItem(
    job: Job,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${job.surface} ha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (job.status == "Pendiente")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
}

// --- Calendar Utility Functions ---

private fun getMonthName(month: Int): String {
    val months = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return months.getOrElse(month) { "Invalid month" }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return calendar.get(Calendar.DAY_OF_WEEK)
}

// --- Reusable Styled Components ---

@Composable
private fun JobsHeader(
    totalJobs: Int,
    pendingHectares: Double,
    onFilterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Resumen de Trabajos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalJobs trabajos registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "%.2f".format(pendingHectares),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Hectáreas Pendientes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onFilterClick) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Filtrar")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun JobListItem(job: Job, onClick: () -> Unit, onOptionsClick: () -> Unit) {
    val defaultColor = MaterialTheme.colorScheme.outline
    val (jobTypeColor, jobTypeIcon) = remember(job.tipoAplicacion, defaultColor) {
        when (job.tipoAplicacion?.lowercase()) {
            "aplicacion liquida" -> Pair(Color(0xFF0288D1), Icons.Default.WaterDrop)
            "aplicacion solida" -> Pair(Color(0xFFFBC02D), Icons.Default.Grain)
            "aplicacion mixta" -> Pair(Color(0xFFE64A19), Icons.Default.Science)
            "aplicaciones varias" -> Pair(Color(0xFF388E3C), Icons.Default.Build)
            else -> Pair(defaultColor, Icons.AutoMirrored.Filled.HelpOutline)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(jobTypeColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = jobTypeIcon,
                    contentDescription = job.tipoAplicacion,
                    tint = jobTypeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.clientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${job.surface} ha - ${job.tipoAplicacion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!job.description.isNullOrBlank()) {
                    Text(
                        text = job.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onOptionsClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
            }
        }
    }
}

// --- Dialogs ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    uiState: JobsUiState,
    onDismiss: () -> Unit,
    onFilterChange: (String?, String?, String?, String?) -> Unit,
    onDateChange: (Long?, Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column {
                // Header
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Filtros de Búsqueda",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterDropdown("Cliente", listOf("") + uiState.allClients.map { "${it.name} ${it.lastname}" }, uiState.selectedClient, { onFilterChange(it, null, null, null) })
                    FilterDropdown("Estado", listOf("", "Pendiente", "Finalizado"), uiState.selectedStatus, { onFilterChange(null, it, null, null) })
                    FilterDropdown("Aplicación", listOf("", "Aplicacion liquida", "Aplicacion solida", "Aplicacion mixta", "Aplicaciones varias"), uiState.selectedType, { onFilterChange(null, null, it, null) })
                    FilterDropdown("Facturación", listOf("", "No Facturado", "Facturado", "Pagado"), uiState.selectedBilling, { onFilterChange(null, null, null, it) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateSelector("Desde", uiState.fromDate, { onDateChange(it, uiState.toDate) }, Modifier.weight(1f))
                        DateSelector("Hasta", uiState.toDate, { onDateChange(uiState.fromDate, it) }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Limpiar Filtros", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Limpiar Filtros")
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Ayuda: Gestión de Trabajos", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta pantalla muestra una lista de todos los trabajos, separados en 'Pendientes' y 'Finalizados'.")
                    Text("• Filtrar: Usa el botón 'Mostrar Filtros' para buscar trabajos por cliente, estado, tipo de aplicación, facturación o rango de fechas.")
                    Text("• Ver Detalles: Pulsa sobre un trabajo para ver su panel de detalles, que incluye el pronóstico del tiempo y el menú de acciones.")
                    Text("• Opciones: Pulsa el ícono de tres puntos en un trabajo para acceder a acciones rápidas como marcarlo como finalizado, editarlo, eliminarlo o gestionar su facturación.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Entendido")
                }
            }
        }
    }
}

@Composable
private fun JobOptionsDialog(
    job: Job,
    onDismiss: () -> Unit,
    onMarkAsFinished: (Job) -> Unit,
    onEdit: (Job) -> Unit,
    onDelete: (Job) -> Unit,
    onBilling: (Job) -> Unit,
    onRecipes: (Job) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Opciones: ${job.clientName}",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2
                )
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton(text = "Marcar como Finalizado", icon = Icons.Default.CheckCircle, onClick = { onMarkAsFinished(job) })
                    OptionButton(text = "Editar Trabajo", icon = Icons.Default.Edit, onClick = { onEdit(job) })
                    OptionButton(text = "Gestionar Facturación", icon = Icons.Default.MonetizationOn, onClick = { onBilling(job) })
                    OptionButton(text = "Ver Recetas", icon = Icons.Default.Science, onClick = { onRecipes(job) })
                    Spacer(Modifier.height(8.dp))
                    OptionButton(
                        text = "Eliminar Trabajo",
                        icon = Icons.Default.Delete,
                        onClick = { onDelete(job) },
                        isDestructive = true
                    )
                }
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val colors = if (isDestructive) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    val border = if (isDestructive) null else ButtonDefaults.outlinedButtonBorder

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        border = border
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

// --- Utility Functions and Composables ---

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "Todos" else selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option.isEmpty()) "Todos" else option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DateSelector(label: String, value: Long?, onDateSelected: (Long?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val displayText = value?.let { dateFormat.format(Date(it)) } ?: ""

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
