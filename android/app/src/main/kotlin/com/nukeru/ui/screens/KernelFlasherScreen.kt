package com.nukeru.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nukeru.ui.components.WavyLinearProgressIndicator
import com.nukeru.ui.components.WavyCircularProgressIndicator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelFlasherScreen() {
    var isRootGranted by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }
    
    var activeSlot by remember { mutableStateOf("A") }
    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    var targetSlotOption by remember { mutableStateOf("active") } // active, inactive, both
    
    var isFlashing by remember { mutableStateOf(false) }
    var flashProgress by remember { mutableStateOf(0f) }
    val consoleLogs = remember { mutableStateListOf<String>() }
    
    var showRebootDialog by remember { mutableStateOf(false) }
    var rebootTarget by remember { mutableStateOf("") }
    var isRebooting by remember { mutableStateOf(false) }
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupProgressPartition by remember { mutableStateOf<String?>(null) }
    var backupProgress by remember { mutableStateOf(0f) }

    // Reboot Speed Dial FAB State
    var isRebootMenuExpanded by remember { mutableStateOf(false) }

    // Trigger mock root check verification
    LaunchedEffect(isCheckingRoot) {
        if (isCheckingRoot) {
            delay(1500)
            isRootGranted = true
            isCheckingRoot = false
            consoleLogs.addAll(listOf(
                "[ROOT] Superuser access granted successfully.",
                "[SYS] KernelFlasher sandbox initialized.",
                "[SYS] Partition block maps read OKAY (148 partitions parsed)."
            ))
        }
    }

    // Trigger mock AnyKernel3 flashing process
    LaunchedEffect(isFlashing) {
        if (isFlashing) {
            consoleLogs.clear()
            consoleLogs.addAll(listOf(
                "$ sh /data/user/0/com.nukeru/files/ak3/flash.sh",
                "AnyKernel3 by Osm0sis @ XDA-Developers",
                "---------------------------------------",
                "Unpacking boot image...",
                "Found raw boot block: /dev/block/by-name/boot_${activeSlot.lowercase()}",
                "Parsing kernel and ramdisk headers..."
            ))
            
            var p = 0f
            while (p < 1.0f) {
                delay(350)
                p += 0.12f
                flashProgress = p.coerceAtMost(1f)
                if (p >= 0.36f && p < 0.48f) {
                    if (!consoleLogs.contains("Patching ramdisk flags...")) {
                        consoleLogs.add("Patching ramdisk flags...")
                        consoleLogs.add("Inserting KernelSU helper hooks...")
                    }
                }
                if (p >= 0.7f && p < 0.82f) {
                    if (!consoleLogs.contains("Repacking boot ramdisk image...")) {
                        consoleLogs.add("Repacking boot ramdisk image...")
                        consoleLogs.add("Recalculating SHA-1 digests...")
                    }
                }
            }
            delay(400)
            consoleLogs.add("Writing back to block sector: boot_${activeSlot.lowercase()}... OKAY")
            
            if (targetSlotOption == "both") {
                val opposite = if (activeSlot == "A") "b" else "a"
                consoleLogs.addAll(listOf(
                    "Writing to secondary block sector: boot_$opposite...",
                    "Synching file system buffers... OKAY"
                ))
            }
            
            consoleLogs.add("Success! Kernel flash successfully completed.")
            isFlashing = false
            flashProgress = 0f
        }
    }

    // Trigger mock local partition backup
    LaunchedEffect(backupProgressPartition) {
        val part = backupProgressPartition
        if (part != null) {
            backupProgress = 0f
            var p = 0f
            while (p < 1.0f) {
                delay(180)
                p += 0.2f
                backupProgress = p.coerceAtMost(1f)
            }
            delay(200)
            consoleLogs.add("[BACKUP] Created partition image: /sdcard/Nukeru/Backups/backup_${part}_slot${activeSlot}.img")
            backupProgressPartition = null
            showBackupDialog = false
        }
    }

    // Trigger mock reboot
    LaunchedEffect(isRebooting) {
        if (isRebooting) {
            delay(1800)
            isRebooting = false
            showRebootDialog = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop overlay to dismiss reboot menu when expanded
        if (isRebootMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isRebootMenuExpanded = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP PREVIEW CONTROLLER: Root Simulator Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Environment Sandbox",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AssistChip(
                    onClick = { isRootGranted = !isRootGranted },
                    label = {
                        Text(
                            text = if (isRootGranted) "Root State: Granted" else "Root State: Denied",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isRootGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isRootGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                         else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            AnimatedContent(
                targetState = isRootGranted,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "RootStateTransition"
            ) { targetRootState ->
                if (!targetRootState) {
                    // STATE A: Root Required Empty State Screen (About-inspired Card style)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Root Access Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "KernelFlasher operates directly on locked hardware partition blocks to read/write boot and ramdisk sectors. This operation is locked behind superuser permission scopes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("1", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = "Ensure your phone is rooted with Magisk, KernelSU, or APatch.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("2", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = "When prompted by your root manager, grant Nukeru 'Superuser' access.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { isCheckingRoot = true },
                                enabled = !isCheckingRoot,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                if (isCheckingRoot) {
                                    WavyCircularProgressIndicator(
                                        progress = { 0.4f },
                                        color = MaterialTheme.colorScheme.onError,
                                        trackColor = Color.Transparent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Grant Superuser Access", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                } else {
                    // STATE B: Premium Kernel Flasher Dashboard (About-inspired Card style)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. A/B Slot Manager Panel
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.CompareArrows,
                                            contentDescription = "Slots",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "A/B Partition Slots",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Manage system partitions and active slots",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Slot A Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { activeSlot = "A" }
                                            .background(
                                                if (activeSlot == "A") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (activeSlot == "A") MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("SLOT A", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = if (activeSlot == "A") "Running (Active)" else "Bootable",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (activeSlot == "A") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Slot B Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { activeSlot = "B" }
                                            .background(
                                                if (activeSlot == "B") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (activeSlot == "B") MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("SLOT B", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = if (activeSlot == "B") "Running (Active)" else "Bootable",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (activeSlot == "B") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Kernel Status Info
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Memory,
                                        contentDescription = "Kernel",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Kernel Version",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Linux 6.1.75-android15-9-g0123abc4567-ab123456 #1 SMP PREEMPT Sunday May 24 2026",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. Backup & Restore Center
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Storage,
                                            contentDescription = "Backup",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "On-Device Backups",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Local backup images stored in internal storage",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val partitions = listOf("boot", "init_boot", "vendor_boot", "dtbo")
                                    partitions.forEach { part ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Storage,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = part,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        backupProgressPartition = part
                                                        showBackupDialog = true
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Backup", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                                }
                                                OutlinedButton(
                                                    onClick = { /* Simulated restore action */ },
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                                ) {
                                                    Text("Restore", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Flash AnyKernel3 Zip / Image
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.FolderZip,
                                            contentDescription = "Flash",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Flash Kernel Bundle",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Select and flash kernel updates directly on device",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Interactive Zip Selection Box (About-inspired list item row style)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                selectedFileUri = "/storage/emulated/0/Download/kernels/AnyKernel3-KSU-6.1-Pixel.zip"
                                                consoleLogs.add("[FILE] Loaded zip file target: AnyKernel3-KSU-6.1-Pixel.zip")
                                            }
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(
                                                    text = if (selectedFileUri != null) "AnyKernel3-KSU-6.1-Pixel.zip" else "Choose AnyKernel3 Bundle",
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = selectedFileUri ?: "Select AnyKernel3 zip or raw boot.img",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (selectedFileUri != null) {
                                            IconButton(onClick = { selectedFileUri = null }) {
                                                Icon(Icons.Outlined.Close, contentDescription = null)
                                            }
                                        }
                                    }

                                    // Flash Target Slot Segmented Switch
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Flash Destination",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(6.dp)
                                        ) {
                                            listOf("active" to "Active Slot", "inactive" to "Inactive Slot", "both" to "Both Slots").forEach { (opt, label) ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (targetSlotOption == opt) MaterialTheme.colorScheme.primaryContainer
                                                            else Color.Transparent
                                                        )
                                                        .clickable { targetSlotOption = opt }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (targetSlotOption == opt) MaterialTheme.colorScheme.onPrimaryContainer
                                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Flashing active progress status
                                    if (isFlashing) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Patching and Flashing kernel...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                Text("${(flashProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                            WavyLinearProgressIndicator(
                                                progress = { flashProgress },
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { isFlashing = true },
                                        enabled = selectedFileUri != null && !isFlashing,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Outlined.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Execute Flash Script", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }

                        // 5. Console Terminal
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Terminal,
                                            contentDescription = "Logs",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Console Output",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF0F110D))
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (consoleLogs.isEmpty()) {
                                            Text(
                                                text = "Ready. Select an AnyKernel3 zip to flash, or run a partition backup.",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        } else {
                                            consoleLogs.forEach { log ->
                                                Text(
                                                    text = log,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = if (log.startsWith("$") || log.startsWith("[ROOT]")) Color(0xFF4ADE80)
                                                            else if (log.contains("Success") || log.contains("OKAY")) Color.Cyan
                                                            else if (log.startsWith("AnyKernel3")) Color(0xFFFACC15)
                                                            else Color.LightGray,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Device Diagnostics Card (Reboot items completely moved to Speed Dial FAB!)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Troubleshoot,
                                            contentDescription = "Diagnostics",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Device Diagnostics",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Generate standard system logs and kernel diagnostics",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Dmesg", "Logcat", "Ramoops").forEach { dump ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .clickable {
                                                    consoleLogs.add("[DIAG] Generated standard $dump diagnostic report to: /sdcard/Nukeru/Logs/${dump.lowercase()}.txt")
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Outlined.Troubleshoot, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text(dump, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // padding for bottom nav
        }

        // Expandable Reboot Speed Dial FAB (Stays in bottom right corner, floating over layout)
        if (isRootGranted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Speed Dial Items
                    AnimatedVisibility(
                        visible = isRebootMenuExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            // Item 1: Reboot Bootloader
                            SpeedDialItem(
                                label = "Fastboot",
                                icon = Icons.Outlined.FlashOn,
                                onClick = {
                                    isRebootMenuExpanded = false
                                    rebootTarget = "Bootloader"
                                    showRebootDialog = true
                                }
                            )
                            
                            // Item 2: Reboot Recovery
                            SpeedDialItem(
                                label = "Recovery",
                                icon = Icons.Outlined.Build,
                                onClick = {
                                    isRebootMenuExpanded = false
                                    rebootTarget = "Recovery"
                                    showRebootDialog = true
                                }
                            )
                            
                            // Item 3: Reboot System
                            SpeedDialItem(
                                label = "System",
                                icon = Icons.Outlined.PowerSettingsNew,
                                onClick = {
                                    isRebootMenuExpanded = false
                                    rebootTarget = "System"
                                    showRebootDialog = true
                                }
                            )
                        }
                    }

                    // Main Power FAB (Normal 0f by default, tilts to -45f when clicked)
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isRebootMenuExpanded) -45f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "FabRotation"
                    )

                    FloatingActionButton(
                        onClick = { isRebootMenuExpanded = !isRebootMenuExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = "Reboot Options",
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(rotationZ = rotationAngle)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for mock backup progress
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss during operation */ },
            confirmButton = {},
            title = {
                Text("Creating Device Backup", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reading block sectors for partition '${backupProgressPartition}' and writing target image to /sdcard/Nukeru/Backups/...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    WavyLinearProgressIndicator(
                        progress = { backupProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Text(
                        text = "${(backupProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal dialog for mock reboot progress
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRebooting) showRebootDialog = false },
            title = {
                Text(
                    text = if (isRebooting) "Rebooting Device..." else "Reboot Confirmation",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRebooting) {
                        WavyCircularProgressIndicator(
                            progress = { 0.6f },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sending IPC signals to init... Device is going down for reboot to $rebootTarget now.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Are you sure you want to reboot the device to $rebootTarget?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                if (!isRebooting) {
                    Button(
                        onClick = { isRebooting = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reboot")
                    }
                }
            },
            dismissButton = {
                if (!isRebooting) {
                    TextButton(onClick = { showRebootDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
