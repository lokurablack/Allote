package com.example.allote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun LocationPickerDialog(
    initialLat: Double,
    initialLng: Double,
    isEditing: Boolean,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val initialLocation = LatLng(initialLat, initialLng)
    var selectedLatLng by remember {
        mutableStateOf<LatLng?>(if (isEditing) initialLocation else null)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(initialLocation, if (isEditing) 16f else 10f, 0f, 0f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Ubicación") },
        text = {
            Column {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID),
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                    }
                ) {
                    selectedLatLng?.let {
                        Marker(state = MarkerState(position = it), title = "Ubicación seleccionada")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedLatLng?.let {
                        onConfirm(it.latitude, it.longitude)
                    }
                },
                enabled = selectedLatLng != null
            ) {
                Text("Confirmar ubicación")
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
fun LocationViewerDialog(
    lat: Double,
    lng: Double,
    description: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val jobLocation = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(jobLocation, 16f, 0f, 0f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubicación del Trabajo") },
        text = {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.HYBRID)
            ) {
                Marker(
                    state = MarkerState(position = jobLocation),
                    title = description
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Editar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
