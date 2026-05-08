package com.nukeru.ui.screens

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nukeru.backend.NukeruJni

data class PartitionInfo(
    val name: String,
    val sizeBytes: Long,
    val opCount: Int,
    var checked: Boolean = true
)

@Composable
fun HomeScreen(
    currentState: Int,
    onStateChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var zipName by remember { mutableStateOf("") }
    var zipFdPath by remember { mutableStateOf("") }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var partitionsList = remember { mutableStateListOf<PartitionInfo>() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                // Get file name
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                zipName = cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else "payload.zip"
                } ?: "payload.zip"

                // Open FD
                val newPfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (newPfd != null) {
                    pfd?.close() // close previous if any
                    pfd = newPfd
                    zipFdPath = "FD:${newPfd.fd}"

                    val res = NukeruJni.getPartitions(zipFdPath)
                    if (res.startsWith("OK:")) {
                        partitionsList.clear()
                        val partsData = res.substring(3).split(";")
                        for (p in partsData) {
                            if (p.isNotBlank()) {
                                val split = p.split("|")
                                if (split.size == 3) {
                                    partitionsList.add(
                                        PartitionInfo(split[0], split[1].toLong(), split[2].toInt(), true)
                                    )
                                }
                            }
                        }
                        onStateChange(2) // Move to SelectionState
                    } else {
                        Toast.makeText(context, "Gagal membaca ZIP: $res", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Crossfade(targetState = currentState, label = "HomeStateCrossfade") { state ->
                when (state) {
                    1 -> EmptyState(
                        onSelectFile = { launcher.launch("*/*") }
                    )
                    2 -> SelectionState(
                        zipName = zipName,
                        partitions = partitionsList,
                        onPartitionToggle = { index ->
                            val item = partitionsList[index]
                            partitionsList[index] = item.copy(checked = !item.checked)
                        },
                        onStartExtract = { onStateChange(3) }
                    )
                    3 -> ProgressState(
                        zipFdPath = zipFdPath,
                        partitions = partitionsList.filter { it.checked },
                        onComplete = {
                            pfd?.close()
                            pfd = null
                            onStateChange(1)
                        }
                    )
                }
            }
        }

        // Floating Action Button
        if (currentState < 3) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (currentState == 1) launcher.launch("*/*")
                    else if (currentState == 2 && partitionsList.any { it.checked }) onStateChange(3)
                },
                containerColor = MaterialTheme.colorScheme.inversePrimary,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Outlined.CheckCircleOutline, "Select")
                Spacer(Modifier.width(8.dp))
                val fabText = if (currentState == 1) "Select .zip File" else "Extract ${partitionsList.count { it.checked }} Partitions"
                Text(text = fabText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState(onSelectFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(40.dp)
                )
                .clickable { onSelectFile() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Select Partitions to Extract",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select an OTA .zip file to extract partitions. Processed via streaming using Rust.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SelectionState(
    zipName: String,
    partitions: List<PartitionInfo>,
    onPartitionToggle: (Int) -> Unit,
    onStartExtract: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SELECTED FILE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = zipName,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    maxLines = 1
                )
                Text(
                    text = "Check the partitions you want to dump to your internal storage.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(partitions.size) { index ->
                    val p = partitions[index]
                    PartitionSelectionItem(
                        name = p.name,
                        size = "${p.sizeBytes / 1024 / 1024} MB",
                        checked = p.checked,
                        onToggle = { onPartitionToggle(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun PartitionSelectionItem(name: String, size: String, checked: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun ProgressState(
    zipFdPath: String,
    partitions: List<PartitionInfo>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var isDone by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Extracting...") }
    val progressMap = remember { mutableStateMapOf<String, Float>() }
    var savedDirStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val baseDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val nukeruDir = java.io.File(baseDir, "Nukeru")
            if (!nukeruDir.exists()) {
                nukeruDir.mkdirs()
            }
            val outDir = nukeruDir.absolutePath
            savedDirStr = outDir

            val selectedNames = partitions.joinToString(",") { it.name }
            
            NukeruJni.startExtraction(zipFdPath, outDir, selectedNames)

            while (true) {
                val msg = NukeruJni.pollProgress()
                if (msg == "WAIT" || msg == "NONE") {
                    delay(50)
                    continue
                }
                if (msg == "DISCONNECTED" || msg == "FINISHED") {
                    statusText = "Extraction Complete!"
                    isDone = true
                    break
                }
                if (msg.startsWith("FATAL|")) {
                    statusText = "Error: ${msg.substring(6)}"
                    isDone = true
                    break
                }
                if (msg.startsWith("P|")) {
                    val parts = msg.split("|")
                    if (parts.size >= 5) {
                        val pName = parts[1]
                        val opsDone = parts[2].toFloatOrNull() ?: 0f
                        val opsTotal = parts[3].toFloatOrNull() ?: 1f
                        val prog = if (opsTotal > 0f) opsDone / opsTotal else 0f
                        progressMap[pName] = prog
                    }
                }
                if (msg.startsWith("D|")) {
                    val parts = msg.split("|")
                    if (parts.size >= 3) {
                        progressMap[parts[1]] = 1f
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isDone) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(partitions) { p ->
                val currentProg = progressMap[p.name] ?: 0f
                ProgressItem(name = p.name, progress = currentProg, totalOps = p.opCount)
            }
        }

        if (isDone) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Back to Home")
            }
            Text(
                text = "Files saved to $savedDirStr",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProgressItem(name: String, progress: Float, totalOps: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )
    val isComplete = animatedProgress >= 1f

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isComplete) Color(0xFF146C2E) else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isComplete) Color(0xFF146C2E) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ops: ${(animatedProgress * totalOps).toInt()} / $totalOps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
