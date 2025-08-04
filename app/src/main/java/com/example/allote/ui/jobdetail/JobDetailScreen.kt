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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Detalles del Trabajo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla centraliza toda la información y acciones para este trabajo.")
                Text("• Información General: Revisa los detalles del cliente, fechas y estado del trabajo.")
                Text("• Menú de Acciones: Es el centro de control. Desde aquí puedes acceder a:")
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("- Costos: Para facturar el trabajo.")
                    Text("- Recetas: Para crear la mezcla de productos.")
                    Text("- Lotes: Para dividir el trabajo y registrar la superficie real tratada.")
                }
                Text("• Pronóstico: Consulta el clima para planificar la aplicación.", style = MaterialTheme.typography.bodySmall)
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
private fun JobSummaryCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Cliente", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = job.clientName, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoColumn(icon = Icons.Default.Map, label = "Hectáreas", value = job.surface.toString(), modifier = Modifier.weight(1f))
                InfoColumn(icon = Icons.Default.Timelapse, label = "Estado", value = job.status, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun JobDetailsCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(icon = Icons.Default.CalendarToday, label = "Creado", value = DateFormatter.formatDate(job.date))
            InfoRow(icon = Icons.Default.PlayArrow, label = "Inicio real", value = formatDateOrDefault(job.startDate))
            InfoRow(icon = Icons.Default.Stop, label = "Finalizado", value = formatDateOrDefault(job.endDate?: 0L))
            InfoRow(icon = Icons.Default.MonetizationOn, label = "Facturación", value = job.billingStatus)
            InfoRow(icon = Icons.AutoMirrored.Filled.Notes, label = "Notas", value = job.notes?.takeIf { it.isNotBlank() } ?: "N/A")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobActionMenu(jobId: Int, onNavigate: (String) -> Unit, onLocationButtonClick: () -> Unit) {
    Column {
        Text("Acciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ListItem(
                    headlineContent = { Text("Costos") },
                    leadingContent = { Icon(Icons.Filled.Calculate, contentDescription = "Costos") },
                    modifier = Modifier.clickable { onNavigate(AppDestinations.ADMINISTRACION_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ListItem(
                    headlineContent = { Text("Parámetros utilizados") },
                    leadingContent = { Icon(Icons.Default.Tune, contentDescription = "Parámetros") },
                    modifier = Modifier.clickable { onNavigate(AppDestinations.PARAMETROS_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ListItem(
                    headlineContent = { Text("Recetas") },
                    leadingContent = { Icon(Icons.Default.Science, contentDescription = "Recetas") },
                    modifier = Modifier.clickable { onNavigate(AppDestinations.RECETAS_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ListItem(
                    headlineContent = { Text("Ubicación") },
                    leadingContent = { Icon(Icons.Default.PinDrop, contentDescription = "Ubicación") },
                    modifier = Modifier.clickable(onClick = onLocationButtonClick)
                )
                ListItem(
                    headlineContent = { Text("Imágenes del Trabajo") },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = "Imágenes") },
                    modifier = Modifier.clickable { onNavigate(AppDestinations.IMAGES_JOB_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
                ListItem(
                    headlineContent = { Text("Gestión de Lotes") },
                    leadingContent = { Icon(Icons.Default.Grain, contentDescription = "Lotes") },
                    modifier = Modifier.clickable { onNavigate(AppDestinations.GESTION_LOTES_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())) }
                )
            }
        }
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
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(text = value)
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
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    TopAppBar(
                        title = { Text(text = "Pronóstico Horario", fontSize = 18.sp) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Cerrar")
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    items(dayForecast.hourly) { weather ->
                        HourlyForecastItem(
                            weather = weather,
                            minTempOfDay = dayForecast.minTemp,
                            maxTempOfDay = dayForecast.maxTemp
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna de la izquierda con la hora y los detalles
        Column(
            modifier = Modifier.width(80.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            HourlyDetail(
                icon = Icons.Default.WaterDrop,
                text = "${weather.precipitationProbability}%"
            )
            HourlyDetail(
                icon = Icons.Default.Air,
                text = "${weather.windSpeed.roundToInt()} km/h"
            )
        }

        // Gráfico en el centro
        TemperatureBar(
            modifier = Modifier.weight(1f),
            minTemp = minTempOfDay,
            maxTemp = maxTempOfDay,
            currentTemp = weather.temperature
        )

        // Icono a la derecha
        Icon(
            imageVector = WeatherIconMapper.getIcon(weather.weatherType),
            contentDescription = weather.weatherType.weatherDesc,
            modifier = Modifier
                .size(36.dp)
                .padding(start = 16.dp),
            tint = MaterialTheme.colorScheme.primary
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
                    bottom.linkTo(parent.bottom) // Anclar también abajo para centrar verticalmente el layout
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