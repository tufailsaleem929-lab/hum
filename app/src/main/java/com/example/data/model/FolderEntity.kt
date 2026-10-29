package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#8B5CF6",
    val iconName: String = "folder",
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
