package com.example.allote.ui.docviewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.allote.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    onNavigateUp: () -> Unit,
    viewModel: DocumentViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Movimiento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.movimiento == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Movimiento no encontrado.")
            }
        } else {
            val movimiento = uiState.movimiento!!
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- SECCIÓN DE DETALLES ---
                item {
                    Column {
                        Text(movimiento.descripcion, style = MaterialTheme.typography.headlineSmall)
                        val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
                        Text(
                            text = dateFormat.format(Date(movimiento.fecha)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        val monto = movimiento.haber - movimiento.debe
                        Text(
                            text = CurrencyFormatter.format(monto, uiState.currencySettings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (monto >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        // --- MUESTRA LAS NOTAS ---
                        if (!movimiento.detallesPago.isNullOrBlank()) {
                            Text(
                                text = movimiento.detallesPago,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // --- SECCIÓN DE DOCUMENTOS ---
                if (uiState.documentos.isNotEmpty()) {
                    item {
                        Text(
                            "Documentos Adjuntos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    items(uiState.documentos, key = { it.id }) { documento ->
                        DocumentoItem(
                            documento = documento,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(documento.uri), documento.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Abrir con..."))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentoItem(documento: com.example.allote.data.DocumentoMovimiento, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = "Documento")
            Spacer(Modifier.width(16.dp))
            Text(documento.fileName ?: "Documento", style = MaterialTheme.typography.bodyLarge)
        }
    }
}