package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.NoteFlowViewModel

@Composable
fun SettingsSecurityScreen(
    viewModel: NoteFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAppLockOn by remember { mutableStateOf(viewModel.securityManager.isAppLockEnabled) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }

    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isProUser by viewModel.billingManager.isProUser.collectAsStateWithLifecycle()
    var showProPaywallDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚙️ Settings & Security",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Google Play Billing Pro Section
            Text("GOOGLE PLAY SUBSCRIPTION", color = Color(0xFF06B6D4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isProUser) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF06B6D4).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isProUser) "NoteFlow Pro Active ✨" else "NoteFlow Free Plan",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isProUser) "All AI features, voice transcription & sync unlocked" else "Upgrade to unlock unlimited AI, sync & speech dictation",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showProPaywallDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProUser) Color(0xFF1E293B) else Color(0xFF06B6D4)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isProUser) "View Pro Benefits" else "Upgrade with Play Store",
                                color = if (isProUser) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.billingManager.restorePurchases { restored ->
                                    val msg = if (restored) "Pro subscription restored! 🎉" else "No active purchases found."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Restore", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Security Section
            Text("SECURITY & PRIVACY", color = Color(0xFF06B6D4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("App Lock with PIN", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Protect entire app with 4-digit PIN", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Switch(
                        checked = isAppLockOn,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (isProUser) {
                                    showSetPinDialog = true
                                } else {
                                    Toast.makeText(context, "App Lock & Security Vault is a NoteFlow Pro feature.", Toast.LENGTH_SHORT).show()
                                    showProPaywallDialog = true
                                }
                            } else {
                                viewModel.securityManager.disableAppPin()
                                isAppLockOn = false
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF06B6D4))
                    )
                }
            }

            // Cloud Sync Section
            Text("CLOUD SYNC & BACKUP", color = Color(0xFF06B6D4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cloud Synchronization", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text(syncStatus, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Button(
                            onClick = {
                                if (isProUser) {
                                    viewModel.syncWithCloud()
                                } else {
                                    Toast.makeText(context, "Cloud Sync across devices is a NoteFlow Pro feature.", Toast.LENGTH_SHORT).show()
                                    showProPaywallDialog = true
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isSyncing) "Syncing..." else "Sync Now", color = Color.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportBackup { json ->
                                    exportedJsonText = json
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Developer Sandbox & Testing Section (Requirement 7)
            Text("DEVELOPER SANDBOX & TEST PURCHASES", color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            var isTestModeOn by remember { mutableStateOf(viewModel.billingManager.isTestModeEnabled) }

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sandbox Test Purchases", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Simulate purchases locally without contacting Google Play. Strictly isolated from production.",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isTestModeOn,
                            onCheckedChange = { checked ->
                                viewModel.billingManager.isTestModeEnabled = checked
                                isTestModeOn = checked
                                if (!checked) {
                                    viewModel.billingManager.clearTestEntitlements()
                                }
                                Toast.makeText(context, if (checked) "Developer Sandbox Mode enabled" else "Developer Sandbox Mode disabled", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B))
                        )
                    }

                    if (isTestModeOn) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "⚠️ Sandboxed Mode: Simulated purchases are permitted only while this toggle is active and will never grant production entitlements.",
                            color = Color(0xFFFCD34D),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.billingManager.simulateTestPurchase(com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL)
                                    Toast.makeText(context, "Simulated Annual Pro unlocked", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Annual", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.billingManager.simulateTestPurchase(com.example.billing.BillingManager.PRODUCT_LIFETIME_VIP)
                                    Toast.makeText(context, "Simulated Lifetime VIP unlocked", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Lifetime", color = Color(0xFFF59E0B), fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.billingManager.clearTestEntitlements()
                                    Toast.makeText(context, "Entitlements cleared", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear", color = Color(0xFFF43F5E), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // About NoteFlow Section
            Text("ABOUT NOTEFLOW", color = Color(0xFF06B6D4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("NoteFlow v2.0 AI Edition", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "All-in-one AI note-taking with real-time speech transcription, executive summarization, rich multi-palette organization, and offline-first durability.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Set 4-Digit App PIN", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text("Enter 4 digits") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.securityManager.setAppPin(pinInput)
                            isAppLockOn = true
                            showSetPinDialog = false
                            Toast.makeText(context, "App PIN enabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Save PIN", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Export Backup Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Exported Backup Data", color = Color.White) },
            text = {
                Column {
                    Text("Your complete notes, folders, and tags backup:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
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
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("NoteFlow Backup", exportedJsonText))
                        Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Copy to Clipboard", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = Color.LightGray)
                }
            }
        )
    }

    // Restore Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Color(0xFF111827),
            title = { Text("Restore From Backup", color = Color.White) },
            text = {
                Column {
                    Text("Paste your exported JSON backup text below:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("{ \"notes\": [...] }") },
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
                        if (importJsonText.isNotBlank()) {
                            viewModel.restoreBackup(importJsonText) { count ->
                                Toast.makeText(context, "Restored $count notes successfully!", Toast.LENGTH_LONG).show()
                                showImportDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Restore", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
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
fun SettingsCard(content: @Composable () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        content = { content() }
    )
}
