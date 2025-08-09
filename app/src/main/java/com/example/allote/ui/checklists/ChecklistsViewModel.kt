package com.example.allote.ui.checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allote.data.Checklist
import com.example.allote.data.ChecklistItem
import com.example.allote.data.ChecklistsRepository
import com.example.allote.ui.checklists.components.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChecklistsUiState(
    val checklists: List<Checklist> = emptyList(),
    val selectedChecklist: Checklist? = null,
    val items: List<ChecklistItem> = emptyList(),
    val checklistProgress: Map<Int, Pair<Int, Int>> = emptyMap(), // checklistId to (completed, total)
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_DESC,
    val showCompletedOnly: Boolean = false,
    val showBottomSheet: Boolean = false,
    val editingItem: ChecklistItem? = null
)

@HiltViewModel
class ChecklistsViewModel @Inject constructor(
    private val repository: ChecklistsRepository
) : ViewModel() {

    private val selectedChecklistId = MutableStateFlow<Int?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    private val _showCompletedOnly = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(ChecklistsUiState(isLoading = true))
    val uiState: StateFlow<ChecklistsUiState> = _uiState.asStateFlow()

    private val checklistsFlow = repository.getAllChecklists()

    private val itemsFlow = selectedChecklistId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getItemsForChecklist(id)
    }

    init {
        // Combine all flows to update UI state
        viewModelScope.launch {
            combine(
                checklistsFlow,
                itemsFlow
            ) { checklists, items ->
                val checklistProgress = calculateChecklistProgress(checklists)
                val selected = checklists.firstOrNull { it.id == selectedChecklistId.value }
                
                ChecklistsUiState(
                    checklists = checklists,
                    selectedChecklist = selected,
                    items = items,
                    checklistProgress = checklistProgress,
                    isLoading = false,
                    searchQuery = "",
                    sortOption = SortOption.DATE_DESC,
                    showCompletedOnly = false,
                    showBottomSheet = _uiState.value.showBottomSheet,
                    editingItem = _uiState.value.editingItem
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        // Auto-select first checklist if none selected
        viewModelScope.launch {
            checklistsFlow.collect { lists ->
                if (selectedChecklistId.value == null && lists.isNotEmpty()) {
                    selectedChecklistId.value = lists.first().id
                }
            }
        }
    }

    private suspend fun calculateChecklistProgress(checklists: List<Checklist>): Map<Int, Pair<Int, Int>> {
        return checklists.associate { checklist ->
            val items = repository.getItemsForChecklist(checklist.id).first()
            val completed = items.count { it.isDone }
            checklist.id to (completed to items.size)
        }
    }

    // Eliminado: búsqueda y ordenamiento

    fun selectChecklist(id: Int) {
        selectedChecklistId.value = id
        _uiState.value = _uiState.value.copy(showBottomSheet = false)
    }

    fun createChecklist(title: String) = viewModelScope.launch {
        val newId = repository.createChecklist(title)
        selectedChecklistId.value = newId
    }

    fun renameChecklist(checklist: Checklist, newTitle: String) = viewModelScope.launch {
        repository.renameChecklist(checklist, newTitle)
    }

    fun deleteChecklist(checklist: Checklist) = viewModelScope.launch {
        repository.deleteChecklist(checklist)
        if (checklist.id == selectedChecklistId.value) {
            selectedChecklistId.value = null
        }
    }

    fun addItem(text: String) = viewModelScope.launch {
        val checklist = _uiState.value.selectedChecklist ?: return@launch
        val position = _uiState.value.items.size
        repository.addItem(checklist.id, text, position)
    }

    fun toggleItem(item: ChecklistItem) = viewModelScope.launch {
        repository.toggleItem(item)
    }

    fun updateItemText(item: ChecklistItem, newText: String) = viewModelScope.launch {
        repository.updateItemText(item, newText)
        _uiState.value = _uiState.value.copy(editingItem = null)
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch {
        repository.deleteItem(item)
    }

    fun reorderItems(from: Int, to: Int) = viewModelScope.launch {
        val items = _uiState.value.items.toMutableList()
        val item = items.removeAt(from)
        items.add(to, item)
        
        val newOrder = items.map { it.id }
        val checklist = _uiState.value.selectedChecklist ?: return@launch
        repository.reorderItems(checklist.id, newOrder)
        // Force refresh of items in UI state by re-emitting
        _uiState.value = _uiState.value.copy(items = items)
    }

    // Eliminado: manejo de búsqueda/orden/completos

    fun showBottomSheet() {
        _uiState.value = _uiState.value.copy(showBottomSheet = true)
    }

    fun hideBottomSheet() {
        _uiState.value = _uiState.value.copy(showBottomSheet = false)
    }

    fun startEditingItem(item: ChecklistItem) {
        _uiState.value = _uiState.value.copy(editingItem = item)
    }

    fun cancelEditingItem() {
        _uiState.value = _uiState.value.copy(editingItem = null)
    }
}


