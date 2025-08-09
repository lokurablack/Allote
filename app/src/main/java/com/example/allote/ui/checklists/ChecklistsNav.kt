package com.example.allote.ui.checklists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChecklistsRoute(
    setFabAction: (() -> Unit) -> Unit
) {
    val viewModel: ChecklistsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    ChecklistsScreen(
        uiState = uiState,
        onCreateChecklist = viewModel::createChecklist,
        onRenameChecklist = viewModel::renameChecklist,
        onDeleteChecklist = viewModel::deleteChecklist,
        onSelectChecklist = viewModel::selectChecklist,
        onAddItem = viewModel::addItem,
        onToggleItem = viewModel::toggleItem,
        onUpdateItemText = viewModel::updateItemText,
        onDeleteItem = viewModel::deleteItem,
        onReorderItem = viewModel::reorderItems,
        onShowBottomSheet = viewModel::showBottomSheet,
        onHideBottomSheet = viewModel::hideBottomSheet,
        onStartEditingItem = viewModel::startEditingItem,
        onCancelEditingItem = viewModel::cancelEditingItem,
        setFabAction = setFabAction
    )
}


