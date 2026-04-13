package com.nukeru.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.nukeru.ui.screens.HomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiraApp() {
    var homeState by remember { mutableIntStateOf(1) } // 1: Empty, 2: Selection, 3: Progress
    var currentTab by remember { mutableIntStateOf(1) } // 1: Home, 2: Log, 3: About
    var isFloatingNav by remember { mutableStateOf(true) } // Toggle for Nav mode

    val navItems = listOf(
        NavItem("Home", Icons.Outlined.Home, 1),
        NavItem("Log", Icons.Outlined.History, 2),
        NavItem("Tentang", Icons.Outlined.Info, 3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kira",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate Settings */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            // Only show FAB in state 1 or 2
            if (homeState < 3) {
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
                    Text(text = if (homeState == 1) "Pilih File .zip" else "Extract 2 Partisi", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            KiraBottomNav(
                items = navItems,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                isFloating = isFloatingNav
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("About Screen Placeholder")
                    }
                }
            }
        }
    }
}
