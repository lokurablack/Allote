package com.example.allote.ui.administraciongeneral

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FiltroFechaMenu(
    onFiltroSeleccionado: (TipoFiltroFecha) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opciones de filtro")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TipoFiltroFecha.entries.forEach { tipoFiltro ->
                DropdownMenuItem(
                    text = { Text(tipoFiltro.displayName) },
                    onClick = {
                        onFiltroSeleccionado(tipoFiltro)
                        expanded = false
                    }
                )
            }
        }
    }
}