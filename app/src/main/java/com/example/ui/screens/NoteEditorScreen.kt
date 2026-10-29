package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ai.AiTone
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.components.SketchCanvasDialog
import com.example.ui.theme.NOTE_COLOR_PALETTES
import com.example.ui.viewmodel.NoteFlowViewModel
import com.example.util.ExportHelper
import org.json.JSONArray

@Composable
fun NoteEditorScreen(
    viewModel: NoteFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val isRecording by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val recDuration by viewModel.audioRecorder.recordingDurationSec.collectAsStateWithLifecycle()
    val recAmplitude by viewModel.audioRecorder.amplitude.collectAsStateWithLifecycle()

    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val playProgress by viewModel.audioPlayer.playbackProgress.collectAsStateWithLifecycle()

    val isListening by viewModel.voiceTranscription.isListening.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.voiceTranscription.partialTranscript.collectAsStateWithLifecycle()

    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsStateWithLifecycle()

    var title by remember(currentNote) { mutableStateOf(currentNote?.title ?: "") }
    var content by remember(currentNote) { mutableStateOf(currentNote?.content ?: "") }
    var tags by remember(currentNote) { mutableStateOf(currentNote?.tags ?: "") }
    var colorId by remember(currentNote) { mutableStateOf(currentNote?.colorId ?: "default") }
    var folderId by remember(currentNote) { mutableStateOf(currentNote?.folderId) }
    var summaryText by remember(currentNote) { mutableStateOf(currentNote?.summaryText) }
    var attachedAudioPath by remember(currentNote) { mutableStateOf(currentNote?.audioPath) }
    var attachedImageUri by remember(currentNote) { mutableStateOf(currentNote?.imageUri) }
    var attachedSketch by remember(currentNote) { mutableStateOf(currentNote?.sketchData) }
    var isLocked by remember(currentNote) { mutableStateOf(currentNote?.isLocked ?: false) }

    var showAiModal by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSketchDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var showAskDialog by remember { mutableStateOf(false) }
    var askQuestionInput by remember { mutableStateOf("") }
    var aiAnswerText by remember { mutableStateOf<String?>(null) }
    var showLockPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    val isProUser by viewModel.billingManager.isProUser.collectAsStateWithLifecycle()
    var showProPaywallDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.audioRecorder.startRecording(
                onSuccess = { path -> attachedAudioPath = path },
                onError = {}
            )
        }
    }

    // Voice dictation permission launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceTranscription.startListening(
                onResult = { text ->
                    content = if (content.isBlank()) text else "$content $text"
                },
                onError = {}
            )
        }
    }

    // Auto-save on exit or change
    fun triggerSave() {
        val note = currentNote ?: return
        viewModel.saveCurrentNote(
            title = title,
            content = content,
            tags = tags,
            colorId = colorId,
            folderId = folderId
        )
    }

    BackHandler {
        triggerSave()
        viewModel.closeEditor()
    }

    val palette = NOTE_COLOR_PALETTES.find { it.id == colorId } ?: NOTE_COLOR_PALETTES[0]
    val bgColor = Color(android.graphics.Color.parseColor(palette.bgDarkHex))
    val accentColor = Color(android.graphics.Color.parseColor(palette.accentHex))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            Surface(
                color = Color(0xFF111827),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                triggerSave()
                                viewModel.closeEditor()
                            },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        // Auto-save badge
                        Text(
                            text = "Auto-saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // AI Assistant Trigger
                        Button(
                            onClick = {
                                if (viewModel.canUseAi()) {
                                    showAiModal = true
                                } else {
                                    android.widget.Toast.makeText(context, "Daily AI limit reached (3/3). Upgrade to NoteFlow Pro for unlimited AI.", android.widget.Toast.LENGTH_LONG).show()
                                    showProPaywallDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("ai_assistant_button")
                        ) {
                            Text("✨ AI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Palette color picker
                        IconButton(onClick = { showColorDialog = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Color theme", tint = accentColor)
                        }

                        // Folder selector
                        IconButton(onClick = { showFolderDialog = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color.LightGray)
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.LightGray)
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share Note", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showMoreMenu = false
                                        currentNote?.let { ExportHelper.shareNote(context, it.copy(title = title, content = content)) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to Markdown", color = Color.White) },
                                    onClick = {
                                        showMoreMenu = false
                                        currentNote?.let { ExportHelper.exportToMarkdown(context, it.copy(title = title, content = content)) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to Plain Text", color = Color.White) },
                                    onClick = {
                                        showMoreMenu = false
                                        currentNote?.let { ExportHelper.exportToTxt(context, it.copy(title = title, content = content)) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Version History", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showMoreMenu = false
                                        showVersionHistory = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isLocked) "Remove Note PIN Lock" else "Lock Note with PIN", color = Color(0xFFF59E0B)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    onClick = {
                                        showMoreMenu = false
                                        if (isLocked) {
                                            isLocked = false
                                        } else {
                                            if (isProUser) {
                                                showLockPinDialog = true
                                            } else {
                                                android.widget.Toast.makeText(context, "Note PIN Vault is exclusive to NoteFlow Pro members.", android.widget.Toast.LENGTH_SHORT).show()
                                                showProPaywallDialog = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090D16))
        ) {
            // AI Loading indicator banner
            AnimatedVisibility(visible = isAiLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF083344))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = aiStatusMessage ?: "AI is thinking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE0F2FE)
                    )
                }
            }

            // Speech transcription live status banner
            AnimatedVisibility(visible = isListening) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E1065))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (partialTranscript.isNotBlank()) partialTranscript else "Listening to your voice... Speak clearly",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFDDD6FE)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.voiceTranscription.stopListening() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                    }
                }
            }

            // Audio memo recording status banner
            AnimatedVisibility(visible = isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF881337))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF43F5E))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording: ${recDuration}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { recAmplitude },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFDA4AF),
                        trackColor = Color(0xFF4C0519)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val path = viewModel.audioRecorder.stopRecording()
                            if (path != null) attachedAudioPath = path
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Finish recording", tint = Color.White)
                    }
                }
            }

            // Main Editor Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Folder indicator
                val currentFolder = folders.find { it.id == folderId }
                if (currentFolder != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentFolder.name, color = accentColor, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // AI Executive Summary Banner (if generated)
                if (!summaryText.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2338)),
                        border = BorderStroke(1.dp, Color(0xFF0284C7))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✨", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "AI Summary & Takeaways",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { summaryText = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = summaryText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0F2FE),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Audio Note Playback Card (if audio attached)
                if (attachedAudioPath != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        viewModel.audioPlayer.pause()
                                    } else {
                                        viewModel.audioPlayer.play(attachedAudioPath!!)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF06B6D4))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🎙️ Voice Memo", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { playProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF06B6D4),
                                    trackColor = Color(0xFF334155)
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.audioPlayer.stop()
                                    attachedAudioPath = null
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove audio", tint = Color.Gray)
                            }
                        }
                    }
                }

                // Image Attachment Preview
                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White)
                        }
                    }
                }

                // Sketch note indicator
                if (attachedSketch != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable { showSketchDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, tint = Color(0xFFA78BFA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✍️ Hand-drawn sketch attached", color = Color(0xFFDDD6FE), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Edit", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Note Title Field
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(Color(0xFF06B6D4)),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "Note Title...",
                                color = Color(0xFF64748B),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags input
                BasicTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    textStyle = TextStyle(
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        if (tags.isEmpty()) {
                            Text(
                                text = "+ Add tags (e.g. Work, Ideas, Strategy)",
                                color = Color(0xFF475569),
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Note Content Field
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = TextStyle(
                        color = Color(0xFFF1F5F9),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF06B6D4)),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "Start writing your thoughts, tap the mic to transcribe speech, or use ✨ AI to structure your notes...",
                                color = Color(0xFF64748B),
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("note_content_input")
                )
            }

            // Bottom Rich-Text & Multimedia Toolbar
            Surface(
                color = Color(0xFF111827),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice dictation button
                    item {
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    viewModel.voiceTranscription.stopListening()
                                } else {
                                    voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isListening) Color(0xFF8B5CF6) else Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Dictation", tint = Color.White)
                        }
                    }

                    // Audio memo recorder
                    item {
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    val path = viewModel.audioRecorder.stopRecording()
                                    if (path != null) attachedAudioPath = path
                                } else {
                                    if (!isProUser && attachedAudioPath != null) {
                                        android.widget.Toast.makeText(context, "Free tier allows 1 voice memo per note. Upgrade to Pro for unlimited recordings.", android.widget.Toast.LENGTH_LONG).show()
                                        showProPaywallDialog = true
                                    } else {
                                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRecording) Color(0xFFF43F5E) else Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record Audio Memo", tint = if (isRecording) Color.White else Color(0xFFFDA4AF))
                        }
                    }

                    // Photo attachment
                    item {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Add image", tint = Color.LightGray)
                        }
                    }

                    // Sketch Canvas
                    item {
                        IconButton(onClick = { showSketchDialog = true }) {
                            Icon(Icons.Default.Draw, contentDescription = "Draw or sketch", tint = Color(0xFFA78BFA))
                        }
                    }

                    // Formatting tools
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "# " else "$content\n# " }) {
                            Text("H1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "## " else "$content\n## " }) {
                            Text("H2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    item {
                        IconButton(onClick = { content = "$content **Bold** " }) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.LightGray)
                        }
                    }
                    item {
                        IconButton(onClick = { content = "$content *Italic* " }) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.LightGray)
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "- " else "$content\n- " }) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = Color.LightGray)
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "1. " else "$content\n1. " }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", tint = Color.LightGray)
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "- [ ] " else "$content\n- [ ] " }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Checklist", tint = Color(0xFF10B981))
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty()) "> " else "$content\n> " }) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    // AI Assistant Modal Dialog
    if (showAiModal) {
        AlertDialog(
            onDismissRequest = { showAiModal = false },
            containerColor = Color(0xFF111827),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨ NoteFlow AI Assistant", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select an AI action for this note:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)

                    // 1. Summarize
                    AiActionRow("📌 Summarize Note", "Generate executive summary and takeaways") {
                        showAiModal = false
                        viewModel.aiSummarize(content) { res ->
                            summaryText = res
                        }
                    }

                    // 2. Fix Grammar & Spelling
                    AiActionRow("📝 Fix Grammar & Spelling", "Auto-correct mistakes and punctuation") {
                        showAiModal = false
                        viewModel.aiFixGrammar(content) { res ->
                            content = res
                        }
                    }

                    // 3. Extract Key Action Points
                    AiActionRow("⚡ Extract Key Points", "Convert note into numbered action items") {
                        showAiModal = false
                        viewModel.aiExtractKeyPoints(content) { res ->
                            content = "$content\n\n$res"
                        }
                    }

                    // 4. Generate Catchy Title
                    AiActionRow("🏷️ Generate Title", "Create concise 3-5 word title") {
                        showAiModal = false
                        viewModel.aiGenerateTitle(content) { res ->
                            title = res
                        }
                    }

                    // 5. Tone: Professional
                    AiActionRow("👔 Tone: Professional", "Polish into executive tone") {
                        showAiModal = false
                        viewModel.aiRewrite(content, AiTone.PROFESSIONAL) { res ->
                            content = res
                        }
                    }

                    // 6. Tone: Casual
                    AiActionRow("☕ Tone: Casual & Friendly", "Relaxed conversational tone") {
                        showAiModal = false
                        viewModel.aiRewrite(content, AiTone.CASUAL) { res ->
                            content = res
                        }
                    }

                    // 7. Tone: Concise (TL;DR)
                    AiActionRow("⚡ Tone: Concise (TL;DR)", "Distill into punchy summary") {
                        showAiModal = false
                        viewModel.aiRewrite(content, AiTone.CONCISE) { res ->
                            content = res
                        }
                    }

                    // 8. Expand Writing
                    AiActionRow("💡 Expand Thoughts", "Flesh out rough notes into full plan") {
                        showAiModal = false
                        viewModel.aiExpandWriting(content) { res ->
                            content = res
                        }
                    }

                    // 9. Meeting Minutes Formatter
                    AiActionRow("🎙️ Format Meeting Minutes", "Organize into Agenda, Decisions & Next Steps") {
                        showAiModal = false
                        viewModel.aiMeetingMinutes(content) { res ->
                            content = res
                        }
                    }

                    // 10. Ask Notes (Q&A)
                    AiActionRow("❓ Ask Questions About Note", "Ask the AI anything about your note content") {
                        showAiModal = false
                        showAskDialog = true
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiModal = false }) {
                    Text("Close", color = Color(0xFF06B6D4))
                }
            }
        )
    }

    // Ask Note Dialog
    if (showAskDialog) {
        AlertDialog(
            onDismissRequest = { showAskDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Ask Your Note", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = askQuestionInput,
                        onValueChange = { askQuestionInput = it },
                        label = { Text("Ask a question about this note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    if (aiAnswerText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(aiAnswerText!!, color = Color(0xFF38BDF8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.aiAskNote(askQuestionInput, content) { answer ->
                            aiAnswerText = answer
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Ask", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskDialog = false; aiAnswerText = null }) {
                    Text("Done", color = Color.LightGray)
                }
            }
        )
    }

    // Color Palette Selection Dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Pick Note Color Theme", color = Color.White) },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    items(NOTE_COLOR_PALETTES) { p ->
                        val col = Color(android.graphics.Color.parseColor(p.accentHex))
                        val isExclusiveTheme = p.id != "default" && p.id != "ocean"
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (colorId == p.id) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (isExclusiveTheme && !isProUser) {
                                        showColorDialog = false
                                        showProPaywallDialog = true
                                        android.widget.Toast.makeText(context, "Theme '${p.name}' is exclusive to NoteFlow Pro members.", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        colorId = p.id
                                        showColorDialog = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isExclusiveTheme && !isProUser) {
                                Text("👑", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // Folder Selector Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Assign to Folder", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (folderId == null) Color(0xFF334155) else Color.Transparent)
                            .clickable {
                                folderId = null
                                showFolderDialog = false
                            }
                            .padding(10.dp)
                    ) {
                        Text("None (Root / Uncategorized)", color = Color.White)
                    }

                    folders.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (folderId == f.id) Color(0xFF334155) else Color.Transparent)
                            .clickable {
                                folderId = f.id
                                showFolderDialog = false
                            }
                            .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(f.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Sketch Canvas Modal
    if (showSketchDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSketchDialog = false }) {
            SketchCanvasDialog(
                initialData = attachedSketch,
                onSaveSketch = { sketchData ->
                    attachedSketch = sketchData
                },
                onDismiss = { showSketchDialog = false }
            )
        }
    }

    // Version History Dialog
    if (showVersionHistory) {
        AlertDialog(
            onDismissRequest = { showVersionHistory = false },
            containerColor = Color(0xFF111827),
            title = { Text("Note Version History", color = Color.White) },
            text = {
                val raw = currentNote?.versionHistoryJson
                if (raw.isNullOrBlank()) {
                    Text("No previous snapshots saved yet.", color = Color.Gray)
                } else {
                    val snapshots = remember(raw) {
                        val arr = JSONArray(raw)
                        (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            o.optLong("timestamp") to o.optString("content")
                        }.reversed()
                    }
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        snapshots.forEachIndexed { idx, (time, oldContent) ->
                            val dateStr = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Snapshot #${snapshots.size - idx} — $dateStr", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                                    Text(oldContent.take(60) + "...", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                TextButton(
                                    onClick = {
                                        content = oldContent
                                        showVersionHistory = false
                                    }
                                ) {
                                    Text("Restore", color = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionHistory = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // Lock Note with PIN Dialog
    if (showLockPinDialog) {
        AlertDialog(
            onDismissRequest = { showLockPinDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Lock Note with PIN", color = Color.White) },
            text = {
                Column {
                    Text("Set a 4-digit PIN to protect this note:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4) enteredPin = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin.length == 4) {
                            isLocked = true
                            showLockPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Lock Note", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockPinDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    if (showProPaywallDialog) {
        com.example.ui.components.NoteFlowProDialog(
            billingManager = viewModel.billingManager,
            onDismiss = { showProPaywallDialog = false }
        )
    }
}

@Composable
fun AiActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
    }
}
