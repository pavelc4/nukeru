package com.nukeru.ui.screens

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



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
                val dirs = nukeruDir.listFiles { file -> file.isDirectory } ?: arrayOf()
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

    Crossfade(targetState = selectedItem, label = "HistoryCrossfade") { currentItem ->
        if (currentItem != null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Extraction History",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Files saved to Downloads/Nukeru will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
