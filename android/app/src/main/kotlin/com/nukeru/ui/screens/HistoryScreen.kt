package com.nukeru.ui.screens

import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AospHistoryEmptyIllustration(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .background(containerColor, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val width = size.width
            val height = size.height
            
            // Draw a beautiful AOSP-style Clock / Archive history representation
            val radius = width * 0.3f
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Outer dynamic outline circle representing history log
            drawCircle(
                color = color,
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 2.5f.dp.toPx())
            )
            
            // Clock hands representing past extraction events
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(centerX, centerY - radius * 0.6f),
                strokeWidth = 2.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(centerX + radius * 0.4f, centerY),
                strokeWidth = 2.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Small floating particles representing extracted parts
            drawCircle(
                color = color.copy(alpha = 0.7f),
                radius = 3.5f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(centerX - radius - 10.dp.toPx(), centerY - 8.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = 2.5f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(centerX + radius + 12.dp.toPx(), centerY + 12.dp.toPx())
            )
        }
    }
}

@Composable
fun HistoryScreen() {
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    BackHandler(enabled = selectedItem != null) {
        selectedItem = null
    }

    val loadHistory = suspend {
        withContext(Dispatchers.IO) {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val nukeruDir = File(baseDir, "Nukeru")
            
            if (!nukeruDir.exists()) {
                emptyList<HistoryItem>()
            } else {
                val dirs = nukeruDir.listFiles { file -> 
                    file.isDirectory && (file.listFiles { f -> f.isFile && f.name.endsWith(".img") }?.isNotEmpty() == true)
                } ?: arrayOf()
                dirs.map { dir ->
                    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".img") } ?: arrayOf()
                    var totalSize = 0L
                    for (f in files) {
                        totalSize += f.length()
                    }
                    
                    val lastModified = dir.lastModified()
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    val dateStr = sdf.format(Date(lastModified))
                    
                    val extractedFiles = files.map { f ->
                        ExtractedFile(f.name, f.length() / (1024 * 1024))
                    }.sortedBy { it.name }
                    
                    HistoryItem(
                        dir = dir,
                        name = dir.name,
                        dateStr = dateStr,
                        fileCount = files.size,
                        totalSizeMB = totalSize / (1024 * 1024),
                        files = extractedFiles
                    )
                }.sortedByDescending { it.dir.lastModified() }
            }
        }
    }

    LaunchedEffect(Unit) {
        historyList = loadHistory()
        isLoading = false
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Log?") },
            text = { Text("Are you sure you want to delete '${itemToDelete!!.name}' and all its extracted images? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dir = itemToDelete!!.dir
                        dir.deleteRecursively()
                        itemToDelete = null
                        // Refresh list
                        isLoading = true
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Refresh if deleted
    LaunchedEffect(isLoading) {
        if (isLoading) {
            historyList = loadHistory()
            isLoading = false
        }
    }

    AnimatedContent(
        targetState = selectedItem,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(300)))
            } else {
                (slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(300)))
            }
        },
        label = "HistoryTransition",
        modifier = Modifier.fillMaxSize()
    ) { currentItem ->
        if (currentItem != null) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                HistoryDetailScreen(
                    item = currentItem,
                    onBack = { selectedItem = null }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AospHistoryEmptyIllustration()
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "No Extraction History",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Files saved to Downloads/Nukeru will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyList) { item ->
                            HistoryCard(
                                item = item,
                                onClick = { selectedItem = item },
                                onDelete = { itemToDelete = item }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav
                        }
                    }
                }
            }
        }
    }
}
