package com.nukeru.ui.screens

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import com.nukeru.backend.ExtractionService

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
    val state = ExtractionService.ExtractionState

    androidx.activity.compose.BackHandler(enabled = currentState == 2) {
        state.clearSelection()
        onStateChange(1)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                state.zipName = cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else "payload.zip"
                } ?: "payload.zip"

                val newPfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (newPfd != null) {
                    state.pfd?.close()
                    state.pfd = newPfd
                    state.zipFdPath = "FD:${newPfd.fd}"

                    val res = NukeruJni.getPartitions(state.zipFdPath)
                    if (res.startsWith("OK:")) {
                        state.partitionsList.clear()
                        val partsData = res.substring(3).split(";")
                        for (p in partsData) {
                            if (p.isNotBlank()) {
                                val split = p.split("|")
                                if (split.size == 3) {
                                    state.partitionsList.add(
                                        PartitionInfo(split[0], split[1].toLong(), split[2].toInt(), true)
                                    )
                                }
                            }
                        }
                        onStateChange(2)
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
            AnimatedContent(
                targetState = currentState,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "HomeStateTransition"
            ) { s ->
                when (s) {
                    1 -> EmptyState(
                        onSelectFile = { launcher.launch("*/*") }
                    )
                    2 -> SelectionState(
                        zipName = state.zipName,
                        partitions = state.partitionsList,
                        onPartitionToggle = { index ->
                            val item = state.partitionsList[index]
                            state.partitionsList[index] = item.copy(checked = !item.checked)
                        },
                        onStartExtract = { onStateChange(3) }
                    )
                    3 -> ProgressState(
                        zipFdPath = state.zipFdPath,
                        zipName = state.zipName,
                        partitions = state.partitionsList.filter { it.checked },
                        onComplete = {
                            state.clearSelection()
                            onStateChange(1)
                        }
                    )
                }
            }
        }

        if (currentState < 3) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (currentState == 1) launcher.launch("*/*")
                    else if (currentState == 2 && state.partitionsList.any { it.checked }) onStateChange(3)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Outlined.CheckCircleOutline, "Select")
                Spacer(Modifier.width(8.dp))
                val fabText = if (currentState == 1) "Select .zip File" else "Extract ${state.partitionsList.count { it.checked }} Partitions"
                Text(text = fabText, fontWeight = FontWeight.Bold)
            }
        }
    }
}


