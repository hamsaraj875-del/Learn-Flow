package com.example.learnflow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastQueryTime = 0L
    private var currentForeground = ""
    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    private val poller = object : Runnable {
        override fun run() {
            checkForeground()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()

        val ongoing = NotificationCompat.Builder(this, SERVICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentTitle("Focus Mode is on")
            .setContentText("LearnFlow is blocking your selected apps")
            .setOngoing(true)
            .build()
        startForeground(1, ongoing)

        lastQueryTime = System.currentTimeMillis()
        handler.post(poller)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(poller)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForeground() {
        val prefs = getSharedPreferences("blocker", MODE_PRIVATE)
        if (!prefs.getBoolean("active", false)) {
            stopSelf()
            return
        }

        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(lastQueryTime, now)
        lastQueryTime = now

        @Suppress("DEPRECATION")
        val resumed = if (Build.VERSION.SDK_INT >= 29)
            UsageEvents.Event.ACTIVITY_RESUMED
        else
            UsageEvents.Event.MOVE_TO_FOREGROUND

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == resumed) currentForeground = e.packageName
        }

        val pkg = currentForeground
        if (pkg.isEmpty() || pkg == packageName) return

        val blockedApps = prefs.getStringSet("apps", emptySet()) ?: emptySet()
        if (pkg !in blockedApps) return

        val t = SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPackage && t - lastBlockedTime < 2000) return
        lastBlockedPackage = pkg
        lastBlockedTime = t

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (ex: Exception) {
            pkg
        }

        showBlockedNotification(appName)
        openBlockedActivity(appName)
    }

    private fun openBlockedActivity(appName: String) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            putExtra("blocked_app", appName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val m = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            m.createNotificationChannel(
                NotificationChannel(SERVICE_CHANNEL, "Focus service", NotificationManager.IMPORTANCE_LOW)
            )
            m.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "Focus Mode", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun showBlockedNotification(appName: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentTitle("A Message From Future You")
            .setContentText("$appName is blocked during your study session.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "I'm your future self. " +
                            "You still have time to change that. " +
                            "Close this app and start studying. " +
                            "Your future depends on the choices you make today."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(appName.hashCode(), notification)
    }

    companion object {
        const val SERVICE_CHANNEL = "focus_service_channel"
        const val ALERT_CHANNEL = "focus_mode_channel"
    }
}