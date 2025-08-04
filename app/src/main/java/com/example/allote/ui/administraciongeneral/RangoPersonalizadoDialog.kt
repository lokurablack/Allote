package com.example.allote.ui.administraciongeneral

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RangoPersonalizadoDialog(
    onDismiss: () -> Unit,
    onConfirm: (inicio: Long, fin: Long) -> Unit
) {
    var fechaInicio by remember { mutableStateOf<Long?>(null) }
    var fechaFin by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Rango de Fechas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DateSelector(
                    label = "Desde",
                    fechaSeleccionada = fechaInicio,
                    onFechaSeleccionada = { fechaInicio = it }
                )
                DateSelector(
                    label = "Hasta",
                    fechaSeleccionada = fechaFin,
                    onFechaSeleccionada = { fechaFin = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inicio = fechaInicio ?: 0
                    val fin = fechaFin ?: Long.MAX_VALUE
                    onConfirm(inicio, fin)
                },
                // El botón solo se activa si ambas fechas han sido seleccionadas
                enabled = fechaInicio != null && fechaFin != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DateSelector(
    label: String,
    fechaSeleccionada: Long?,
    onFechaSeleccionada: (Long) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Box {
        OutlinedTextField(
            value = fechaSeleccionada?.let { dateFormat.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val cal = Calendar.getInstance()
                    fechaSeleccionada?.let { cal.timeInMillis = it }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
                            onFechaSeleccionada(cal.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )
    }
}