package com.nukeru.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nukeru.ui.components.NukeruBottomNav
import com.nukeru.ui.components.NavItem
import com.nukeru.ui.screens.AboutScreen
import com.nukeru.ui.screens.HistoryScreen
import com.nukeru.ui.screens.HomeScreen
import com.nukeru.ui.screens.SettingsScreen
import com.nukeru.ui.screens.FlasherScreen
import com.nukeru.ui.screens.KernelFlasherScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NukeruApp(
    isDynamicColor: Boolean = true,
    onDynamicColorChange: (Boolean) -> Unit = {},
    selectedColorIndex: Int = 0,
    onColorIndexChange: (Int) -> Unit = {},
    selectedStyleMode: Int = 1,
    onStyleModeChange: (Int) -> Unit = {}
) {
    var homeState by remember { mutableIntStateOf(1) } // 1: Empty, 2: Selection, 3: Progress
    var currentTab by remember { mutableIntStateOf(1) } // 1: Home, 2: OTG Flasher, 3: Kernel Flasher, 4: Log, 5: About
    var isFloatingNav by remember { mutableStateOf(true) } // Toggle for Nav mode
    var isSettingsOpen by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("Home", Icons.Outlined.Home, 1),
        NavItem("Flasher", Icons.Outlined.FlashOn, 2),
        NavItem("Kernel", Icons.Outlined.Memory, 3),
        NavItem("Log", Icons.Outlined.History, 4),
        NavItem("About", Icons.Outlined.Info, 5)
    )

    androidx.activity.compose.BackHandler(enabled = currentTab == 1 && homeState == 2) {
        homeState = 1
        com.nukeru.backend.ExtractionService.ExtractionState.clearSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSettingsOpen) "General Settings" else when(currentTab) {
                            1 -> "Nukeru"
                            2 -> "Fastboot Flasher"
                            3 -> "Kernel Flasher"
                            4 -> "Log History"
                            5 -> "About Nukeru"
                            else -> "Nukeru"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (isSettingsOpen) {
                        IconButton(onClick = { isSettingsOpen = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (currentTab == 1 && homeState == 2) {
                        IconButton(onClick = {
                            homeState = 1
                            com.nukeru.backend.ExtractionService.ExtractionState.clearSelection()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (!isSettingsOpen) {
                        IconButton(onClick = { isSettingsOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },

        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            if (!isSettingsOpen) {
                NukeruBottomNav(
                    items = navItems,
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isFloating = isFloatingNav
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isSettingsOpen) {
                SettingsScreen(
                    isFloatingNav = isFloatingNav,
                    onNavStyleChanged = { isFloatingNav = it },
                    isDynamicColor = isDynamicColor,
                    onDynamicColorChanged = onDynamicColorChange,
                    selectedColorIndex = selectedColorIndex,
                    onColorIndexChanged = onColorIndexChange,
                    selectedStyleMode = selectedStyleMode,
                    onStyleModeChange = onStyleModeChange
                )
            } else {
                when (currentTab) {
                    1 -> {
                        HomeScreen(
                            currentState = homeState,
                            onStateChange = { newState -> homeState = newState }
                        )
                    }
                    2 -> {
                        FlasherScreen()
                    }
                    3 -> {
                        KernelFlasherScreen()
                    }
                    4 -> {
                        HistoryScreen()
                    }
                    5 -> {
                        AboutScreen()
                    }
                } 
            } 
        } 
    } 
}
