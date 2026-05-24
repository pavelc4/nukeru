package com.nukeru.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nukeru.ui.components.WavyLinearProgressIndicator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlasherScreen() {
    var isOtgConnected by remember { mutableStateOf(false) }
    var selectedPartition by remember { mutableStateOf("boot") }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var disableVerity by remember { mutableStateOf(true) }
    var isFlashing by remember { mutableStateOf(false) }
    var flashProgress by remember { mutableStateOf(0f) }
    val consoleLogs = remember { mutableStateListOf<String>() }
    var showPartitionDropdown by remember { mutableStateOf(false) }

    val partitions = listOf("boot", "init_boot", "recovery", "vbmeta", "vendor_boot", "dtbo", "system", "vendor")

    // Dynamic mock OTG polling animation
    LaunchedEffect(Unit) {
        delay(2500)
        isOtgConnected = true
        consoleLogs.addAll(listOf(
            "[OTG] USB Host controller active.",
            "[OTG] Target device connected in fastboot mode (USB ID: 18d1:4ee0).",
            "$ fastboot devices",
            "nukeru_otg_target    fastboot"
        ))
    }

    // Trigger mock flashing process
    LaunchedEffect(isFlashing) {
        if (isFlashing) {
            val imgName = selectedImagePath?.substringAfterLast("/") ?: "image.img"
            consoleLogs.addAll(listOf(
                "$ fastboot flash $selectedPartition $imgName",
                "Sending '$selectedPartition' (${(32..64).random()} MB)..."
            ))
            
            var p = 0f
            while (p < 1.0f) {
                delay(300)
                p += 0.15f
                flashProgress = p.coerceAtMost(1f)
            }
            delay(400)
            consoleLogs.add("Writing '$selectedPartition'... OKAY [0.925s]")
            
            if (selectedPartition == "vbmeta" && disableVerity) {
                consoleLogs.addAll(listOf(
                    "$ fastboot flash --disable-verity --disable-verification vbmeta vbmeta.img",
                    "Disabling boot verity flags... OKAY [0.210s]"
                ))
            }
            
            consoleLogs.add("Finished. Total time: 3.421s")
            isFlashing = false
            flashProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. OTG Connection Status Panel
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOtgConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                 else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
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
                        .background(
                            if (isOtgConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOtgConnected) Icons.Outlined.Usb else Icons.Outlined.UsbOff,
                        contentDescription = null,
                        tint = if (isOtgConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOtgConnected) "Device Connected (Fastboot)" else "Waiting for OTG Connection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isOtgConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isOtgConnected) "Nukeru Host controller is ready to flash." else "Connect target phone via OTG USB cable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Flash Configuration Card
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
                    text = "Flash Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Partition Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPartitionDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Target Partition: $selectedPartition", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            }
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showPartitionDropdown,
                        onDismissRequest = { showPartitionDropdown = false }
                    ) {
                        partitions.forEach { p: String ->
                            DropdownMenuItem(
                                text = { Text(p, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedPartition = p
                                    showPartitionDropdown = false
                                }
                            )
                        }
                    }
                }

                // File Picker Box
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedImagePath = "/storage/emulated/0/Download/Nukeru/${selectedPartition}.img"
                            consoleLogs.add("[FILE] Loaded image path: $selectedImagePath")
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
                            Icon(Icons.Outlined.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = if (selectedImagePath != null) "${selectedPartition}.img" else "Select Partition Image",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = selectedImagePath ?: "Tap to choose boot/recovery .img file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        if (selectedImagePath != null) {
                            IconButton(onClick = { selectedImagePath = null }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    }
                }

                // VBMeta Disable-Verity Switch
                if (selectedPartition == "vbmeta") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disable Android Verity / Verification", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Text("Add --disable-verity flags to avoid bootloops.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = disableVerity, onCheckedChange = { disableVerity = it })
                    }
                }
            }
        }

        // 3. Wavy Progress Indicator (Only visible when flashing)
        if (isFlashing) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Flashing Active Partition...", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("${(flashProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    WavyLinearProgressIndicator(
                        progress = { flashProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 4. Action Button
        Button(
            onClick = { isFlashing = true },
            enabled = isOtgConnected && selectedImagePath != null && !isFlashing,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Outlined.FlashOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Flash Partition Image", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }

        // 5. Console output terminal
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
                    text = "CONSOLE OUTPUT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (consoleLogs.isEmpty()) {
                    Text(
                        text = "Waiting for fastboot OTG connection log...",
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
                            color = if (log.startsWith("$")) Color(0xFF4ADE80) else if (log.contains("OKAY")) Color.Cyan else Color.LightGray,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // padding for bottom nav
    }
}
