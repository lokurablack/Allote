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

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN DE CLIMA ---
        uiState.weatherReport?.let { report ->
            item {
                CurrentWeatherCard(
                    report = report,
                    locationName = uiState.locationName,
                    isExpanded = isWeatherCardExpanded,
                    onClick = { isWeatherCardExpanded = !isWeatherCardExpanded },
                    onShowHourlyForecast = onShowHourlyForecast
                )
            }
        }

        item {
            SummaryCard(
                modifier = Modifier.fillMaxWidth(0.7f),
                title = "Pendientes",
                value = uiState.trabajosPendientes.toString(),
                subtitle = "%.2f ha".format(uiState.hectareasPendientes), // Pasar el subtítulo
                icon = Icons.Default.HourglassTop,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = { onNavigate(AppDestinations.JOBS_ROUTE) }
            )
        }

        // --- SECCIÓN DE NOTICIAS ---
        if (uiState.newsState.articles.isNotEmpty()) {
            item {
                Text(
                    text = "Noticias del Agro",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
            }
        }

        items(uiState.newsState.articles) { article ->
            NewsArticleCard(
                article = article,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                    context.startActivity(intent)
                }
            )
        }

        if (uiState.newsState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Lógica para el scroll infinito
    val buffer = 5
    LaunchedEffect(listState, uiState.newsState.articles) {
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
    onShowHourlyForecast: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
            Column(modifier = Modifier.padding(16.dp)) {
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
