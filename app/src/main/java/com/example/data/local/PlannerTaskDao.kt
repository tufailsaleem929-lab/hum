package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlannerTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerTaskDao {
    @Query("SELECT * FROM planner_tasks ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE dueDateEpochDay = :epochDay ORDER BY isCompleted ASC, priority DESC")
    fun getTasksForDate(epochDay: Long): Flow<List<PlannerTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTaskEntity): Long

    @Update
    suspend fun updateTask(task: PlannerTaskEntity)

    @Query("UPDATE planner_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)
}
