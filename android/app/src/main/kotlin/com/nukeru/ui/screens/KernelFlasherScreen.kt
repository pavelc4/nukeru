package com.nukeru.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    // Trigger mock root check verification
    LaunchedEffect(isCheckingRoot) {
        if (isCheckingRoot) {
            delay(1800)
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
                delay(400)
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
                delay(200)
                p += 0.2f
                backupProgress = p.coerceAtMost(1f)
            }
            delay(300)
            consoleLogs.add("[BACKUP] Created partition image: /sdcard/Nukeru/Backups/backup_${part}_slot${activeSlot}.img")
            backupProgressPartition = null
            showBackupDialog = false
        }
    }

    // Trigger mock reboot
    LaunchedEffect(isRebooting) {
        if (isRebooting) {
            delay(2000)
            isRebooting = false
            showRebootDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
                // STATE A: Root Required Empty State Screen
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Locks & Shield Custom Vector Canvas representation
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Root Permission Required",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "KernelFlasher interacts directly with partition blocks to flash AnyKernel3 archives and modify device slots.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "How to grant access:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "When prompted by your root manager, grant Nukeru 'Superuser' access.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { isCheckingRoot = true },
                            enabled = !isCheckingRoot,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            if (isCheckingRoot) {
                                WavyCircularProgressIndicator(
                                    progress = { 0.4f },
                                    color = MaterialTheme.colorScheme.onError,
                                    trackColor = Color.Transparent,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Icons.Outlined.Shield, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Root Access & Verify", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // STATE B: Premium Kernel Flasher Dashboard
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. A/B Slot Manager Panel
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "A/B Partition Slots",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Displays active running system partitions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Active Slot: $activeSlot",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Slot A Button
                                OutlinedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (activeSlot == "A") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                         else Color.Transparent
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (activeSlot == "A") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeSlot = "A" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("SLOT A", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (activeSlot == "A") "Running (Active)" else "Bootable",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (activeSlot == "A") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Slot B Button
                                OutlinedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (activeSlot == "B") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                         else Color.Transparent
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (activeSlot == "B") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeSlot = "B" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("SLOT B", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(
                                    text = "Kernel Version",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "On-Device Backups",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Backup partitions directly to your device storage",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("3 Images Saved", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            val partitions = listOf("boot", "init_boot", "vendor_boot", "dtbo")
                            partitions.forEach { part ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        Text(part, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                backupProgressPartition = part
                                                showBackupDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Backup")
                                        }
                                        OutlinedButton(
                                            onClick = { /* Simulated restore action */ },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Flash AnyKernel3 Zip / Image
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Flash Kernel Bundle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Interactive Zip Selection Box
                            OutlinedCard(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFileUri = "/storage/emulated/0/Download/kernels/AnyKernel3-KSU-6.1-Pixel.zip"
                                        consoleLogs.add("[FILE] Loaded zip file target: AnyKernel3-KSU-6.1-Pixel.zip")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
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
                            }

                            // Flash Target Slot Segmented Switch
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Flash Destination", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    listOf("active" to "Active Slot", "inactive" to "Inactive Slot", "both" to "Both Slots").forEach { (opt, label) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (targetSlotOption == opt) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                    else Color.Transparent
                                                )
                                                .clickable { targetSlotOption = opt }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (targetSlotOption == opt) MaterialTheme.colorScheme.primary
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
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Outlined.FlashOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Execute Flash Script", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5. Console Terminal
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F110D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "ROOT LOGSHELL OUTPUT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
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

                    // 6. Advanced Reboot Panel & Diagnostics
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Device Controls & Diagnostics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick Log dump triggers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Dmesg", "Logcat", "Ramoops").forEach { dump ->
                                    OutlinedButton(
                                        onClick = {
                                            consoleLogs.add("[DIAG] Generated standard $dump diagnostic report to: /sdcard/Nukeru/Logs/${dump.lowercase()}.txt")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Outlined.Troubleshoot, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(dump, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Advanced Reboot triggers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("System" to "Reboot", "Recovery" to "Recovery", "Bootloader" to "Fastboot").forEach { (target, label) ->
                                    Button(
                                        onClick = {
                                            rebootTarget = target
                                            showRebootDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
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
