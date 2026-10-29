package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.NoteEntity
import java.io.File
import java.io.FileWriter

object ExportHelper {
    fun shareNote(context: Context, note: NoteEntity) {
        val shareText = buildString {
            appendLine(note.title)
            if (note.tags.isNotBlank()) appendLine("Tags: ${note.tags}")
            if (!note.summaryText.isNullOrBlank()) {
                appendLine()
                appendLine("--- AI Summary ---")
                appendLine(note.summaryText)
                appendLine("------------------")
            }
            appendLine()
            appendLine(note.content)
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, note.title)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Note via NoteFlow")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun exportToMarkdown(context: Context, note: NoteEntity): String {
        val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(context.cacheDir, "${sanitizedTitle}_${System.currentTimeMillis()}.md")
        val mdContent = buildString {
            appendLine("# ${note.title}")
            appendLine()
            appendLine("> **Exported from NoteFlow** | ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.updatedAt))}")
            if (note.tags.isNotBlank()) appendLine("> **Tags:** ${note.tags}")
            if (!note.summaryText.isNullOrBlank()) {
                appendLine()
                appendLine("### 📌 AI Summary")
                appendLine(note.summaryText)
            }
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(note.content)
        }
        FileWriter(file).use { it.write(mdContent) }
        return file.absolutePath
    }

    fun exportToTxt(context: Context, note: NoteEntity): String {
        val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(context.cacheDir, "${sanitizedTitle}_${System.currentTimeMillis()}.txt")
        val txtContent = buildString {
            appendLine(note.title)
            appendLine("=".repeat(note.title.length.coerceAtLeast(10)))
            appendLine(note.content)
        }
        FileWriter(file).use { it.write(txtContent) }
        return file.absolutePath
    }
}
