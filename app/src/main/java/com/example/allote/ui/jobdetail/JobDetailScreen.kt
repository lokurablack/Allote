package com.example.allote.ui.jobdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.allote.DateFormatter
import com.example.allote.LocationPickerDialog
import com.example.allote.LocationViewerDialog
import com.example.allote.data.DailyWeather
import com.example.allote.data.HourlyWeather
import com.example.allote.data.Job
import com.example.allote.ui.AppDestinations
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    uiState: JobDetailUiState,
    onUpdateLocation: (lat: Double, lng: Double) -> Unit,
    onNavigate: (route: String) -> Unit,
    onDaySelected: (DailyWeather) -> Unit,
    onDismissHourlyDialog: () -> Unit
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    uiState.selectedDayForecast?.let {
        HourlyForecastDialog(
            dayForecast = it,
            onDismiss = onDismissHourlyDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.job?.description ?: "Detalle del Trabajo") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.job == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Trabajo no encontrado.")
            }
        } else {
            val job = uiState.job
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                JobSummaryCard(job = job)
                JobDetailsCard(job = job)
                JobActionMenu(
                    jobId = job.id,
                    onNavigate = onNavigate,
                    onLocationButtonClick = { showLocationDialog = true }
                )
                uiState.forecast?.let { forecast ->
                    ForecastSection(
                        forecast = forecast,
                        onDaySelected = onDaySelected
                    )
                }
            }

            if (showLocationDialog) {
                var isPickerMode by remember { mutableStateOf(job.latitude == null || job.longitude == null) }
                val isEditing = job.latitude != null && job.longitude != null

                if (isPickerMode) {
                    LocationPickerDialog(
                        initialLat = job.latitude ?: -32.36,
                        initialLng = job.longitude ?: -62.31,
                        isEditing = isEditing,
                        onConfirm = { lat, lng ->
                            onUpdateLocation(lat, lng)
                            showLocationDialog = false
                        },
                        onDismiss = { showLocationDialog = false }
                    )
                } else {
                    job.latitude?.let { lat ->
                        job.longitude?.let { lng ->
                            LocationViewerDialog(
                                lat = lat,
                                lng = lng,
                                description = job.description ?: "",
                                onDismiss = { showLocationDialog = false },
                                onEdit = { isPickerMode = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
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
                // Header con gradiente
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
                                Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ayuda: Detalles del Trabajo",
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
                
                // Contenido
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpSection(
                        icon = Icons.Default.Info,
                        title = "Información General",
                        description = "Revisa los detalles del cliente, fechas y estado del trabajo.",
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HelpSection(
                        icon = Icons.Default.TouchApp,
                        title = "Menú de Acciones",
                        description = "Centro de control con acceso a todas las funcionalidades:",
                        color = MaterialTheme.colorScheme.secondary,
                        items = listOf(
                            "Costos: Para facturar el trabajo",
                            "Recetas: Para crear la mezcla de productos",
                            "Lotes: Para dividir el trabajo y registrar la superficie real tratada",
                            "Ubicación: Para ver y editar la ubicación",
                            "Imágenes: Para gestionar fotos del trabajo"
                        )
                    )
                    
                    HelpSection(
                        icon = Icons.Default.WbSunny,
                        title = "Pronóstico Climático",
                        description = "Consulta el clima para planificar la aplicación de manera óptima.",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun JobSummaryCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header con cliente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Cliente",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = job.clientName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Información principal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EnhancedInfoColumn(
                        icon = Icons.Default.Map,
                        label = "Hectáreas",
                        value = job.surface.toString()
                    )
                    
                    // Divider vertical
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    EnhancedInfoColumn(
                        icon = Icons.Default.Timelapse,
                        label = "Estado",
                        value = job.status,
                        statusColor = when (job.status.lowercase()) {
                            "completado" -> Color(0xFF4CAF50)
                            "pendiente" -> Color(0xFFFF9800)
                            "en progreso" -> Color(0xFF2196F3)
                            else -> Color.White
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun JobDetailsCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Detalles del Trabajo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            EnhancedInfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Creado",
                value = DateFormatter.formatDate(job.date),
                iconColor = MaterialTheme.colorScheme.secondary
            )
            EnhancedInfoRow(
                icon = Icons.Default.PlayArrow,
                label = "Inicio real",
                value = formatDateOrDefault(job.startDate),
                iconColor = Color(0xFF4CAF50)
            )
            EnhancedInfoRow(
                icon = Icons.Default.Stop,
                label = "Finalizado",
                value = formatDateOrDefault(job.endDate ?: 0L),
                iconColor = Color(0xFFE57373)
            )
            EnhancedInfoRow(
                icon = Icons.Default.MonetizationOn,
                label = "Facturación",
                value = job.billingStatus,
                iconColor = Color(0xFFFFB74D),
                valueColor = when (job.billingStatus.lowercase()) {
                    "facturado" -> Color(0xFF4CAF50)
                    "pendiente" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            EnhancedInfoRow(
                icon = Icons.AutoMirrored.Filled.Notes,
                label = "Notas",
                value = job.notes?.takeIf { it.isNotBlank() } ?: "Sin notas",
                iconColor = MaterialTheme.colorScheme.tertiary,
                isMultiline = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobActionMenu(jobId: Int, onNavigate: (String) -> Unit, onLocationButtonClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Acciones Disponibles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Grid de acciones con cards individuales
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Calculate,
                    title = "Costos",
                    subtitle = "Administrar",
                    color = Color(0xFF4CAF50),
                    onClick = { onNavigate(AppDestinations.ADMINISTRACION_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Tune,
                    title = "Parámetros",
                    subtitle = "Configurar",
                    color = Color(0xFF2196F3),
                    onClick = { onNavigate(AppDestinations.PARAMETROS_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Science,
                    title = "Recetas",
                    subtitle = "Mezclas",
                    color = Color(0xFF9C27B0),
                    onClick = { onNavigate(AppDestinations.RECETAS_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PinDrop,
                    title = "Ubicación",
                    subtitle = "Ver mapa",
                    color = Color(0xFFFF5722),
                    onClick = onLocationButtonClick
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image,
                    title = "Imágenes",
                    subtitle = "Galería",
                    color = Color(0xFFFF9800),
                    onClick = { onNavigate(AppDestinations.IMAGES_JOB_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Grain,
                    title = "Lotes",
                    subtitle = "Gestionar",
                    color = Color(0xFF795548),
                    onClick = { onNavigate(AppDestinations.GESTION_LOTES_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
            }
        }
    }
}

@Composable
private fun EnhancedInfoColumn(
    icon: ImageVector,
    label: String,
    value: String,
    statusColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = statusColor
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoColumn(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EnhancedInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isMultiline: Boolean = false
) {
    Row(
        verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconColor.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                maxLines = if (isMultiline) Int.MAX_VALUE else 1
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(text = value)
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),
                            color
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HelpSection(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    items: List<String> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color.copy(alpha = 0.6f),
                                    CircleShape
                                )
                                .padding(top = 6.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDateOrDefault(timestamp: Long): String {
    return if (timestamp > 0L) DateFormatter.formatMillis(timestamp) else "No establecida"
}

@Composable
private fun ForecastSection(
    forecast: List<DailyWeather>,
    onDaySelected: (DailyWeather) -> Unit
) {
    Column {
        Text("Pronóstico Climático", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            items(forecast) { dailyWeather ->
                ForecastCard(
                    weather = dailyWeather,
                    onClick = { onDaySelected(dailyWeather) }
                )
            }
        }
    }
}

@Composable
private fun ForecastCard(
    weather: DailyWeather,
    onClick: () -> Unit
) {
    val date = remember(weather.date) {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.forLanguageTag("es-ES"))
        val formatter = java.text.SimpleDateFormat("EEE dd", java.util.Locale.forLanguageTag("es-ES"))
        parser.parse(weather.date)?.let { formatter.format(it).replaceFirstChar { char -> char.uppercase() } } ?: ""
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)

                Icon(
                    imageVector = WeatherIconMapper.getIcon(weather.weatherType),
                    contentDescription = weather.weatherType.weatherDesc,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )

                Text(
                    text = "${weather.maxTemp.roundToInt()}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${weather.minTemp.roundToInt()}°",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WeatherInfoChip(
                        icon = Icons.Default.WaterDrop,
                        text = "${weather.precipitationSum.roundToInt()} mm"
                    )
                    WeatherInfoChip(
                        icon = Icons.Default.Air,
                        text = "${weather.maxWindSpeed.roundToInt()} km/h"
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherInfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(14.dp),
            tint = Color.White
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourlyForecastDialog(
    dayForecast: DailyWeather,
    onDismiss: () -> Unit
) {
    val date = remember(dayForecast.date) {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.forLanguageTag("es-ES"))
        val formatter = java.text.SimpleDateFormat("EEEE, dd 'de' MMMM", java.util.Locale.forLanguageTag("es-ES"))
        parser.parse(dayForecast.date)?.let { formatter.format(it).replaceFirstChar { char -> char.titlecase() } } ?: ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column {
                // Header con gradiente
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
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pronóstico Horario",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
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
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Resumen del día
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = WeatherIconMapper.getIcon(dayForecast.weatherType),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${dayForecast.maxTemp.roundToInt()}°/${dayForecast.minTemp.roundToInt()}°",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${dayForecast.precipitationSum.roundToInt()} mm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Contenido principal
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayForecast.hourly) { weather ->
                        HourlyForecastItem(
                            weather = weather,
                            minTempOfDay = dayForecast.minTemp,
                            maxTempOfDay = dayForecast.maxTemp
                        )
                    }
                }
            }
        }
    }
}

// --- INICIO DEL NUEVO DISEÑO ---

@Composable
private fun HourlyForecastItem(
    weather: HourlyWeather,
    minTempOfDay: Double,
    maxTempOfDay: Double
) {
    val time = remember(weather.time) { weather.time.substring(11, 16) }
    val isHighTemp = weather.temperature > (minTempOfDay + maxTempOfDay) / 2
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighTemp) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Columna de tiempo
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${weather.temperature.roundToInt()}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isHighTemp) 
                        MaterialTheme.colorScheme.error
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
            
            // Icono del clima
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isHighTemp)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WeatherIconMapper.getIcon(weather.weatherType),
                    contentDescription = weather.weatherType.weatherDesc,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Información meteorológica
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Barra de temperatura mejorada
                EnhancedTemperatureBar(
                    minTemp = minTempOfDay,
                    maxTemp = maxTempOfDay,
                    currentTemp = weather.temperature
                )
                
                // Detalles en fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeatherMetricChip(
                        icon = Icons.Default.WaterDrop,
                        value = "${weather.precipitationProbability}%",
                        label = "Lluvia",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    WeatherMetricChip(
                        icon = Icons.Default.Air,
                        value = "${weather.windSpeed.roundToInt()}",
                        label = "km/h",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    WeatherMetricChip(
                        icon = Icons.Default.Opacity,
                        value = "${weather.humidity}%",
                        label = "Hum.",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedTemperatureBar(
    minTemp: Double,
    maxTemp: Double,
    currentTemp: Double
) {
    val tempRange = (maxTemp - minTemp).toFloat().coerceAtLeast(1f)
    val normalizedPosition = ((currentTemp - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Etiquetas de temperatura
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minTemp.roundToInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${maxTemp.roundToInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Barra de temperatura con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            // Barra de fondo con gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF64B5F6), // Azul frío
                                Color(0xFF81C784), // Verde medio
                                Color(0xFFFFB74D), // Naranja cálido
                                Color(0xFFE57373)  // Rojo caliente
                            )
                        ),
                        RoundedCornerShape(4.dp)
                    )
            )
            
            // Indicador de temperatura actual
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedPosition)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            Color.White,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
fun WeatherMetricChip(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .background(
                    color.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TemperatureBar(
    modifier: Modifier = Modifier,
    minTemp: Double,
    maxTemp: Double,
    currentTemp: Double,
) {
    val tempRange = (maxTemp - minTemp).toFloat().coerceAtLeast(1f)
    val normalizedPosition = ((currentTemp - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)

    val pointColor = MaterialTheme.colorScheme.primary
    val trackColor = pointColor.copy(alpha = 0.2f)

    ConstraintLayout(modifier = modifier) {
        val (textRef, barRef, pointRef) = createRefs()

        Text(
            text = "${currentTemp.roundToInt()}°",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(textRef) {
                top.linkTo(parent.top)
                start.linkTo(barRef.start)
                end.linkTo(barRef.end)
                horizontalBias = normalizedPosition
            }
        )

        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .background(trackColor, CircleShape)
                .constrainAs(barRef) {
                    top.linkTo(textRef.bottom, margin = 4.dp)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(pointColor, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .constrainAs(pointRef) {
                    centerVerticallyTo(barRef)
                    start.linkTo(barRef.start)
                    end.linkTo(barRef.end)
                    horizontalBias = normalizedPosition
                }
        )
    }
}


@Composable
private fun HourlyDetail(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}