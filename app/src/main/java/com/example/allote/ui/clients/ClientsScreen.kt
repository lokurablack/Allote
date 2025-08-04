package com.example.allote.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.allote.data.Client
import com.example.allote.ui.components.ClientDialog
import com.example.allote.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    uiState: ClientsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSaveClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onClientClick: (Int) -> Unit,
    setFabAction: (() -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        setFabAction {
            clientToEdit = null
            showClientDialog = true
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                label = { Text("Buscar clientes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.clients.isEmpty()) {
                EmptyState(
                    title = "No hay clientes",
                    subtitle = "Añade tu primer cliente usando el botón '+' en la barra inferior.",
                    icon = Icons.Default.People
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val clientsToShow = if (uiState.searchQuery.isBlank()) uiState.clients else uiState.filteredClients
                    if (uiState.searchQuery.isNotEmpty() && clientsToShow.isEmpty()) {
                        item {
                            Text(
                                text = "No se encontraron clientes para '${uiState.searchQuery}'",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(clientsToShow, key = { it.id }) { client ->
                            ClientListItem(
                                client = client,
                                onClick = { onClientClick(client.id) },
                                onEdit = {
                                    clientToEdit = client
                                    showClientDialog = true
                                },
                                onDelete = onDeleteClient
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClientDialog) {
        ClientDialog(
            client = clientToEdit,
            onDismissRequest = {
                showClientDialog = false
                clientToEdit = null
            },
            onSave = { client ->
                onSaveClient(client)
                showClientDialog = false
                clientToEdit = null
            }
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayuda: Gestión de Clientes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta pantalla muestra tu lista de clientes. Desde aquí puedes gestionar toda su información.")
                Text("• Añadir: Usa el botón flotante (+) para registrar un nuevo cliente.")
                Text("• Ver detalles: Pulsa sobre un cliente para ver su panel de administración, trabajos y contabilidad.")
                Text("• Editar/Eliminar: Usa los íconos de lápiz y papelera en cada cliente para modificar su información o eliminarlo.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
fun ClientListItem(
    client: Client,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Client) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Está seguro de eliminar al cliente ${client.name} ${client.lastname}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(client); showDeleteDialog = false }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Cliente",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${client.name} ${client.lastname}", style = MaterialTheme.typography.titleMedium)
                if (!client.phone.isNullOrBlank()) {
                    Text(
                        text = client.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar Cliente") }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    "Eliminar Cliente",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
