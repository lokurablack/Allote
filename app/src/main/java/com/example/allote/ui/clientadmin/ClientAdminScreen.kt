package com.example.allote.ui.clientadmin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.allote.data.Client

// === FIRMA CORREGIDA ===
// Ahora solo recibe el objeto 'Client' y las lambdas para la navegación
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAdministracionScreen(
    client: Client?,
    onNavigateToJobs: (Int) -> Unit,
    onNavigateToContabilidad: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = client?.let { "${it.name} ${it.lastname}" } ?: "Cargando...") })
        }
    ) { paddingValues ->
        // Si el cliente es nulo, mostramos una pantalla de carga
        if (client == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Cuando el cliente llega, mostramos la UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val imageUrl = "https://images.pexels.com/photos/3184304/pexels-photo-3184304.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=650&w=940"
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "Imagen de administración",
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))

                // La UI de los detalles se queda igual
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    if (client.name.isNotBlank()) Text("Nombre: ${client.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (client.lastname.isNotBlank()) Text("Apellido: ${client.lastname}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (!client.phone.isNullOrBlank()) Text("Teléfono: ${client.phone}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    // ... etc ...
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Seleccione la opción deseada para administrar al cliente.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 24.dp))

                // Los botones ahora llaman a las lambdas de evento
                Button(
                    onClick = { onNavigateToJobs(client.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Trabajos") }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onNavigateToContabilidad(client.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Contabilidad") }
            }
        }
    }
}