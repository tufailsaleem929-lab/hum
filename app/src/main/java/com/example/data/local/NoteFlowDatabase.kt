package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.PlannerTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteEntity::class, FolderEntity::class, PlannerTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoteFlowDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun plannerTaskDao(): PlannerTaskDao

    companion object {
        @Volatile
        private var INSTANCE: NoteFlowDatabase? = null

        fun getDatabase(context: Context): NoteFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteFlowDatabase::class.java,
                    "noteflow_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default starter content
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getDatabase(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: NoteFlowDatabase) {
            val personalFolderId = db.folderDao().insertFolder(
                FolderEntity(name = "Personal", colorHex = "#EC4899", iconName = "person")
            )
            val workFolderId = db.folderDao().insertFolder(
                FolderEntity(name = "Work & Projects", colorHex = "#06B6D4", iconName = "work")
            )
            val ideasFolderId = db.folderDao().insertFolder(
                FolderEntity(name = "Creative Ideas", colorHex = "#8B5CF6", iconName = "lightbulb")
            )

            // Sample Welcome Note
            db.noteDao().insertNote(
                NoteEntity(
                    title = "✨ Welcome to NoteFlow",
                    content = """
# Welcome to NoteFlow!
Your all-in-one AI note-taking, transcription, and productivity workspace.

### 🚀 Key Features:
- **Speech-to-Text Transcription**: Tap the microphone button in any note to transcribe speech in real-time.
- **AI Writing & Summarization**: Tap the Sparkle icon to summarize, rephrase tone, fix grammar, or extract key action points.
- **Vibrant Dark-Themed Dashboard**: Color-code your notes, pin essential thoughts, and toggle between grid and list views.
- **Productivity & Planner**: Organize daily tasks with priorities and deadlines.
- **Security & Privacy**: Lock confidential notes with a secure PIN.
- **Offline & Cross-Device Ready**: Full offline database with JSON backup and instant Markdown export.
                    """.trimIndent(),
                    folderId = personalFolderId,
                    tags = "Welcome, Guide, NoteFlow",
                    colorId = "violet",
                    isPinned = true,
                    isFavorite = true,
                    summaryText = "NoteFlow guide covering real-time voice transcription, AI assistant features, dark-themed dashboard, and security."
                )
            )

            // Sample Meeting Minutes Note
            db.noteDao().insertNote(
                NoteEntity(
                    title = "🎙️ Product Strategy & AI Sync",
                    content = """
## Executive Discussion
- Reviewed the Q4 AI-powered note taking milestones.
- Benchmarked on-device speech transcription and summarized notes.

### Decisions Made:
1. Prioritize low-latency voice dictation.
2. Enable multi-tone rewrites (Professional, Casual, Concise).
3. Implement 30-day trash protection.
                    """.trimIndent(),
                    folderId = workFolderId,
                    tags = "Strategy, Meeting, AI",
                    colorId = "ocean",
                    isPinned = true,
                    summaryText = "Q4 product sync: prioritized voice dictation latency, AI multi-tone rewrite, and 30-day trash recovery."
                )
            )

            // Sample Planner Tasks
            val todayEpochDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            db.plannerTaskDao().insertTask(
                PlannerTaskEntity(
                    title = "Review NoteFlow AI summary for Q4 sync",
                    priority = "HIGH",
                    isCompleted = false,
                    dueDateEpochDay = todayEpochDay,
                    dueTimeText = "10:30 AM"
                )
            )
            db.plannerTaskDao().insertTask(
                PlannerTaskEntity(
                    title = "Test voice transcription note recording",
                    priority = "MEDIUM",
                    isCompleted = true,
                    dueDateEpochDay = todayEpochDay,
                    dueTimeText = "02:00 PM"
                )
            )
        }
    }
}
