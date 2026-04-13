package com.nukeru.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nukeru.ui.components.KiraBottomNav
import com.nukeru.ui.components.NavItem
import com.nukeru.ui.screens.AboutScreen
import com.nukeru.ui.screens.HomeScreen
import com.nukeru.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiraApp(
    isDynamicColor: Boolean = true,
    onDynamicColorChange: (Boolean) -> Unit = {}
) {
    var homeState by remember { mutableIntStateOf(1) } // 1: Empty, 2: Selection, 3: Progress
    var currentTab by remember { mutableIntStateOf(1) } // 1: Home, 2: Log, 3: About
    var isFloatingNav by remember { mutableStateOf(true) } // Toggle for Nav mode
    var isSettingsOpen by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("Home", Icons.Outlined.Home, 1),
        NavItem("Log", Icons.Outlined.History, 2),
        NavItem("About", Icons.Outlined.Info, 3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSettingsOpen) "General Settings" else when(currentTab) {
                            1 -> "Kira"
                            2 -> "Log History"
                            3 -> "About Kira"
                            else -> "Kira"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (isSettingsOpen) {
                        IconButton(onClick = { isSettingsOpen = false }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
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
        floatingActionButton = {
            // Only show FAB in state 1 or 2, and NOT in settings
            if (homeState < 3 && currentTab == 1 && !isSettingsOpen) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (homeState == 1) homeState = 2 else if (homeState == 2) homeState = 3
                    },
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircleOutline, "Select")
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (homeState == 1) "Select .zip File" else "Extract 2 Partitions", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            if (!isSettingsOpen) {
                KiraBottomNav(
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
                    onDynamicColorChanged = { onDynamicColorChange(it) }
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Log Screen Placeholder")
                    }
                }
                3 -> {
                    AboutScreen()
                }
            } 
        } 
        } 
    } 
} 
