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
                        zipName = zipName,
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


