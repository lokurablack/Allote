package com.example.allote.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.allote.ui.components.EmptyState
import java.io.File

// === FIRMA CORREGIDA ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    onAddDocuments: (List<Uri>) -> Unit,
    onDeleteDocument: (DocumentoItem) -> Unit,
    onLoadThumbnail: suspend (DocumentoItem) -> Bitmap?
) {
    // El launcher ahora vive en el Composable que lo usa
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onAddDocuments(uris)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Documentos del Trabajo") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { documentPickerLauncher.launch(arrayOf("application/pdf", "image/jpeg")) }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Cargar Documentos")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.documents.isEmpty()) {
                EmptyState(
                    title = "Sin Documentos",
                    subtitle = "Aún no has adjuntado documentos o imágenes a este trabajo.",
                    icon = Icons.Default.FindInPage
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        DocumentListItem(
                            document = doc,
                            onDelete = { onDeleteDocument(doc) },
                            onLoadThumbnail = { onLoadThumbnail(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentListItem(
    document: DocumentoItem,
    onDelete: () -> Unit,
    onLoadThumbnail: suspend () -> Bitmap?
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumb by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(document.uri) {
        isLoadingThumb = true
        thumbnail = onLoadThumbnail()
        isLoadingThumb = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar archivo") },
            text = { Text("¿Está seguro que desea eliminar este archivo?") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { openDocument(context, document.uri) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                if (isLoadingThumb) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else if (thumbnail != null) {
                    Image(thumbnail!!.asImageBitmap(), "Thumbnail")
                } else {
                    Icon(
                        if (document.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                        "Icono de archivo", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(document.name, modifier = Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun openDocument(context: Context, uri: Uri) {
    val contentUri: Uri = try {
        if (uri.scheme == "file") {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(uri.path!!))
        } else {
            uri
        }
    } catch (_: Exception) {
        Toast.makeText(context, "No se puede generar URI para el archivo.", Toast.LENGTH_SHORT).show()
        return
    }

    val mimeType = if (uri.toString().endsWith(".pdf", true)) "application/pdf" else "image/jpeg"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No hay aplicación para abrir este archivo.", Toast.LENGTH_SHORT).show()
    }
}