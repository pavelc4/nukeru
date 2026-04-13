package com.nukeru.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    isFloatingNav: Boolean,
    onNavStyleChanged: (Boolean) -> Unit
) {
    var isThemePreset by remember { mutableStateOf(true) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedModeIndex by remember { mutableIntStateOf(1) } // 0=Muted, 1=Expressive, 2=Vibrant
    var isEnglish by remember { mutableStateOf(false) }
    var isDebugMode by remember { mutableStateOf(false) }
    var logLevelIndex by remember { mutableIntStateOf(1) } // 1=Debug

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Navigation Style ---
        SectionTitle("Bottom Navigation Style")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavStyleCard(
                title = "Standard",
                isSelected = !isFloatingNav,
                onClick = { onNavStyleChanged(false) },
                modifier = Modifier.weight(1f)
            )
            NavStyleCard(
                title = "Floating",
                isSelected = isFloatingNav,
                onClick = { onNavStyleChanged(true) },
                modifier = Modifier.weight(1f)
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- Theme System ---
        SectionTitle("Theme System")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeToggleCard(
                title = "Material You (Auto)",
                subtitle = "Follow system wallpaper (A12+)",
                isSelected = !isThemePreset,
                onClick = { isThemePreset = false }
            )
            ThemeToggleCard(
                title = "Manual Preset",
                subtitle = "Choose color and style",
                isSelected = isThemePreset,
                onClick = { isThemePreset = true }
            )
        }

        AnimatedVisibility(visible = isThemePreset) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Base Color",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPreset(
                                name = "Pistachio", c1 = Color(0xFFC5E384), c2 = Color(0xFF200F07),
                                isSelected = selectedColorIndex == 0,
                                onClick = { selectedColorIndex = 0 },
                                modifier = Modifier.weight(1f)
                            )
                            ColorPreset(
                                name = "Matcha", c1 = Color(0xFFC2D8C4), c2 = Color(0xFF222222),
                                isSelected = selectedColorIndex == 1,
                                onClick = { selectedColorIndex = 1 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPreset(
                                name = "Tyrian", c1 = Color(0xFF700143), c2 = Color(0xFFF8EDAD),
                                isSelected = selectedColorIndex == 2,
                                onClick = { selectedColorIndex = 2 },
                                modifier = Modifier.weight(1f)
                            )
                            ColorPreset(
                                name = "Moss", c1 = Color(0xFF385144), c2 = Color(0xFFF8F5F2),
                                isSelected = selectedColorIndex == 3,
                                onClick = { selectedColorIndex = 3 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Style (Mode)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SegmentedControl(
                            items = listOf("Muted", "Expressive", "Vibrant"),
                            selectedIndex = selectedModeIndex,
                            onItemSelected = { selectedModeIndex = it }
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- Language ---
        SectionTitle("Language (Bahasa)")
        SegmentedControl(
            items = listOf("Indonesia", "English"),
            selectedIndex = if (isEnglish) 1 else 0,
            onItemSelected = { isEnglish = it == 1 },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            activeColor = MaterialTheme.colorScheme.surface,
            inactiveColor = Color.Transparent
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- Developer Options ---
        SectionTitle("Developer Options")
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = { isDebugMode = !isDebugMode }
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Debug Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Show JNI & Rust logs (Verbose)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isDebugMode, onCheckedChange = { isDebugMode = it })
            }
        }

        AnimatedVisibility(visible = isDebugMode) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "JNI/Rust Log Level",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // FlowRow or wrapped layout could be used in Compose, simple Row with scroll or grid here
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Trace", "Debug", "Info", "Warn", "Error").forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (logLevelIndex == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                    .clickable { logLevelIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    title,
                                    fontSize = 11.sp,
                                    fontWeight = if (logLevelIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (logLevelIndex == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Warning: Trace/Debug mode will generate high volume I/O logs which may slow down extraction.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
fun NavStyleCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Miniature rep of bottom nav
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(16.dp, 8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Box(modifier = Modifier.size(12.dp).background(Color.Gray.copy(alpha=0.4f), CircleShape))
                    Box(modifier = Modifier.size(12.dp).background(Color.Gray.copy(alpha=0.4f), CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeToggleCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorPreset(
    name: String,
    c1: Color,
    c2: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
        ),
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(c1))
                    Box(Modifier.weight(1f).fillMaxHeight().background(c2))
                }
            }
            Text(name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
    activeColor: Color = MaterialTheme.colorScheme.surface,
    inactiveColor: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedIndex == index) activeColor else inactiveColor)
                    .clickable { onItemSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selectedIndex == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
