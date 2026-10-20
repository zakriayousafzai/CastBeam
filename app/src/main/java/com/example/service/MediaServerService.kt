package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaServerService : Service() {
    private val tag = "MediaServerService"

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val ACTION_START = "com.example.action.START_SERVER"
        const val ACTION_STOP = "com.example.action.STOP_SERVER"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_IP = "extra_ip"
        const val CHANNEL_ID = "castbeam_server_channel"
        const val NOTIFICATION_ID = 1001

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context, ipAddress: String, port: Int) {
            val intent = Intent(context, MediaServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_IP, ipAddress)
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MediaServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: "192.168.x.x"
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                startForegroundServer(ip, port)
            }
            ACTION_STOP -> {
                stopForegroundServer()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServer(ipAddress: String, port: Int) {
        acquireLocks()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MediaServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val serverUrl = "http://$ipAddress:$port"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CastBeam Server Active")
            .setContentText("Broadcasting at $serverUrl")
            .setSubText("Smart TV Media Hub")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        _isServiceRunning.value = true
        Log.i(tag, "Foreground service started with locks at $serverUrl")
    }

    private fun stopForegroundServer() {
        releaseLocks()
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CastBeam::ServerWakeLock").apply {
                setReferenceCounted(false)
                acquire(4 * 3600 * 1000L) // 4 hours max safe timeout
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "CastBeam::ServerWifiLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire wake/wifi locks: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing locks: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CastBeam Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps CastBeam local media streaming running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releaseLocks()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
