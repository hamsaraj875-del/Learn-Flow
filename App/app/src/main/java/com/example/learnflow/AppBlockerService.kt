
package com.example.learnflow

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class AppBlockerService : AccessibilityService() {

    private var lastBlockedPackage = ""
    private var lastBlockedTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == applicationContext.packageName) return

        val prefs = getSharedPreferences("blocker", MODE_PRIVATE)

        if (!prefs.getBoolean("active", false)) return

        val blockedApps = prefs.getStringSet("apps", emptySet()) ?: emptySet()

        if (packageName !in blockedApps) return

        val currentTime = SystemClock.elapsedRealtime()

        if (packageName == lastBlockedPackage &&
            currentTime - lastBlockedTime < 2000
        ) return

        lastBlockedPackage = packageName
        lastBlockedTime = currentTime

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }

        showBlockedNotification(appName)

        val blockerIntent = Intent(this, AppBlockerActivity::class.java).apply {
            putExtra(AppBlockerActivity.EXTRA_BLOCKED_APP, appName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(blockerIntent)
    }

    private fun showBlockedNotification(appName: String) {

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "focus_mode_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Mode",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentTitle("⚠️ A Message From Future You")
            .setContentText("I'm suffering because you chose distractions over your goals.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚠️ I'm your future self."+
                            "Every time you choose distraction, You still have time to change that. Start now." +
                            "Close this app and start studying. Your future depends on the choices you make today."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        manager.notify(appName.hashCode(), notification)
    }

    override fun onInterrupt() {
    }
}
