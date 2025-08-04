package com.example.allote.ui.adminresumen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allote.data.ResumenData
import com.example.allote.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministracionResumenScreen(
    uiState: ResumenUiState
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Resumen de Administración") }) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.resumenData == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No se encontraron datos para este trabajo.")
            }
        } else {
            ResumenUI(
                resumen = uiState.resumenData,
                settings = uiState.currencySettings,
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        }
    }
}

@Composable
fun ResumenUI(resumen: ResumenData, settings: com.example.allote.data.CurrencySettings, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Detalles de Facturación",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            InfoRow(label = "Hectáreas:", value = "%.2f".format(resumen.hectareas))
            InfoRow(label = "Costo por hectárea:", value = CurrencyFormatter.format(resumen.costoPorHectarea, settings,))
            InfoRow(label = "IVA aplicado:", value = if (resumen.aplicaIVA) "Sí (10.5%)" else "No")
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            InfoRow(label = "Total sin IVA:", value = CurrencyFormatter.format(resumen.totalSinIVA, settings), isHighlight = true)
            InfoRow(label = "Total con IVA:", value = CurrencyFormatter.format(resumen.totalConIVA, settings), isHighlight = true)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if(isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = if(isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if(isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if(isHighlight) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    }
}