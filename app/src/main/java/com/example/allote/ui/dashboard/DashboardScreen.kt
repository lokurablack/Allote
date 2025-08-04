package com.example.allote.ui.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.allote.data.Article
import com.example.allote.data.WeatherReport
import com.example.allote.ui.AppDestinations
import com.example.allote.ui.components.SummaryCard
import com.example.allote.ui.jobdetail.HourlyForecastDialog
import com.example.allote.ui.main.MainUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onNavigate: (String) -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onFetchNextPage: () -> Unit,
    onRefresh: () -> Unit
) {
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onLocationPermissionGranted()
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    var showForecastDialog by remember { mutableStateOf(false) }

    if (showForecastDialog && uiState.weatherReport != null) {
        HourlyForecastDialog(
            dayForecast = uiState.weatherReport.daily.first(),
            onDismiss = { showForecastDialog = false }
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                DolarTickerBar(dolarInfo = uiState.dolarInfo)
                DashboardContent(
                    uiState = uiState,
                    onNavigate = onNavigate,
                    onShowHourlyForecast = { showForecastDialog = true },
                    onFetchNextPage = onFetchNextPage
                )
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun DashboardContent(
    uiState: MainUiState,
    onNavigate: (String) -> Unit,
    onShowHourlyForecast: () -> Unit,
    onFetchNextPage: () -> Unit
) {
    var isWeatherCardExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Obtener saludo basado en la hora
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (currentHour) {
        in 5..11 -> "Buenos días"
        in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    
    val currentDate = remember {
        val formatter = java.text.SimpleDateFormat("EEEE, dd 'de' MMMM", java.util.Locale("es", "ES"))
        formatter.format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER PERSONALIZADO ---
        item {
            DashboardHeader(
                greeting = greeting,
                date = currentDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // --- NAVEGACIÓN RÁPIDA ---
        item {
            QuickNavigationSection(
                onNavigate = onNavigate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // --- ESTADÍSTICAS RÁPIDAS ---
        item {
            QuickStatsSection(
                uiState = uiState,
                onNavigate = onNavigate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // --- SECCIÓN DE CLIMA ---
        uiState.weatherReport?.let { report ->
            item {
                CurrentWeatherCard(
                    report = report,
                    locationName = uiState.locationName,
                    isExpanded = isWeatherCardExpanded,
                    onClick = { isWeatherCardExpanded = !isWeatherCardExpanded },
                    onShowHourlyForecast = onShowHourlyForecast,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // --- SECCIÓN DE NOTICIAS ---
        if (uiState.newsState.articles.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Noticias del Agro",
                    subtitle = "Mantente informado",
                    icon = Icons.Default.Newspaper,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(uiState.newsState.articles) { article ->
            EnhancedNewsArticleCard(
                article = article,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                    context.startActivity(intent)
                }
            )
        }

        // --- MANEJO DE ESTADOS DE CARGA Y ERROR ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.newsState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.newsState.error != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error al cargar noticias",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.newsState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Lógica para el scroll infinito
    val buffer = 5
    LaunchedEffect(listState, uiState.newsState.articles, uiState.newsState.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty() && visibleItems.last().index >= uiState.newsState.articles.size - buffer && !uiState.newsState.isLoading) {
                    onFetchNextPage()
                }
            }
    }
}

// ... (CurrentWeatherCard y NewsArticleCard se mantienen igual que en la versión anterior)
@Composable
fun CurrentWeatherCard(
    report: WeatherReport,
    locationName: String?,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onShowHourlyForecast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            locationName ?: "Clima Actual",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            "${report.current.temperature.roundToInt()}°C",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            report.current.weatherType.weatherDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = report.current.weatherType.icon,
                        contentDescription = report.current.weatherType.weatherDesc,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            InfoColumn(
                                icon = Icons.Default.Air,
                                label = "Viento",
                                value = "${report.daily.first().dominantWindDirection} ${report.current.windSpeed.roundToInt()} km/h"
                            )
                            InfoColumn(
                                icon = Icons.Default.WaterDrop,
                                label = "Humedad",
                                value = "${report.daily.first().hourly.first().humidity}%"
                            )
                            InfoColumn(
                                icon = Icons.Default.Grain,
                                label = "Lluvia (hoy)",
                                value = "${report.daily.first().precipitationSum} mm"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onShowHourlyForecast,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("VER PRONÓSTICO POR HORA", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EnhancedNewsArticleCard(
    article: Article,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            Box {
                SubcomposeAsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Agriculture,
                                contentDescription = "Placeholder",
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
                
                // Overlay con gradiente para mejorar legibilidad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                ),
                                startY = 100f
                            )
                        )
                )
            }
            
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Source,
                            contentDescription = "Fuente",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = article.source_id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Abrir enlace",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            SubcomposeAsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Agriculture,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.source_id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// === COMPONENTES AUXILIARES PARA DASHBOARD ===

@Composable
fun DashboardHeader(
    greeting: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
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
                        imageVector = when {
                            greeting.contains("días") -> Icons.Default.WbSunny
                            greeting.contains("tardes") -> Icons.Default.WbTwilight
                            else -> Icons.Default.NightlightRound
                        },
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickNavigationSection(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Acceso Rápido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Work,
                title = "Trabajos",
                color = Color(0xFF4CAF50),
                onClick = { onNavigate(AppDestinations.JOBS_ROUTE) }
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.People,
                title = "Clientes",
                color = Color(0xFF2196F3),
                onClick = { onNavigate(AppDestinations.CLIENTS_ROUTE) }
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                title = "Administración",
                color = Color(0xFF9C27B0),
                onClick = { onNavigate(AppDestinations.ADMIN_ROUTE) }
            )
        }
    }
}

@Composable
fun QuickStatsSection(
    uiState: MainUiState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Resumen",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnhancedStatCard(
                modifier = Modifier.weight(1f),
                title = "Pendientes",
                value = uiState.trabajosPendientes.toString(),
                subtitle = "%.1f ha".format(uiState.hectareasPendientes),
                icon = Icons.Default.HourglassTop,
                color = Color(0xFFFF9800),
                onClick = { onNavigate(AppDestinations.JOBS_ROUTE) }
            )
            
            EnhancedStatCard(
                modifier = Modifier.weight(1f),
                title = "Clientes",
                value = uiState.totalClientes.toString(),
                subtitle = "Activos",
                icon = Icons.Default.Group,
                color = Color(0xFF4CAF50),
                onClick = { onNavigate(AppDestinations.CLIENTS_ROUTE) }
            )
            
            EnhancedStatCard(
                modifier = Modifier.weight(1f),
                title = "Saldo",
                value = if (uiState.saldoGeneral >= 0) "+$${uiState.saldoGeneral.toInt()}" else "-$${kotlin.math.abs(uiState.saldoGeneral.toInt())}",
                subtitle = uiState.currencySettings.displayCurrency,
                icon = Icons.Default.AccountBalance,
                color = if (uiState.saldoGeneral >= 0) Color(0xFF4CAF50) else Color(0xFFE57373),
                onClick = { onNavigate(AppDestinations.ADMIN_ROUTE) }
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
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
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EnhancedStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
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
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }
                
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
