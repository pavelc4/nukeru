package com.nukeru.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nukeru.backend.NukeruJni
import com.nukeru.backend.ExtractionService
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

@Composable
fun AospEmptyIllustration(
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
            
            // Draw a minimalist AOSP-style Zip Capsule representation
            val rectWidth = width * 0.5f
            val rectHeight = height * 0.65f
            val rectLeft = (width - rectWidth) / 2f
            val rectTop = (height - rectHeight) / 2f
            
            // Draw the background dynamic rounded capsule
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.5f.dp.toPx())
            )
            
            // Draw dynamic "zip teeth" lines inside the capsule representing a secure archive
            val zipperTop = rectTop + 12.dp.toPx()
            val zipperBottom = rectTop + rectHeight - 12.dp.toPx()
            val zipperStep = 8.dp.toPx()
            var zY = zipperTop
            var toggle = false
            while (zY <= zipperBottom) {
                val lineLength = 5.dp.toPx()
                val startX = width / 2f
                val endX = if (toggle) startX + lineLength else startX - lineLength
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(startX, zY),
                    end = androidx.compose.ui.geometry.Offset(endX, zY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                zY += zipperStep
                toggle = !toggle
            }
            
            // Draw small dynamic floating circles (representing extracted clean partition blocks / pixels)
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(rectLeft - 10.dp.toPx(), rectTop + 10.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.5f),
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(rectLeft + rectWidth + 12.dp.toPx(), rectTop + rectHeight - 15.dp.toPx())
            )
        }
    }
}

@Composable
fun EmptyState(onSelectFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AospEmptyIllustration(
            modifier = Modifier.clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onSelectFile
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Select Partitions to Extract",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
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
    val animatedBgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ItemBgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                      else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ItemBorderColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedBgColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (checked) BorderStroke(1.dp, animatedBorderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp
                    ),
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun ProgressState(
    zipFdPath: String,
    zipName: String,
    partitions: List<PartitionInfo>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val state = ExtractionService.ExtractionState

    LaunchedEffect(Unit) {
        if (!state.isRunning && !state.isDone) {
            val baseDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val nukeruDir = java.io.File(baseDir, "Nukeru")
            
            val timeFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val timestamp = timeFormat.format(java.util.Date())
            val romBaseName = zipName.substringBeforeLast(".zip")
            val sessionDirName = "${romBaseName}_$timestamp"
            
            val sessionDir = java.io.File(nukeruDir, sessionDirName)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            val outDir = sessionDir.absolutePath
            val selectedNames = partitions.joinToString(",") { it.name }

            val intent = Intent(context, ExtractionService::class.java).apply {
                putExtra("zipFdPath", zipFdPath)
                putExtra("outDir", outDir)
                putExtra("selectedNames", selectedNames)
                putExtra("zipName", zipName)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    if (state.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(partitions) { p ->
                    val currentProg = state.progressMap[p.name] ?: 0f
                    ProgressItem(name = p.name, progress = currentProg, totalOps = p.opCount)
                }
            }

            if (state.isRunning) {
                Button(
                    onClick = {
                        NukeruJni.cancelExtraction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Cancel Extraction", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (state.isDone) {
                LaunchedEffect(state.savedDir) {
                    if (state.savedDir.isNotEmpty() && state.errorText == null) {
                        Toast.makeText(context, "Files saved to ${state.savedDir}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        if (state.isDone) {
            ExtendedFloatingActionButton(
                onClick = {
                    state.isDone = false
                    state.isRunning = false
                    onComplete()
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 16.dp)
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = "Home")
                Spacer(Modifier.width(8.dp))
                Text("Back to Home", fontWeight = FontWeight.Bold)
            }
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

    val progressColor by animateColorAsState(
        targetValue = if (isComplete) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
        label = "ProgressColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isComplete) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = progressColor
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            // Premium Wavy progress line - active waves fade out into a flat line on success!
            com.nukeru.ui.components.WavyLinearProgressIndicator(
                progress = { animatedProgress },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                strokeWidth = 4.dp,
                amplitude = if (isComplete) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ops: ${(animatedProgress * totalOps).toInt()} / $totalOps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
