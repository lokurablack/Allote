package com.example.allote.data

import kotlinx.coroutines.flow.Flow

class ChecklistsRepository(
    private val dao: ChecklistDao
) {
    fun getAllChecklists(): Flow<List<Checklist>> = dao.getAllChecklists()
    fun getItemsForChecklist(checklistId: Int): Flow<List<ChecklistItem>> = dao.getItemsForChecklist(checklistId)

    suspend fun createChecklist(title: String): Int = dao.insertChecklist(Checklist(title = title)).toInt()
    suspend fun renameChecklist(checklist: Checklist, newTitle: String) = dao.updateChecklist(checklist.copy(title = newTitle))
    suspend fun deleteChecklist(checklist: Checklist) = dao.deleteChecklist(checklist)

    suspend fun addItem(checklistId: Int, text: String, position: Int): Int =
        dao.insertItem(ChecklistItem(checklistId = checklistId, text = text, position = position)).toInt()

    suspend fun toggleItem(item: ChecklistItem) = dao.updateItem(item.copy(isDone = !item.isDone))
    suspend fun updateItemText(item: ChecklistItem, newText: String) = dao.updateItem(item.copy(text = newText))
    suspend fun deleteItem(item: ChecklistItem) = dao.deleteItem(item)
    suspend fun reorderItems(checklistId: Int, orderedItemIds: List<Int>) = dao.reorderItems(checklistId, orderedItemIds)
}


