package com.example.learnflow

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppBlockerActivity : AppCompatActivity() {

    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var btnEnable: Button
    private lateinit var txtSelectedCount: TextView
    private lateinit var permissionCard: LinearLayout

    private val selectedApps = mutableSetOf<String>()

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val appIcon: Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_blocker)

        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        btnEnable = findViewById(R.id.btnEnable)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        permissionCard = findViewById(R.id.permissionCard)

        appsRecyclerView.layoutManager = LinearLayoutManager(this)

        val savedApps = getSharedPreferences("blocker", MODE_PRIVATE)
            .getStringSet("apps", emptySet()) ?: emptySet()

        selectedApps.addAll(savedApps)

        btnEnable.setOnClickListener {
            if (allPermissionsGranted()) {
                toast("Permission already granted")
            } else {
                showPermissionDisclosure()
            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {

            if (selectedApps.isEmpty()) {
                toast("Select at least one app")
                return@setOnClickListener
            }

            if (!allPermissionsGranted()) {
                showPermissionDisclosure()
                return@setOnClickListener
            }

            saveApps()

            getSharedPreferences("blocker", MODE_PRIVATE)
                .edit()
                .putBoolean("active", true)
                .apply()

            ContextCompat.startForegroundService(
                this,
                Intent(this, AppBlockerService::class.java)
            )

            toast("Focus Mode started")
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {

            getSharedPreferences("blocker", MODE_PRIVATE)
                .edit()
                .putBoolean("active", false)
                .apply()

            stopService(Intent(this, AppBlockerService::class.java))

            toast("Focus Mode stopped")
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }

        loadApps()
        updateSelectedCount()
    }

    override fun onResume() {
        super.onResume()

        if (::btnEnable.isInitialized) {

            val granted = allPermissionsGranted()

            btnEnable.text = if (granted) {
                "Permission Granted"
            } else {
                "Grant Permission"
            }

            permissionCard.visibility = if (granted) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun showPermissionDisclosure() {

        val needUsage = !hasUsageAccess()

        AlertDialog.Builder(this)
            .setTitle("Enable Focus Mode")
            .setMessage(
                "LearnFlow needs two permissions:\n\n" +
                        "1. Usage access: to detect which app is open, " +
                        "so it can compare it with the apps you selected.\n\n" +
                        "2. Display over other apps: to show the focus " +
                        "screen on top of a blocked app.\n\n" +
                        "This check happens only on your device. LearnFlow does " +
                        "not read your screen and does not upload your app activity."
            )
            .setPositiveButton("Continue") { _, _ ->
                if (needUsage) {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } else {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadApps() {

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter {
                it.activityInfo.packageName != packageName
            }
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    appName = it.loadLabel(packageManager).toString(),
                    appIcon = it.loadIcon(packageManager)
                )
            }
            .sortedBy {
                it.appName.lowercase()
            }

        appsRecyclerView.adapter = AppAdapter(apps)
    }

    private fun saveApps() {

        getSharedPreferences("blocker", MODE_PRIVATE)
            .edit()
            .putStringSet("apps", selectedApps.toSet())
            .apply()
    }

    private fun updateSelectedCount() {
        txtSelectedCount.text = "${selectedApps.size} selected"
    }

    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun allPermissionsGranted(): Boolean =
        hasUsageAccess() && Settings.canDrawOverlays(this)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    inner class AppAdapter(
        private val apps: List<AppInfo>
    ) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

        inner class AppViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            val appIcon: ImageView =
                view.findViewById(R.id.imgAppIcon)

            val appName: TextView =
                view.findViewById(R.id.txtAppName)

            val checkBox: CheckBox =
                view.findViewById(R.id.checkApp)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AppViewHolder {

            val view = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_blocker_app,
                    parent,
                    false
                )

            return AppViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: AppViewHolder,
            position: Int
        ) {

            val app = apps[position]

            holder.appIcon.setImageDrawable(app.appIcon)
            holder.appName.text = app.appName

            holder.checkBox.setOnCheckedChangeListener(null)

            holder.checkBox.isChecked =
                selectedApps.contains(app.packageName)

            holder.checkBox.setOnCheckedChangeListener { _, checked ->

                if (checked) {
                    selectedApps.add(app.packageName)
                } else {
                    selectedApps.remove(app.packageName)
                }

                saveApps()
                updateSelectedCount()
            }

            holder.itemView.setOnClickListener {
                holder.checkBox.isChecked =
                    !holder.checkBox.isChecked
            }
        }

        override fun getItemCount(): Int = apps.size
    }
}