package com.example.allote.ui.workplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
// import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.allote.data.FlightSegment
import com.example.allote.data.Job
import com.example.allote.data.Lote
import com.example.allote.data.WorkPlan
import com.example.allote.ui.workplan.BoundaryPoint
import com.example.allote.ui.workplan.components.LotBoundaryDialog
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPlanScreen(
    uiState: WorkPlanUiState,
    onExtensionEOChanged: (String) -> Unit,
    onExtensionNSChanged: (String) -> Unit,
    onCaudalChanged: (String) -> Unit,
    onInterlineadoChanged: (String) -> Unit,
    onVelocidadTrabajoChanged: (String) -> Unit,
    onAutonomiaChanged: (String) -> Unit,
    onCapacidadTanqueChanged: (String) -> Unit,
    onTiempoReabastecimientoChanged: (String) -> Unit,
    onDroneCountChanged: (String) -> Unit,
    onLatReabastecedorChanged: (String) -> Unit,
    onLngReabastecedorChanged: (String) -> Unit,
    onDireccionVientoChanged: (Float) -> Unit,
    onVelocidadVientoChanged: (String) -> Unit,
    onCalculatePlan: () -> Unit,
    onDeletePlan: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSuccess: () -> Unit,
    onUseJobLocationForRefuel: () -> Unit,
    onUseLoteLocationForRefuel: () -> Unit,
    onBoundaryDefined: (List<BoundaryPoint>) -> Unit,
    onBoundaryCleared: () -> Unit
) {
    var showBoundaryDialog by remember { mutableStateOf(false) }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (showBoundaryDialog) {
        LotBoundaryDialog(
            initialPoints = uiState.lotBoundary,
            onConfirm = { points ->
                onBoundaryDefined(points)
                showBoundaryDialog = false
            },
            onDismiss = { showBoundaryDialog = false }
        )
    }

    if (uiState.showSuccess) {
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            title = { Text("Plan actualizado") },
            text = { Text("El plan de trabajo se calculo correctamente.") },
            confirmButton = {
                TextButton(onClick = onDismissSuccess) {
                    Text("Continuar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Planificacion de trabajo")
                        uiState.lote?.let { lote ->
                            Text(
                                text = "Lote: ${lote.nombre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                JobInfoCard(job = uiState.job, lote = uiState.lote)
            }

            uiState.currentPlan?.let { plan ->
                item {
                    PlanSummaryCard(plan = plan, onDeletePlan = onDeletePlan)
                }
                item {
                    FlightSegmentsCard(segments = uiState.flightSegments)
                }
            }

            item {
                ConfigurationCard(
                    uiState = uiState,
                    onExtensionEOChanged = onExtensionEOChanged,
                    onExtensionNSChanged = onExtensionNSChanged,
                    onCaudalChanged = onCaudalChanged,
                    onInterlineadoChanged = onInterlineadoChanged,
                    onVelocidadTrabajoChanged = onVelocidadTrabajoChanged,
                    onAutonomiaChanged = onAutonomiaChanged,
                    onCapacidadTanqueChanged = onCapacidadTanqueChanged,
                    onTiempoReabastecimientoChanged = onTiempoReabastecimientoChanged,
                    onDroneCountChanged = onDroneCountChanged,
                    onLatReabastecedorChanged = onLatReabastecedorChanged,
                    onLngReabastecedorChanged = onLngReabastecedorChanged,
                    onDireccionVientoChanged = onDireccionVientoChanged,
                    onVelocidadVientoChanged = onVelocidadVientoChanged,
                    onUseJobLocationForRefuel = onUseJobLocationForRefuel,
                    onUseLoteLocationForRefuel = onUseLoteLocationForRefuel,
                    onDefineBoundary = { showBoundaryDialog = true },
                    onClearBoundary = onBoundaryCleared
                )
            }

            item {
                Button(
                    onClick = onCalculatePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isCalculating
                ) {
                    if (uiState.isCalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.currentPlan != null) "Recalcular plan" else "Calcular plan",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun JobInfoCard(job: Job?, lote: Lote?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = job?.description.orEmpty().ifBlank { "Trabajo sin descripcion" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InfoChip(
                        icon = Icons.Default.Person,
                        text = job?.clientName.orEmpty().ifBlank { "Cliente sin asignar" }
                    )
                    InfoChip(
                        icon = Icons.Default.Map,
                        text = "${lote?.hectareas ?: job?.surface ?: 0.0} ha"
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSummaryCard(plan: WorkPlan, onDeletePlan: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar plan") },
            text = { Text("Se eliminara el plan y sus segmentos. Continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlan()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Resumen del plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Ultima actualizacion: ${plan.fechaModificacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar plan",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    icon = Icons.Default.Flight,
                    label = "Vuelos",
                    value = plan.totalVuelos.toString()
                )
                SummaryMetric(
                    icon = Icons.Default.Timer,
                    label = "Tiempo",
                    value = "${plan.tiempoTotalEstimado} min"
                )
                SummaryMetric(
                    icon = Icons.Default.LocalGasStation,
                    label = "Reabastecimientos",
                    value = plan.numeroReabastecimientos.toString()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(
                    icon = Icons.Default.Navigation,
                    label = "Direccion de pasadas",
                    value = plan.direccionPasadas.replace("_", " ")
                )
                DetailRow(
                    icon = Icons.Default.Air,
                    label = "Estrategia respecto al viento",
                    value = plan.ordenPasadas.replace("_", " ")
                )
                DetailRow(
                    icon = Icons.Default.Route,
                    label = "Distancia total",
                    value = "${(plan.distanciaTotalRecorrida / 1000).roundToInt()} km"
                )
                DetailRow(
                    icon = Icons.Default.ArrowForward,
                    label = "Interlineado",
                    value = "${plan.interlineado} m"
                )
                DetailRow(
                    icon = Icons.Default.Timer,
                    label = "Autonomia estimada",
                    value = "${plan.autonomiaBateria} min"
                )
                DetailRow(
                    icon = Icons.Default.LocalGasStation,
                    label = "Capacidad de tanque",
                    value = "${plan.capacidadTanque} L"
                )
                DetailRow(
                    icon = Icons.Default.Flight,
                    label = "Drones en operacion",
                    value = plan.numeroDrones.toString()
                )
            }
        }
    }
}

@Composable
private fun FlightSegmentsCard(segments: List<FlightSegment>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Segmentos de vuelo (${segments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            segments.forEachIndexed { index, segment ->
                FlightSegmentItem(segment = segment)
                if (index < segments.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun FlightSegmentItem(segment: FlightSegment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Vuelo ${segment.ordenVuelo}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Area: ${segment.areaCubierta.format(2)} ha - Tiempo: ${segment.tiempoVuelo} min",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Producto: ${segment.productoPulverizado.format(1)} L - Distancia: ${segment.distancia.format(0)} m",
                style = MaterialTheme.typography.bodyMedium
            )
            segment.comentario?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (segment.requiereReabastecimiento) {
            Text(
                text = segment.tipoReabastecimiento ?: "Reabastecer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConfigurationCard(
    uiState: WorkPlanUiState,
    onExtensionEOChanged: (String) -> Unit,
    onExtensionNSChanged: (String) -> Unit,
    onCaudalChanged: (String) -> Unit,
    onInterlineadoChanged: (String) -> Unit,
    onVelocidadTrabajoChanged: (String) -> Unit,
    onAutonomiaChanged: (String) -> Unit,
    onCapacidadTanqueChanged: (String) -> Unit,
    onTiempoReabastecimientoChanged: (String) -> Unit,
    onDroneCountChanged: (String) -> Unit,
    onLatReabastecedorChanged: (String) -> Unit,
    onLngReabastecedorChanged: (String) -> Unit,
    onDireccionVientoChanged: (Float) -> Unit,
    onVelocidadVientoChanged: (String) -> Unit,
    onUseJobLocationForRefuel: () -> Unit,
    onUseLoteLocationForRefuel: () -> Unit,
    onDefineBoundary: () -> Unit,
    onClearBoundary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuracion de entrada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Dimensiones del lote",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.extensionEsteOeste,
                onValueChange = onExtensionEOChanged,
                label = { Text("Extension Este-Oeste (m)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.extensionNorteSur,
                onValueChange = onExtensionNSChanged,
                label = { Text("Extension Norte-Sur (m)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedButton(
                onClick = onDefineBoundary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Definir perimetro en el mapa")
            }

            uiState.boundaryAreaHa?.let { areaHa ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Perimetro definido",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Puntos: ${uiState.lotBoundary.size} | Area aproximada: ${areaHa.format(2)} ha",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onClearBoundary) {
                        Text("Eliminar perímetro")
                    }
                }
            } ?: run {
                Text(
                    text = "Usa el mapa para delimitar el lote y calcular automaticamente el area y las distancias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            Text(
                text = "Aplicacion",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.caudal,
                onValueChange = onCaudalChanged,
                label = { Text("Caudal (L/ha o Kg/ha)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            Divider()

            Text(
                text = "Parametros del equipo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.interlineado,
                onValueChange = onInterlineadoChanged,
                label = { Text("Interlineado (m)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.velocidadTrabajo,
                onValueChange = onVelocidadTrabajoChanged,
                label = { Text("Velocidad de trabajo (km/h)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.autonomiaBateria,
                onValueChange = onAutonomiaChanged,
                label = { Text("Autonomia de bateria (min)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.capacidadTanque,
                onValueChange = onCapacidadTanqueChanged,
                label = { Text("Capacidad de tanque (L)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.tiempoReabastecimiento,
                onValueChange = onTiempoReabastecimientoChanged,
                label = { Text("Tiempo de reabastecimiento (min)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.droneCount,
                onValueChange = onDroneCountChanged,
                label = { Text("Cantidad de drones") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Flight, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true
            )

            Divider()

            Text(
                text = "Ubicacion del reabastecedor",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUseJobLocationForRefuel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Usar ubicacion del trabajo", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onUseLoteLocationForRefuel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Usar ubicacion del lote", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = uiState.latReabastecedor,
                onValueChange = onLatReabastecedorChanged,
                label = { Text("Latitud") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.lngReabastecedor,
                onValueChange = onLngReabastecedorChanged,
                label = { Text("Longitud") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )

            Divider()

            Text(
                text = "Condiciones de viento",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = "Direccion: ${uiState.direccionViento.roundToInt()} deg (${getWindDirection(uiState.direccionViento)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.Slider(
                    value = uiState.direccionViento,
                    onValueChange = onDireccionVientoChanged,
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = uiState.velocidadViento,
                onValueChange = onVelocidadVientoChanged,
                label = { Text("Velocidad del viento (km/h)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Air, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                singleLine = true
            )
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), shape = MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun getWindDirection(degrees: Float): String {
    val value = degrees.roundToInt() % 360
    return when {
        value in 0..22 || value in 338..359 -> "Norte"
        value in 23..67 -> "Noreste"
        value in 68..112 -> "Este"
        value in 113..157 -> "Sureste"
        value in 158..202 -> "Sur"
        value in 203..247 -> "Suroeste"
        value in 248..292 -> "Oeste"
        value in 293..337 -> "Noroeste"
        else -> "N/A"
    }
}

private fun Double.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this).trimEnd('0').trimEnd('.')
}
