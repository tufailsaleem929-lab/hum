package com.example.data.repository

import android.content.Context
import com.example.data.local.FolderDao
import com.example.data.local.NoteDao
import com.example.data.local.PlannerTaskDao
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.PlannerTaskEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class NoteFlowRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val plannerTaskDao: PlannerTaskDao,
    private val context: Context
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val pinnedNotes: Flow<List<NoteEntity>> = noteDao.getPinnedNotes()
    val favoriteNotes: Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashNotes: Flow<List<NoteEntity>> = noteDao.getTrashNotes()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val plannerTasks: Flow<List<PlannerTaskEntity>> = plannerTaskDao.getAllTasks()

    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdImmediate(id: Long): NoteEntity? = noteDao.getNoteByIdImmediate(id)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNotesByTag(tag: String): Flow<List<NoteEntity>> = noteDao.getNotesByTag(tag)

    suspend fun saveNote(note: NoteEntity): Long {
        return if (note.id == 0L) {
            noteDao.insertNote(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun duplicateNote(note: NoteEntity): Long {
        val duplicate = note.copy(
            id = 0,
            title = "${note.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return noteDao.insertNote(duplicate)
    }

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavorite(note: NoteEntity) {
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchive(note: NoteEntity) {
        noteDao.updateNote(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(noteId: Long) {
        noteDao.moveToTrash(noteId, System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(noteId: Long) {
        noteDao.restoreFromTrash(noteId)
    }

    suspend fun deletePermanently(noteId: Long) {
        noteDao.deletePermanently(noteId)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    // Folders
    suspend fun createFolder(name: String, colorHex: String, iconName: String = "folder", parentId: Long? = null): Long {
        return folderDao.insertFolder(
            FolderEntity(name = name, colorHex = colorHex, iconName = iconName, parentId = parentId)
        )
    }

    suspend fun deleteFolder(folderId: Long) {
        folderDao.deleteFolder(folderId)
    }

    // Planner Tasks
    fun getTasksForDate(epochDay: Long): Flow<List<PlannerTaskEntity>> = plannerTaskDao.getTasksForDate(epochDay)

    suspend fun saveTask(task: PlannerTaskEntity): Long {
        return if (task.id == 0L) {
            plannerTaskDao.insertTask(task)
        } else {
            plannerTaskDao.updateTask(task)
            task.id
        }
    }

    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        plannerTaskDao.toggleTaskCompletion(taskId, isCompleted)
    }

    suspend fun deleteTask(taskId: Long) {
        plannerTaskDao.deleteTask(taskId)
    }

    // Search History Storage
    private val prefs = context.getSharedPreferences("noteflow_prefs", Context.MODE_PRIVATE)

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString("recent_searches", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        val existing = getRecentSearches().toMutableList()
        existing.remove(query)
        existing.add(0, query)
        val limited = existing.take(8).joinToString("|||")
        prefs.edit().putString("recent_searches", limited).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches").apply()
    }

    // Backup & Restore
    suspend fun exportAllDataToJson(): String {
        val notes = noteDao.getAllNotesRaw()
        val folders = folderDao.getAllFoldersRaw()

        val rootObj = JSONObject()
        rootObj.put("version", 1)
        rootObj.put("exportedAt", System.currentTimeMillis())

        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("tags", n.tags)
            obj.put("colorId", n.colorId)
            obj.put("isPinned", n.isPinned)
            obj.put("isFavorite", n.isFavorite)
            obj.put("isArchived", n.isArchived)
            obj.put("summaryText", n.summaryText ?: "")
            obj.put("transcriptionText", n.transcriptionText ?: "")
            notesArray.put(obj)
        }
        rootObj.put("notes", notesArray)

        val foldersArray = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("name", f.name)
            obj.put("colorHex", f.colorHex)
            obj.put("iconName", f.iconName)
            foldersArray.put(obj)
        }
        rootObj.put("folders", foldersArray)

        return rootObj.toString(2)
    }

    suspend fun restoreDataFromJson(jsonStr: String): Int {
        val rootObj = JSONObject(jsonStr)
        val notesArray = rootObj.optJSONArray("notes") ?: JSONArray()
        val importedNotes = mutableListOf<NoteEntity>()

        for (i in 0 until notesArray.length()) {
            val obj = notesArray.getJSONObject(i)
            importedNotes.add(
                NoteEntity(
                    title = obj.optString("title", "Imported Note"),
                    content = obj.optString("content", ""),
                    tags = obj.optString("tags", ""),
                    colorId = obj.optString("colorId", "default"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    summaryText = obj.optString("summaryText", "").ifBlank { null },
                    transcriptionText = obj.optString("transcriptionText", "").ifBlank { null }
                )
            )
        }
        if (importedNotes.isNotEmpty()) {
            noteDao.insertAll(importedNotes)
        }
        return importedNotes.size
    }
}
