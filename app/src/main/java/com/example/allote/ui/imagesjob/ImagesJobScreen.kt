package com.example.allote.ui.imagesjob

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.allote.data.ImageEntity
import com.example.allote.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesJobScreen(
    uiState: ImagesJobUiState,
    onSelectImages: (List<Uri>) -> Unit,
    onDeleteImage: (ImageEntity) -> Unit
) {
    val context = LocalContext.current
    val selectImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            onSelectImages(uris)
            Toast.makeText(context, "${uris.size} imágen(es) agregada(s)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Imágenes del Trabajo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = { selectImagesLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Seleccionar Imágenes")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.images.isEmpty()) { // <-- Cambiado a uiState.images
                EmptyState(
                    title = "Sin Imágenes",
                    subtitle = "Añade imágenes a este trabajo para llevar un registro visual.",
                    icon = Icons.Default.Image
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Más espacio
                ) {
                    items(uiState.images, key = { it.id }) { imageEntity ->
                        ImageRow(
                            image = imageEntity,
                            onDelete = { onDeleteImage(imageEntity) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Nuevo Composable para la fila de la imagen, con vista previa,
 * botón de borrar y acción de click.
 */
@Composable
private fun ImageRow(
    image: ImageEntity,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val imageUri = Uri.parse(image.imageUri)

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // --- ACCIÓN DE ABRIR IMAGEN ---
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(imageUri, "image/jpeg") // Asumimos que son jpeg por la compresión
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast
                            .makeText(context, "No se encontró una aplicación para abrir la imagen.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vista previa más pequeña
            Card(shape = MaterialTheme.shapes.small, modifier = Modifier.size(80.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context).data(imageUri).crossfade(true).build()
                    ),
                    contentDescription = "Vista previa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nombre del archivo (o un texto genérico)
            Text(
                text = "Imagen",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            // Botón de eliminar
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar imagen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}