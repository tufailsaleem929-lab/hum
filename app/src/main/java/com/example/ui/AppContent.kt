package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PinLockScreen
import com.example.ui.screens.ArchiveTrashScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.SettingsSecurityScreen
import com.example.ui.viewmodel.NavigationScreen
import com.example.ui.viewmodel.NoteFlowViewModel

data class NavItem(
    val screen: NavigationScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val NAV_ITEMS = listOf(
    NavItem(NavigationScreen.DASHBOARD, "Notes", Icons.Filled.Notes, Icons.Outlined.Notes, "tab_notes"),
    NavItem(NavigationScreen.PLANNER, "Planner", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_planner"),
    NavItem(NavigationScreen.FOLDERS, "Folders", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_folders"),
    NavItem(NavigationScreen.ARCHIVE, "Archive", Icons.Filled.Archive, Icons.Outlined.Archive, "tab_archive"),
    NavItem(NavigationScreen.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
)

@Composable
fun NoteFlowAppContent(
    viewModel: NoteFlowViewModel = viewModel()
) {
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    if (!isAppUnlocked) {
        PinLockScreen(
            onPinEntered = { pin -> viewModel.unlockApp(pin) }
        )
        return
    }

    // Sub-screen back handler
    if (currentScreen != NavigationScreen.DASHBOARD) {
        BackHandler {
            if (currentScreen == NavigationScreen.NOTE_EDITOR) {
                viewModel.closeEditor()
            } else {
                viewModel.navigateTo(NavigationScreen.DASHBOARD)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        val isWideScreen = maxWidth > 600.dp

        if (currentScreen == NavigationScreen.NOTE_EDITOR) {
            // Full-screen note editor without bottom nav
            NoteEditorScreen(viewModel = viewModel)
        } else if (isWideScreen) {
            // Tablet & Desktop Layout: NavigationRail on the left + content
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = Color(0xFF111827),
                    contentColor = Color(0xFF06B6D4),
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NAV_ITEMS.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color(0xFF06B6D4),
                                indicatorColor = Color(0xFF06B6D4),
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentScreen) {
                        NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        NavigationScreen.PLANNER -> PlannerScreen(viewModel = viewModel)
                        NavigationScreen.FOLDERS -> FoldersScreen(viewModel = viewModel)
                        NavigationScreen.ARCHIVE, NavigationScreen.TRASH -> ArchiveTrashScreen(viewModel = viewModel)
                        NavigationScreen.SETTINGS -> SettingsSecurityScreen(viewModel = viewModel)
                        NavigationScreen.NOTE_EDITOR -> NoteEditorScreen(viewModel = viewModel)
                    }
                }
            }
        } else {
            // Phone Layout: Content + NavigationBar at bottom
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color(0xFF090D16),
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF111827),
                        contentColor = Color(0xFF06B6D4),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val isSelected = currentScreen == item.screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(item.screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = Color(0xFF06B6D4),
                                    indicatorColor = Color(0xFF06B6D4),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        NavigationScreen.PLANNER -> PlannerScreen(viewModel = viewModel)
                        NavigationScreen.FOLDERS -> FoldersScreen(viewModel = viewModel)
                        NavigationScreen.ARCHIVE, NavigationScreen.TRASH -> ArchiveTrashScreen(viewModel = viewModel)
                        NavigationScreen.SETTINGS -> SettingsSecurityScreen(viewModel = viewModel)
                        NavigationScreen.NOTE_EDITOR -> NoteEditorScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
