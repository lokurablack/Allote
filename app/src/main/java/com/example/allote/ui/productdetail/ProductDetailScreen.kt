package com.example.allote.ui.productdetail

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.allote.data.ApplicationType
import com.example.allote.data.Product

// === FIRMA CORREGIDA ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onUpdateProduct: (Product) -> Unit
) {
    val context = LocalContext.current

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.product == null) {
        // En un caso real, la navegación ni siquiera debería llegar aquí si el producto es nulo.
        // El NavHost se encargaría. Pero es una buena salvaguarda.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Producto no encontrado.")
        }
    } else {
        // Estado local para los campos del formulario, que se reinicia si el producto cambia
        val product = uiState.product
        var nombreComercial by remember(product) { mutableStateOf(product.nombreComercial) }
        var principioActivo by remember(product) { mutableStateOf(product.principioActivo ?: "") }
        var selectedTipo by remember(product) { mutableStateOf(product.tipo) }
        var selectedFormulacion by remember(product) { mutableStateOf(uiState.allFormulaciones.find { it.id == product.formulacionId }) }
        var bandaToxicologica by remember(product) { mutableStateOf(product.bandaToxicologica ?: "") }

        var isTipoExpanded by remember { mutableStateOf(false) }
        var isFormulacionExpanded by remember { mutableStateOf(false) }
        var isBandaExpanded by remember { mutableStateOf(false) }

        val tiposPulverizacion = listOf("Herbicida", "Insecticida", "Fungicida", "Fertilizante", "Coadyuvante", "PGR", "Bactericida", "Defoliante", "Desecante", "Acaricida", "Nematicida", "Otros")
        val bandasToxicologicas = listOf("Ia", "Ib", "II", "III", "IV")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Detalle del Producto", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = nombreComercial, onValueChange = { nombreComercial = it }, label = { Text("Nombre Comercial") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            if (product.applicationType == ApplicationType.PULVERIZACION) {
                OutlinedTextField(value = principioActivo, onValueChange = { principioActivo = it }, label = { Text("Principio Activo") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = isTipoExpanded, onExpandedChange = { isTipoExpanded = it }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedTipo, onValueChange = {}, readOnly = true, label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTipoExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isTipoExpanded, onDismissRequest = { isTipoExpanded = false }) {
                        tiposPulverizacion.forEach { tipo -> DropdownMenuItem(text = { Text(tipo) }, onClick = { selectedTipo = tipo; isTipoExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = isFormulacionExpanded, onExpandedChange = { isFormulacionExpanded = it }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedFormulacion?.nombre ?: "", onValueChange = {}, readOnly = true, label = { Text("Formulación") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFormulacionExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isFormulacionExpanded, onDismissRequest = { isFormulacionExpanded = false }) {
                        uiState.allFormulaciones.forEach { formulacion ->
                            DropdownMenuItem(text = { Text(formulacion.nombre) }, onClick = { selectedFormulacion = formulacion; isFormulacionExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = isBandaExpanded, onExpandedChange = { isBandaExpanded = it }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = bandaToxicologica, onValueChange = {}, readOnly = true, label = { Text("Banda Toxicológica") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBandaExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = isBandaExpanded, onDismissRequest = { isBandaExpanded = false }) {
                        bandasToxicologicas.forEach { banda ->
                            DropdownMenuItem(text = { Text(banda) }, onClick = { bandaToxicologica = banda; isBandaExpanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val updatedProduct = product.copy(
                        nombreComercial = nombreComercial,
                        principioActivo = principioActivo.takeIf { it.isNotBlank() },
                        tipo = selectedTipo,
                        formulacionId = selectedFormulacion?.id,
                        bandaToxicologica = bandaToxicologica.takeIf { it.isNotBlank() }
                    )
                    onUpdateProduct(updatedProduct)
                    Toast.makeText(context, "Producto actualizado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cambios")
            }
        }
    }
}