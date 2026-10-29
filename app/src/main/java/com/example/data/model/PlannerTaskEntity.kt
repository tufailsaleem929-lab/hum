package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_tasks")
data class PlannerTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long? = null,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false,
    val dueDateEpochDay: Long? = null, // Days since Unix epoch
    val dueTimeText: String? = null,
    val isRecurring: Boolean = false,
    val recurringPeriod: String = "", // DAILY, WEEKLY
    val createdAt: Long = System.currentTimeMillis()
)
