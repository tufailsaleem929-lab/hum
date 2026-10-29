package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val tags: String = "",
    val colorId: String = "default",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val isLocked: Boolean = false,
    val pinCode: String? = null,
    val audioPath: String? = null,
    val audioDurationSec: Int = 0,
    val transcriptionText: String? = null,
    val summaryText: String? = null,
    val imageUri: String? = null,
    val sketchData: String? = null,
    val checklistJson: String? = null,
    val versionHistoryJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

data class ChecklistItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false
)

data class NoteVersionSnapshot(
    val timestamp: Long,
    val title: String,
    val content: String,
    val summary: String? = null
)
