package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.ui.viewmodel.NoteFlowViewModel

@Composable
fun ArchiveTrashScreen(
    viewModel: NoteFlowViewModel,
    modifier: Modifier = Modifier
) {
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Archived (${archivedNotes.size})", "Trash Bin (${trashNotes.size})")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🗄️ Archive & Trash",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF111827),
                contentColor = Color(0xFF06B6D4),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF06B6D4)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) Color(0xFF06B6D4) else Color(0xFF94A3B8),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // Archived Tab
                if (archivedNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No archived notes.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(archivedNotes, key = { it.id }) { note ->
                            ArchiveTrashNoteRow(
                                note = note,
                                actionLabel = "Unarchive",
                                actionIcon = Icons.Default.Archive,
                                onAction = { viewModel.toggleArchive(note) },
                                onDeletePermanent = { viewModel.moveToTrash(note.id) }
                            )
                        }
                    }
                }
            } else {
                // Trash Tab
                if (trashNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Trash is empty.", color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notes are automatically deleted after 30 days.", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                        Button(
                            onClick = { viewModel.emptyTrash() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF881337)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Empty Trash", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(trashNotes, key = { it.id }) { note ->
                            val daysLeft = remember(note.deletedAt) {
                                val elapsed = System.currentTimeMillis() - (note.deletedAt ?: System.currentTimeMillis())
                                val days = 30 - (elapsed / (1000 * 60 * 60 * 24))
                                days.coerceAtLeast(1)
                            }
                            ArchiveTrashNoteRow(
                                note = note,
                                subtitle = "$daysLeft days left before permanent deletion",
                                actionLabel = "Restore",
                                actionIcon = Icons.Default.Restore,
                                onAction = { viewModel.restoreFromTrash(note.id) },
                                onDeletePermanent = { viewModel.deletePermanently(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveTrashNoteRow(
    note: NoteEntity,
    subtitle: String? = null,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle ?: note.content.take(60),
                style = MaterialTheme.typography.bodySmall,
                color = if (subtitle != null) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                maxLines = 1
            )
        }

        IconButton(onClick = onAction) {
            Icon(actionIcon, contentDescription = actionLabel, tint = Color(0xFF38BDF8))
        }

        IconButton(onClick = onDeletePermanent) {
            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = Color(0xFFF43F5E))
        }
    }
}
