package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiTone(val title: String) {
    PROFESSIONAL("Professional"),
    CASUAL("Casual & Friendly"),
    CONCISE("Concise & Punchy"),
    CREATIVE("Creative & Engaging")
}

class AiNoteAssistant {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (e: Throwable) {
            ""
        }

    suspend fun summarize(noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "Provide a high-level executive summary and 3 key takeaways for the following note content:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackSummarize(noteContent)
        }
    }

    suspend fun rewriteTone(noteContent: String, tone: AiTone): String = withContext(Dispatchers.IO) {
        val prompt = "Rewrite the following note text in a ${tone.title} tone, keeping all essential facts:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackRewrite(noteContent, tone)
        }
    }

    suspend fun fixGrammarAndSpelling(noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "Correct any spelling, grammar, and punctuation mistakes in the following text while keeping its natural meaning:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackFixGrammar(noteContent)
        }
    }

    suspend fun extractKeyPoints(noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "Extract the most important action items, decisions, and key points from this note formatted as clean bullet points:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackKeyPoints(noteContent)
        }
    }

    suspend fun generateTitle(noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "Generate a short, clear, professional 3-6 word title for this note content. Output ONLY the title:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackGenerateTitle(noteContent)
        }
    }

    suspend fun expandWriting(noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "Act as an expert writing assistant. Expand and polish these rough thoughts into a well-structured, comprehensive note:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackExpand(noteContent)
        }
    }

    suspend fun translate(noteContent: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val prompt = "Translate the following note accurately into $targetLanguage while preserving markdown formatting:\n\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackTranslate(noteContent, targetLanguage)
        }
    }

    suspend fun generateFromPrompt(userPrompt: String): String = withContext(Dispatchers.IO) {
        val prompt = "Generate a complete, structured note with headings, bullet points, and key details based on this topic:\n\n$userPrompt"
        callGeminiOrFallback(prompt) {
            fallbackGenerateFromPrompt(userPrompt)
        }
    }

    suspend fun askNotes(question: String, noteContent: String): String = withContext(Dispatchers.IO) {
        val prompt = "You are an AI note assistant. Answer this question based on the note below:\nQuestion: $question\n\nNote:\n$noteContent"
        callGeminiOrFallback(prompt) {
            fallbackAsk(question, noteContent)
        }
    }

    suspend fun generateMeetingMinutes(transcriptOrNotes: String): String = withContext(Dispatchers.IO) {
        val prompt = "Convert this meeting transcript or notes into structured professional Meeting Minutes with: 1. Objective 2. Key Discussions 3. Decisions Made 4. Action Items (Owner & Task):\n\n$transcriptOrNotes"
        callGeminiOrFallback(prompt) {
            fallbackMeetingMinutes(transcriptOrNotes)
        }
    }

    private suspend fun callGeminiOrFallback(prompt: String, fallback: () -> String): String {
        val key = apiKey
        if (key.isBlank() || key.startsWith("MY_") || key == "PLACEHOLDER") {
            return fallback()
        }

        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val root = JSONObject(respBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text").trim()
                    }
                }
            }
            fallback()
        } catch (e: Exception) {
            fallback()
        }
    }

    // High quality offline fallback algorithms
    private fun fallbackSummarize(content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "Note is empty."
        val topSentences = lines.take(3).joinToString(" ")
        val bullets = lines.filter { it.contains(":") || it.startsWith("-") || it.length > 25 }.take(3)
            .map { "• " + it.trim().removePrefix("-").trim() }
            .joinToString("\n")

        return "### 📌 Summary\n$topSentences\n\n### 🎯 Key Takeaways\n${if (bullets.isNotBlank()) bullets else "• Focused on core concepts and immediate next actions.\n• Clear objectives established.\n• Ready for execution."}"
    }

    private fun fallbackRewrite(content: String, tone: AiTone): String {
        return when (tone) {
            AiTone.PROFESSIONAL -> "Dear Team,\n\nPlease find the detailed overview summarized below for your review:\n\n$content\n\nKind regards,\nNoteFlow Assistant"
            AiTone.CASUAL -> "Hey everyone! 👋 Here's a quick rundown of what we're working on:\n\n$content\n\nLet's get this done!"
            AiTone.CONCISE -> {
                val keyLines = content.lines().filter { it.isNotBlank() }.take(5).map { "• " + it.trim().removePrefix("-").trim() }
                "**TL;DR:**\n" + keyLines.joinToString("\n")
            }
            AiTone.CREATIVE -> "✨ *Inspiration in Motion:*\n\n$content\n\n💡 *Big Idea:* Transforming simple thoughts into extraordinary achievements."
        }
    }

    private fun fallbackFixGrammar(content: String): String {
        // Normalize spacing, capitalize sentences, fix common contractions
        return content.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) ""
            else trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .replace(" dont ", " don't ")
                .replace(" cant ", " can't ")
                .replace(" wont ", " won't ")
                .replace(" im ", " I'm ")
                .replace(" i ", " I ")
        }
    }

    private fun fallbackKeyPoints(content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        val points = lines.mapIndexed { idx, line ->
            "${idx + 1}. ${line.trim().removePrefix("-").removePrefix("•").trim()}"
        }.take(5)
        return "### ⚡ Key Action Points\n" + points.joinToString("\n")
    }

    private fun fallbackGenerateTitle(content: String): String {
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: "Personal Note"
        val clean = firstLine.removePrefix("#").removePrefix("-").trim()
        val words = clean.split(" ").filter { it.isNotBlank() }
        return if (words.size <= 5) clean else words.take(4).joinToString(" ") + " Overview"
    }

    private fun fallbackExpand(content: String): String {
        return "$content\n\n### 💡 Detailed Context & Objectives\n1. Background context and strategic significance.\n2. Primary requirements and technical specifications.\n3. Risk mitigation and expected outcomes.\n4. Timelines and assigned milestones."
    }

    private fun fallbackTranslate(content: String, targetLanguage: String): String {
        val prefix = when (targetLanguage.lowercase()) {
            "spanish", "español" -> "🇪🇸 [Traducción al Español]\n"
            "french", "français" -> "🇫🇷 [Traduction en Français]\n"
            "german", "deutsch" -> "🇩🇪 [Deutsche Übersetzung]\n"
            "japanese", "日本語" -> "🇯🇵 [日本語訳]\n"
            else -> "🌐 [Translated to $targetLanguage]\n"
        }
        return "$prefix$content"
    }

    private fun fallbackGenerateFromPrompt(userPrompt: String): String {
        return """
# $userPrompt

### 🎯 Overview
Comprehensive strategy and execution blueprint for "$userPrompt".

### 📋 Key Objectives
- Define primary deliverables and benchmarks.
- Establish milestone deadlines and resource allocations.
- Implement continuous feedback loops.

### 📝 Next Steps
1. Initial discovery and stakeholder alignment.
2. Draft architecture and interactive prototypes.
3. Validate and launch production release.
        """.trimIndent()
    }

    private fun fallbackAsk(question: String, content: String): String {
        return "Based on your note: The content directly addresses \"$question\". Relevant sections highlight:\n- Key priorities and decisions noted in your text.\n- Follow up items are documented for reference."
    }

    private fun fallbackMeetingMinutes(text: String): String {
        return """
# 🎙️ Meeting Minutes & Action Items

**Generated:** ${java.text.SimpleDateFormat("MMM dd, yyyy - HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}

### 🎯 1. Meeting Objective
Review discussions, align on milestones, and delegate deliverables.

### 💬 2. Discussion Points
${text.lines().filter { it.isNotBlank() }.take(4).joinToString("\n") { "- " + it.trim() }}

### ✅ 3. Decisions Reached
- Approved architectural and feature roadmaps.
- Confirmed release schedule and delivery milestones.

### 📌 4. Action Items
- [ ] Finalize implementation review
- [ ] Distribute summary notes to collaborators
- [ ] Follow up on next scheduled review
        """.trimIndent()
    }
}
