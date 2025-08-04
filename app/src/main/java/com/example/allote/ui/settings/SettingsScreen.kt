package com.example.allote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateToFormulaciones: () -> Unit,
    onSavePrice: (String, String) -> Unit,
    onSaveCurrency: (String) -> Unit,
    onSaveExchangeRate: (String) -> Unit,
    onUpdateRateFromApi: () -> Unit
) {
    var showDialogFor by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showDialogFor == "prices") {
        PricesScreen(
            uiState = uiState,
            onDismiss = { showDialogFor = null },
            onPriceChange = onSavePrice
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Configuración") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                SettingsHeader(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Currency Section
                SettingsSection(
                    title = "Configuración Monetaria",
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    EnhancedPreferenceCard(
                        icon = Icons.Default.CurrencyExchange,
                        title = "Moneda de Visualización",
                        summary = uiState.currencySettings.displayCurrency,
                        onClick = { showDialogFor = "currency" },
                        iconColor = Color(0xFF4CAF50)
                    )
                    
                    if (uiState.currencySettings.displayCurrency == "ARS") {
                        EnhancedPreferenceCard(
                            icon = Icons.Default.TrendingUp,
                            title = "Tasa de Cambio",
                            summary = "1 USD = ${uiState.currencySettings.exchangeRate} ARS",
                            onClick = { showDialogFor = "rate" },
                            iconColor = Color(0xFF2196F3)
                        )
                        
                        ActionButton(
                            text = "Actualizar Tasa Automáticamente",
                            subtitle = "Obtener del Dólar Blue",
                            icon = Icons.Default.Refresh,
                            isLoading = uiState.isUpdatingRate,
                            onClick = onUpdateRateFromApi,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Prices Section
                SettingsSection(
                    title = "Precios y Parámetros",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    EnhancedPreferenceCard(
                        icon = Icons.Default.LocalOffer,
                        title = "Precios por Tipo de Aplicación",
                        summary = getPricesSummary(uiState.precioLiquida, uiState.precioSolida, uiState.precioMixta, uiState.precioVarias),
                        onClick = { showDialogFor = "prices" },
                        iconColor = Color(0xFFFF9800)
                    )
                    
                    EnhancedPreferenceCard(
                        icon = Icons.Default.Science,
                        title = "Gestionar Formulaciones",
                        summary = "Editar, ordenar y crear nuevas formulaciones",
                        onClick = onNavigateToFormulaciones,
                        iconColor = Color(0xFF9C27B0)
                    )
                }
                
                // Info Section
                SettingsSection(
                    title = "Información",
                    icon = Icons.Default.Info,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    EnhancedPreferenceCard(
                        icon = Icons.Default.Help,
                        title = "Sobre esta App",
                        summary = "Conoce el propósito y las funciones principales",
                        onClick = { showAboutDialog = true },
                        iconColor = Color(0xFF607D8B)
                    )
                }
            }
        }
    }

    when (val dialogType = showDialogFor) {
        "currency" -> CurrencySelectionDialog(
            onDismiss = { showDialogFor = null },
            onSelect = { onSaveCurrency(it); showDialogFor = null }
        )
        "rate" -> PriceDialog(
            title = "Tasa de Cambio USD a ARS", value = uiState.currencySettings.exchangeRate.toString(),
            onConfirm = { onSaveExchangeRate(it); showDialogFor = null },
            onDismiss = { showDialogFor = null }, isNumeric = true
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Agriculture,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Al Lote - App Agrícola",
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
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Esta aplicación está diseñada para ser una herramienta integral de gestión para contratistas agrícolas, permitiendo un control total sobre trabajos, clientes y finanzas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.Work,
                        title = "Gestión de Trabajos y Clientes",
                        description = "Centraliza la información de tus clientes y todos los trabajos asociados a ellos, desde la planificación hasta la finalización.",
                        color = Color(0xFF4CAF50)
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.Science,
                        title = "Planificación Precisa",
                        description = "Crea recetas de aplicación detalladas, gestiona el orden de mezcla de productos y divide los trabajos en lotes.",
                        color = Color(0xFF2196F3)
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Control de Ejecución",
                        description = "Registra la superficie real trabajada en cada lote y calcula automáticamente el sobrante de insumos.",
                        color = Color(0xFFFF9800)
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.AccountBalance,
                        title = "Administración Financiera",
                        description = "Genera los costos de cada trabajo y lleva un control de la cuenta corriente de cada cliente.",
                        color = Color(0xFF9C27B0)
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.Settings,
                        title = "Personalización",
                        description = "Adapta la app a tu negocio configurando precios por tipo de aplicación, moneda de visualización y tasas de cambio.",
                        color = Color(0xFF607D8B)
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencySelectionDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Seleccionar Moneda",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CurrencyOption(
                    currency = "USD",
                    name = "Dólar Estadounidense",
                    icon = Icons.Default.AttachMoney,
                    onClick = { onSelect("USD") }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CurrencyOption(
                    currency = "ARS",
                    name = "Peso Argentino",
                    icon = Icons.Default.MonetizationOn,
                    onClick = { onSelect("ARS") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
fun CurrencyOption(
    currency: String,
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = currency,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onPriceChange: (String, String) -> Unit
) {
    var showDialogFor by remember { mutableStateOf<String?>(null) }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Configurar Precios") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") } }
            )
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                TextPreference("Aplicación Líquida", if (uiState.precioLiquida.isNotEmpty()) "${uiState.precioLiquida}/Ha" else "No establecido") { showDialogFor = "liquida" }
                HorizontalDivider()
                TextPreference("Aplicación Sólida", if (uiState.precioSolida.isNotEmpty()) "${uiState.precioSolida}/Ha" else "No establecido") { showDialogFor = "solida" }
                HorizontalDivider()
                TextPreference("Aplicación Mixta", if (uiState.precioMixta.isNotEmpty()) "${uiState.precioMixta}/Ha" else "No establecido") { showDialogFor = "mixta" }
                HorizontalDivider()
                TextPreference("Aplicaciones Varias", if (uiState.precioVarias.isNotEmpty()) "${uiState.precioVarias}/Ha" else "No establecido") { showDialogFor = "varias" }
            }
        }
    }

    showDialogFor?.let { key ->
        val initialValue = when (key) {
            "liquida" -> uiState.precioLiquida
            "solida" -> uiState.precioSolida
            "mixta" -> uiState.precioMixta
            "varias" -> uiState.precioVarias
            else -> ""
        }
        PriceDialog(
            title = "Precio Aplicación ${key.replaceFirstChar { it.uppercase() }}",
            value = initialValue,
            onConfirm = { newValue -> onPriceChange(key, newValue); showDialogFor = null },
            onDismiss = { showDialogFor = null },
            isNumeric = true
        )
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PriceDialog(
    title: String,
    value: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isNumeric: Boolean,
    onValueChange: ((String) -> Unit)? = null
) {
    var tempValue by remember { mutableStateOf(value) }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { newValue ->
                        if (isNumeric && (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$")))) {
                            tempValue = newValue
                            onValueChange?.invoke(newValue)
                        } else if (!isNumeric) {
                            tempValue = newValue
                            onValueChange?.invoke(newValue)
                        }
                    },
                    label = { Text("Valor") },
                    singleLine = true,
                    keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = { onConfirm(tempValue) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Personaliza tu experiencia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun EnhancedPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    subtitle: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun TextPreference(title: String, summary: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun getPricesSummary(liquida: String, solida: String, mixta: String, varias: String): String {
    val count = listOf(liquida, solida, mixta, varias).count { it.isNotEmpty() }
    return if (count == 0) "No configurado" else "$count de 4 tipos configurados"
}
