package com.example.allote.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.allote.data.DolarValue
import com.example.allote.ui.main.DolarInfo

@Composable
fun DolarTickerBar(
    dolarInfo: DolarInfo,
) {
    if (dolarInfo.isLoading || dolarInfo.error != null || dolarInfo.dolarBlue == null || dolarInfo.dolarOficial == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (dolarInfo.error != null) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (dolarInfo.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Cargando cotizaciones...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (dolarInfo.error != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error al cargar cotizaciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        return
    }

    // Enhanced ticker with better visual design
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2E7D32), // Verde oscuro
                            Color(0xFF388E3C), // Verde medio
                            Color(0xFF4CAF50)  // Verde claro
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Dólar Oficial
                DolarInfoCard(
                    title = "USD Oficial",
                    buyValue = dolarInfo.dolarOficial.valueBuy,
                    sellValue = dolarInfo.dolarOficial.valueSell,
                    modifier = Modifier.weight(1f)
                )
                
                // Separador
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                
                // Dólar Blue
                DolarInfoCard(
                    title = "USD Blue",
                    buyValue = dolarInfo.dolarBlue.valueBuy,
                    sellValue = dolarInfo.dolarBlue.valueSell,
                    modifier = Modifier.weight(1f),
                    isBlue = true
                )
            }
        }
    }
}

@Composable
fun DolarInfoCard(
    title: String,
    buyValue: Double,
    sellValue: Double,
    modifier: Modifier = Modifier,
    isBlue: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DolarValueChip(
                label = "C",
                value = buyValue,
                isBlue = isBlue
            )
            
            DolarValueChip(
                label = "V",
                value = sellValue,
                isBlue = isBlue
            )
        }
    }
}

@Composable
fun DolarValueChip(
    label: String,
    value: Double,
    isBlue: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(
                if (isBlue) Color(0xFF1976D2).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = "$$value",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

private fun buildTickerString(oficial: DolarValue, blue: DolarValue): String {
    val separator = "   -   "
    val dot = " • "
    val oficialText = "Dólar Oficial${dot}Compra: $${oficial.valueBuy}${dot}Venta: $${oficial.valueSell}"
    val blueText = "Dólar Blue${dot}Compra: $${blue.valueBuy}${dot}Venta: $${blue.valueSell}"

    return "$oficialText$separator$blueText        "
}