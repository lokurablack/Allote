package com.example.allote.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists"
)
data class Checklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Checklist::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("checklistId")]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "checklistId") val checklistId: Int,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "isDone") val isDone: Boolean = false,
    @ColumnInfo(name = "position") val position: Int = 0
)


