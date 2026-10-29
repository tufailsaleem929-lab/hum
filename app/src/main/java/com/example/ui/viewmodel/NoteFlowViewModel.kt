package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiNoteAssistant
import com.example.ai.AiTone
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.VoiceTranscriptionManager
import com.example.data.local.NoteFlowDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NOTE_TEMPLATES
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTemplate
import com.example.data.model.PlannerTaskEntity
import com.example.data.repository.NoteFlowRepository
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class NavigationScreen {
    DASHBOARD,
    NOTE_EDITOR,
    PLANNER,
    FOLDERS,
    ARCHIVE,
    TRASH,
    SETTINGS
}

enum class SortOrder {
    LAST_MODIFIED,
    CREATION_DATE,
    ALPHABETICAL
}

class NoteFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val db = NoteFlowDatabase.getDatabase(application)
    val repository = NoteFlowRepository(
        noteDao = db.noteDao(),
        folderDao = db.folderDao(),
        plannerTaskDao = db.plannerTaskDao(),
        context = application
    )

    val securityManager = SecurityManager(application)
    val billingManager = com.example.billing.BillingManager(application)
    val aiAssistant = AiNoteAssistant()
    val audioRecorder = AudioRecorderManager(application)
    val audioPlayer = AudioPlayerManager()
    val voiceTranscription = VoiceTranscriptionManager(application)
    private val appPrefs = application.getSharedPreferences("noteflow_user_prefs", android.content.Context.MODE_PRIVATE)

    fun canUseAi(): Boolean {
        if (billingManager.isProUser.value) return true
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastDate = appPrefs.getString("ai_usage_date", "")
        val count = if (lastDate == today) appPrefs.getInt("ai_usage_count", 0) else 0
        return count < 3
    }

    fun getRemainingDailyAiRequests(): Int {
        if (billingManager.isProUser.value) return 999
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastDate = appPrefs.getString("ai_usage_date", "")
        val count = if (lastDate == today) appPrefs.getInt("ai_usage_count", 0) else 0
        return (3 - count).coerceAtLeast(0)
    }

    private fun consumeAiQuota(): Boolean {
        if (billingManager.isProUser.value) return true
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastDate = appPrefs.getString("ai_usage_date", "")
        val count = if (lastDate == today) appPrefs.getInt("ai_usage_count", 0) else 0
        if (count >= 3) return false
        appPrefs.edit().putString("ai_usage_date", today).putInt("ai_usage_count", count + 1).apply()
        return true
    }

    fun canCreateFolder(): Boolean {
        return billingManager.isProUser.value || folders.value.size < 2
    }

    // Screen navigation
    private val _currentScreen = MutableStateFlow(NavigationScreen.DASHBOARD)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    // Note editor active note
    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    // Layout view: Grid vs List
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Sort order
    private val _sortOrder = MutableStateFlow(SortOrder.LAST_MODIFIED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Selected folder filter (null = all)
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    // Selected tag filter
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Recent searches
    private val _recentSearches = MutableStateFlow(repository.getRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Cloud sync simulation status
    private val _syncStatus = MutableStateFlow("All notes synced to cloud")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // App Lock PIN check state
    private val _isAppUnlocked = MutableStateFlow(!securityManager.isAppLockEnabled)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // AI operation state
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    // Raw sources from repository
    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plannerTasks: StateFlow<List<PlannerTaskEntity>> = repository.plannerTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<NoteEntity>> = repository.trashNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Active Notes
    val activeNotes: StateFlow<List<NoteEntity>> = combine(
        repository.activeNotes,
        _selectedFolderId,
        _selectedTag,
        _searchQuery,
        _sortOrder
    ) { notes, folderId, tag, query, sort ->
        var list = notes

        // Folder filter
        if (folderId != null) {
            list = list.filter { it.folderId == folderId }
        }

        // Tag filter
        if (!tag.isNullOrBlank()) {
            list = list.filter { it.tags.contains(tag, ignoreCase = true) }
        }

        // Search query filter
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true) ||
                (it.summaryText?.contains(query, ignoreCase = true) == true)
            }
        }

        // Sort order
        when (sort) {
            SortOrder.LAST_MODIFIED -> list.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
            SortOrder.CREATION_DATE -> list.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAt })
            SortOrder.ALPHABETICAL -> list.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase() })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation controls
    fun navigateTo(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun openNote(note: NoteEntity) {
        _editingNote.value = note
        _currentScreen.value = NavigationScreen.NOTE_EDITOR
    }

    fun createNewNote(folderId: Long? = _selectedFolderId.value) {
        val newNote = NoteEntity(
            title = "",
            content = "",
            folderId = folderId,
            tags = "",
            colorId = "default"
        )
        _editingNote.value = newNote
        _currentScreen.value = NavigationScreen.NOTE_EDITOR
    }

    fun createFromTemplate(template: NoteTemplate) {
        val newNote = NoteEntity(
            title = template.initialTitle,
            content = template.defaultContent,
            tags = template.defaultTags,
            colorId = template.defaultColor,
            folderId = _selectedFolderId.value
        )
        _editingNote.value = newNote
        _currentScreen.value = NavigationScreen.NOTE_EDITOR
    }

    fun closeEditor() {
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        voiceTranscription.stopListening()
        _editingNote.value = null
        _currentScreen.value = NavigationScreen.DASHBOARD
    }

    // Note persistence
    fun saveCurrentNote(title: String, content: String, tags: String, colorId: String, folderId: Long?) {
        val current = _editingNote.value ?: return
        viewModelScope.launch {
            // Version history snapshot
            val history = parseVersionHistory(current.versionHistoryJson).toMutableList()
            if (current.content.isNotBlank() && current.content != content) {
                history.add(
                    com.example.data.model.NoteVersionSnapshot(
                        timestamp = System.currentTimeMillis(),
                        title = current.title,
                        content = current.content,
                        summary = current.summaryText
                    )
                )
            }

            val updated = current.copy(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                tags = tags,
                colorId = colorId,
                folderId = folderId,
                versionHistoryJson = serializeVersionHistory(history.takeLast(10)),
                updatedAt = System.currentTimeMillis(),
                syncStatus = "SYNCED"
            )
            val id = repository.saveNote(updated)
            _editingNote.value = updated.copy(id = id)
        }
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 3) {
            repository.addRecentSearch(query)
            _recentSearches.value = repository.getRecentSearches()
        }
    }

    fun clearRecentSearches() {
        repository.clearRecentSearches()
        _recentSearches.value = emptyList()
    }

    fun togglePin(note: NoteEntity) = viewModelScope.launch { repository.togglePin(note) }
    fun toggleFavorite(note: NoteEntity) = viewModelScope.launch { repository.toggleFavorite(note) }
    fun toggleArchive(note: NoteEntity) = viewModelScope.launch { repository.toggleArchive(note) }
    fun moveToTrash(noteId: Long) = viewModelScope.launch { repository.moveToTrash(noteId) }
    fun restoreFromTrash(noteId: Long) = viewModelScope.launch { repository.restoreFromTrash(noteId) }
    fun deletePermanently(noteId: Long) = viewModelScope.launch { repository.deletePermanently(noteId) }
    fun emptyTrash() = viewModelScope.launch { repository.emptyTrash() }
    fun duplicateNote(note: NoteEntity) = viewModelScope.launch { repository.duplicateNote(note) }

    // Cloud Sync simulation
    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Connecting to cloud sync..."
            kotlinx.coroutines.delay(1200)
            _syncStatus.value = "Encrypting and verifying hashes..."
            kotlinx.coroutines.delay(800)
            _syncStatus.value = "Cloud sync complete: ${activeNotes.value.size} notes synchronized"
            _isSyncing.value = false
        }
    }

    // App Lock PIN unlock
    fun unlockApp(pin: String): Boolean {
        return if (securityManager.verifyPin(pin)) {
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (securityManager.isAppLockEnabled) {
            _isAppUnlocked.value = false
        }
    }

    // AI Actions on currently editing note
    fun aiSummarize(content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is summarizing your note..."
            val result = aiAssistant.summarize(content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiRewrite(content: String, tone: AiTone, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is rewriting in ${tone.title} tone..."
            val result = aiAssistant.rewriteTone(content, tone)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiFixGrammar(content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is polishing grammar & spelling..."
            val result = aiAssistant.fixGrammarAndSpelling(content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiExtractKeyPoints(content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is extracting key action items..."
            val result = aiAssistant.extractKeyPoints(content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiGenerateTitle(content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is generating title..."
            val result = aiAssistant.generateTitle(content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiExpandWriting(content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI writing assistant is expanding..."
            val result = aiAssistant.expandWriting(content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiTranslate(content: String, targetLang: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "Translating note into $targetLang..."
            val result = aiAssistant.translate(content, targetLang)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiGenerateFromPrompt(prompt: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "Generating note from prompt..."
            val result = aiAssistant.generateFromPrompt(prompt)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiAskNote(question: String, content: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "AI is analyzing your note..."
            val result = aiAssistant.askNotes(question, content)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    fun aiMeetingMinutes(text: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (!consumeAiQuota()) {
                _aiStatusMessage.value = "Daily free AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI."
                onDone("Daily free AI limit reached. Upgrade to NoteFlow Pro for unlimited AI Superpowers.")
                return@launch
            }
            _isAiLoading.value = true
            _aiStatusMessage.value = "Converting transcript to Meeting Minutes..."
            val result = aiAssistant.generateMeetingMinutes(text)
            _isAiLoading.value = false
            _aiStatusMessage.value = null
            onDone(result)
        }
    }

    // Planner task operations
    fun toggleTask(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch { repository.toggleTaskCompletion(taskId, isCompleted) }
    }

    fun addTask(title: String, priority: String, epochDay: Long?, dueTime: String?) {
        viewModelScope.launch {
            repository.saveTask(
                PlannerTaskEntity(
                    title = title,
                    priority = priority,
                    dueDateEpochDay = epochDay,
                    dueTimeText = dueTime
                )
            )
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    // Folders
    fun createFolder(name: String, colorHex: String, parentId: Long? = null) {
        viewModelScope.launch { repository.createFolder(name, colorHex, parentId = parentId) }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch { repository.deleteFolder(folderId) }
    }

    // JSON backup / restore
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllDataToJson()
            onResult(json)
        }
    }

    fun restoreBackup(jsonStr: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.restoreDataFromJson(jsonStr)
            onResult(count)
        }
    }

    // Serialization helpers
    private fun parseVersionHistory(json: String?): List<com.example.data.model.NoteVersionSnapshot> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<com.example.data.model.NoteVersionSnapshot>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.data.model.NoteVersionSnapshot(
                        timestamp = obj.optLong("timestamp"),
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        summary = obj.optString("summary").ifBlank { null }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeVersionHistory(list: List<com.example.data.model.NoteVersionSnapshot>): String {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject().apply {
                put("timestamp", it.timestamp)
                put("title", it.title)
                put("content", it.content)
                put("summary", it.summary ?: "")
            }
            array.put(obj)
        }
        return array.toString()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        voiceTranscription.destroy()
    }
}
