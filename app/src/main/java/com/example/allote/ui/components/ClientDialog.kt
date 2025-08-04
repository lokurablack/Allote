package com.example.allote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.allote.data.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDialog(
    client: Client? = null,
    onDismissRequest: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember(client) { mutableStateOf(client?.name ?: "") }
    var lastName by remember(client) { mutableStateOf(client?.lastname ?: "") }
    var phone by remember(client) { mutableStateOf(client?.phone ?: "") }
    var email by remember(client) { mutableStateOf(client?.email ?: "") }
    var cuit by remember(client) { mutableStateOf(client?.cuit ?: "") }
    var localidad by remember(client) { mutableStateOf(client?.localidad ?: "") }
    var direccion by remember(client) { mutableStateOf(client?.direccion ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    val isFormValid = name.isNotBlank() && lastName.isNotBlank()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (client == null) "Crear Cliente" else "Editar Cliente") },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Guardar") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    onClick = {
                        nameError = name.isBlank()
                        lastNameError = lastName.isBlank()
                        if (isFormValid) {
                            val newClient = Client(
                                id = client?.id ?: 0,
                                name = name.trim(),
                                lastname = lastName.trim(),
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                cuit = cuit.trim().ifBlank { null },
                                localidad = localidad.trim().ifBlank { null },
                                direccion = direccion.trim().ifBlank { null }
                            )
                            onSave(newClient)
                        }
                    },
                    expanded = true
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; nameError = it.isBlank() },
                                label = { Text("Nombre*") },
                                isError = nameError,
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                            )
                            if (nameError) {
                                Text("Requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it; lastNameError = it.isBlank() },
                                label = { Text("Apellido*") },
                                isError = lastNameError,
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                            )
                            if (lastNameError) {
                                Text("Requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = cuit,
                        onValueChange = { cuit = it },
                        label = { Text("CUIT") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = localidad,
                        onValueChange = { localidad = it },
                        label = { Text("Localidad") },
                        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
