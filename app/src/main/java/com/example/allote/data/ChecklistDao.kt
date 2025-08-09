package com.example.allote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist): Long

    @Update
    suspend fun updateChecklist(checklist: Checklist)

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    @Query("SELECT * FROM checklists ORDER BY createdAt DESC")
    fun getAllChecklists(): Flow<List<Checklist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItem): Long

    @Update
    suspend fun updateItem(item: ChecklistItem)

    @Delete
    suspend fun deleteItem(item: ChecklistItem)

    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY position ASC")
    fun getItemsForChecklist(checklistId: Int): Flow<List<ChecklistItem>>

    @Transaction
    suspend fun reorderItems(checklistId: Int, orderedItemIds: List<Int>) {
        orderedItemIds.forEachIndexed { index, itemId ->
            updateItemPosition(itemId, index)
        }
    }

    @Query("UPDATE checklist_items SET position = :position WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: Int, position: Int)
}


