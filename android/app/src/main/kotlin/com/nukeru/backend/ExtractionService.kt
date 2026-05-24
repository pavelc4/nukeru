package com.nukeru.backend

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

import android.os.ParcelFileDescriptor
import androidx.compose.runtime.mutableStateListOf
import com.nukeru.ui.screens.PartitionInfo

class ExtractionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    object ExtractionState {
        var isRunning by mutableStateOf(false)
        var statusText by mutableStateOf("Idle")
        var zipName by mutableStateOf("")
        var zipFdPath by mutableStateOf("")
        var pfd by mutableStateOf<ParcelFileDescriptor?>(null)
        val partitionsList = mutableStateListOf<PartitionInfo>()
        val progressMap = mutableStateMapOf<String, Float>()
        var isDone by mutableStateOf(false)
        var savedDir by mutableStateOf("")
        var errorText by mutableStateOf<String?>(null)

        fun reset(zip: String) {
            isRunning = true
            statusText = "Extracting..."
            zipName = zip
            progressMap.clear()
            isDone = false
            savedDir = ""
            errorText = null
        }

        fun clearSelection() {
            zipName = ""
            zipFdPath = ""
            pfd?.close()
            pfd = null
            partitionsList.clear()
            isDone = false
            isRunning = false
            errorText = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val zipFdPath = intent?.getStringExtra("zipFdPath") ?: ""
        val outDir = intent?.getStringExtra("outDir") ?: ""
        val selectedNames = intent?.getStringExtra("selectedNames") ?: ""
        val zipName = intent?.getStringExtra("zipName") ?: ""

        if (zipFdPath.isNotEmpty() && outDir.isNotEmpty()) {
            ExtractionState.reset(zipName)
            ExtractionState.savedDir = outDir
            
            // Start Foreground immediately to satisfy OS requirements
            val notification = buildNotification("Starting extraction...", 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            job?.cancel()
            job = serviceScope.launch {
                runExtraction(zipFdPath, outDir, selectedNames)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun runExtraction(zipFdPath: String, outDir: String, selectedNames: String) {
        try {
            NukeruJni.startExtraction(zipFdPath, outDir, selectedNames)

            var lastNotificationUpdate = 0L
            while (true) {
                val msg = NukeruJni.pollProgress()
                if (msg == "WAIT" || msg == "NONE") {
                    delay(100)
                    continue
                }
                if (msg == "DISCONNECTED" || msg == "FINISHED") {
                    withContext(Dispatchers.Main) {
                        ExtractionState.statusText = "Extraction Complete!"
                        ExtractionState.isDone = true
                        ExtractionState.isRunning = false
                    }
                    updateNotification("Extraction completed successfully!", 100)
                    break
                }
                if (msg.startsWith("FATAL|")) {
                    val err = msg.substring(6)
                    if (err == "Extraction cancelled by user") {
                        withContext(Dispatchers.Main) {
                            ExtractionState.statusText = "Extraction Cancelled"
                            ExtractionState.errorText = "Cancelled"
                            ExtractionState.isDone = true
                            ExtractionState.isRunning = false
                        }
                        try {
                            java.io.File(outDir).deleteRecursively()
                        } catch (e: Exception) {}
                        updateNotification("Extraction cancelled", 0)
                    } else {
                        withContext(Dispatchers.Main) {
                            ExtractionState.statusText = "Error: $err"
                            ExtractionState.errorText = err
                            ExtractionState.isDone = true
                            ExtractionState.isRunning = false
                        }
                        updateNotification("Extraction failed: $err", 0)
                    }
                    break
                }
                if (msg.startsWith("P|")) {
                    val parts = msg.split("|")
                    if (parts.size >= 5) {
                        val pName = parts[1]
                        val opsDone = parts[2].toFloatOrNull() ?: 0f
                        val opsTotal = parts[3].toFloatOrNull() ?: 1f
                        val prog = if (opsTotal > 0f) opsDone / opsTotal else 0f
                        
                        withContext(Dispatchers.Main) {
                            ExtractionState.progressMap[pName] = prog
                            ExtractionState.statusText = "Extracting $pName..."
                        }

                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdate > 500) {
                            val percent = (prog * 100).toInt()
                            updateNotification("Extracting $pName: $percent%", percent)
                            lastNotificationUpdate = now
                        }
                    }
                }
                if (msg.startsWith("D|")) {
                    val parts = msg.split("|")
                    if (parts.size >= 3) {
                        val pName = parts[1]
                        withContext(Dispatchers.Main) {
                            ExtractionState.progressMap[pName] = 1f
                        }
                        updateNotification("$pName extracted", 100)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                ExtractionState.statusText = "Error: ${e.message}"
                ExtractionState.errorText = e.message
                ExtractionState.isDone = true
                ExtractionState.isRunning = false
            }
            updateNotification("Error: ${e.message}", 0)
        } finally {
            // Keep service alive in foreground for 2 seconds to show complete status, then stop
            delay(2000)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nukeru Partition Extraction",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live extraction progress of Nukeru partitions in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String, progress: Int): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nukeru Extractor")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0 && content.startsWith("Starting"))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(content: String, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(content, progress))
    }

    override fun onDestroy() {
        job?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "nukeru_extraction_channel"
        private const val NOTIFICATION_ID = 2026
    }
}
