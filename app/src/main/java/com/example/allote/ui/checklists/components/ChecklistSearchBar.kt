package com.example.allote.ui.checklists.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    PROGRESS_ASC,
    PROGRESS_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    showCompletedOnly: Boolean,
    onShowCompletedOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Search Field
            Box(modifier = Modifier.weight(1f)) {
                if (!isSearchActive && searchQuery.isEmpty()) {
                    Text(
                        text = "Buscar en checklists...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            
            // Clear button
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Sort button
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nombre (A-Z)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.SortByAlpha, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.NAME_ASC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nombre (Z-A)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.SortByAlpha, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.NAME_DESC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Fecha (Reciente)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.DATE_DESC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Fecha (Antigua)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.DATE_ASC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Progreso (Mayor)") },
                        onClick = {
                            onSortOptionChange(SortOption.PROGRESS_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.PROGRESS_DESC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Progreso (Menor)") },
                        onClick = {
                            onSortOptionChange(SortOption.PROGRESS_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.TrendingDown, contentDescription = null)
                        },
                        trailingIcon = {
                            if (sortOption == SortOption.PROGRESS_ASC) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
            
            // Filter button
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Badge(
                        containerColor = if (showCompletedOnly) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrar",
                            tint = if (showCompletedOnly) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Solo completadas")
                                Switch(
                                    checked = showCompletedOnly,
                                    onCheckedChange = onShowCompletedOnlyChange
                                )
                            }
                        },
                        onClick = {
                            onShowCompletedOnlyChange(!showCompletedOnly)
                        }
                    )
                }
            }
        }
    }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }
}