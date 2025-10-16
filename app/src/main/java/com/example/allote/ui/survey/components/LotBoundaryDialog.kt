package com.example.allote.ui.survey.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.allote.ui.survey.BoundaryPoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun LotBoundaryDialog(
    initialPoints: List<BoundaryPoint>,
    onConfirm: (List<BoundaryPoint>) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var pointList by remember {
        mutableStateOf(initialPoints.map { LatLng(it.latitude, it.longitude) })
    }

    val defaultPosition = pointList.takeIf { it.isNotEmpty() }?.let { points ->
        val latAverage = points.map { it.latitude }.average()
        val lngAverage = points.map { it.longitude }.average()
        LatLng(latAverage, lngAverage)
    } ?: LatLng(-31.436, -63.548)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(defaultPosition, 15f, 0f, 0f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delimitar perímetro del lote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Toca el mapa para agregar vértices. Puedes deshacer el último punto o limpiar toda la selección.",
                    style = MaterialTheme.typography.bodySmall
                )

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID),
                    onMapClick = { latLng -> pointList = pointList + latLng }
                ) {
                    if (pointList.size >= 3) {
                        Polygon(
                            points = pointList,
                            strokeColor = MaterialTheme.colorScheme.primary,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    }
                    pointList.forEachIndexed { index, latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "Punto ${index + 1}"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (pointList.isNotEmpty()) {
                                pointList = pointList.dropLast(1)
                            }
                        },
                        enabled = pointList.isNotEmpty()
                    ) {
                        Text("Deshacer último")
                    }
                    TextButton(
                        onClick = {
                            pointList = emptyList()
                            onClear()
                        },
                        enabled = pointList.isNotEmpty()
                    ) {
                        Text("Limpiar todo")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pointList.size >= 3) {
                        onConfirm(pointList.map { BoundaryPoint(it.latitude, it.longitude) })
                    }
                },
                enabled = pointList.size >= 3
            ) {
                Text("Confirmar perímetro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
