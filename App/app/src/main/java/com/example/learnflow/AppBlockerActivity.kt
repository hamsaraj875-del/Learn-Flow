
package com.example.learnflow

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppBlockerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BLOCKED_APP = "blocked_app"
    }

    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var btnEnable: Button
    private lateinit var txtSelectedCount: TextView
    private lateinit var permissionCard: LinearLayout

    private val selectedApps = mutableSetOf<String>()
    private lateinit var appAdapter: AppAdapter

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val appIcon: Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedApp = intent.getStringExtra(EXTRA_BLOCKED_APP)

        if (blockedApp != null) {
            showBlockedScreen(blockedApp)
            return
        }

        setContentView(R.layout.activity_app_blocker)

        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        btnEnable = findViewById(R.id.btnEnable)
        txtSelectedCount = findViewById(R.id.txtSelectedCount)
        permissionCard = findViewById(R.id.permissionCard)

        appsRecyclerView.layoutManager = LinearLayoutManager(this)

        selectedApps.addAll(
            getSharedPreferences("blocker", MODE_PRIVATE)
                .getStringSet("apps", emptySet()) ?: emptySet()
        )

        btnEnable.setOnClickListener {
            if (isAccessibilityPermissionGranted()) {
                toast("Permission already granted")
            } else {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (selectedApps.isEmpty()) {
                toast("Select at least one app")
                return@setOnClickListener
            }
            if (!isAccessibilityPermissionGranted()) {
                toast("Enable LearnFlow accessibility service")
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            saveApps()
            getSharedPreferences("blocker", MODE_PRIVATE)
                .edit()
                .putBoolean("active", true)
                .apply()
            toast("Focus Mode started")
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            getSharedPreferences("blocker", MODE_PRIVATE)
                .edit()
                .putBoolean("active", false)
                .apply()
            toast("Focus Mode stopped")
        }
        loadApps()
        updateSelectedCount()
    }

    override fun onResume() {
        super.onResume()
        if (::btnEnable.isInitialized) {
            val granted = isAccessibilityPermissionGranted()
            btnEnable.text = if (granted) "Permission Granted" else "Grant Permission"
            permissionCard.visibility = if (granted) View.GONE else View.VISIBLE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_BLOCKED_APP)?.let {
            showBlockedScreen(it)
        }
    }

    private fun showBlockedScreen(appName: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }
        val title = TextView(this).apply {
            text = "Focus Mode Active"
            textSize = 28f
            setTextColor(Color.rgb(255, 152, 0))
            gravity = Gravity.CENTER
        }
        val message = TextView(this).apply {
            text = "$appName is blocked during your study session."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
        val button = Button(this).apply {
            text = "Return to Home"
            setOnClickListener {
                val home = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }
                startActivity(home)
                finish()
            }
        }
        layout.addView(title)
        layout.addView(message)
        layout.addView(button)
        setContentView(layout)
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    appName = it.loadLabel(packageManager).toString(),
                    appIcon = it.loadIcon(packageManager)
                )
            }
            .sortedBy { it.appName.lowercase() }
        appAdapter = AppAdapter(apps)
        appsRecyclerView.adapter = appAdapter
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

    private fun isAccessibilityPermissionGranted(): Boolean {
        val componentName = ComponentName(
            this,
            AppBlockerService::class.java
        )

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any {
            it.equals(
                componentName.flattenToString(),
                ignoreCase = true
            )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    inner class AppAdapter(
        private val apps: List<AppInfo>
    ) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

        inner class AppViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            val appIcon: ImageView = view.findViewById(R.id.imgAppIcon)
            val appName: TextView = view.findViewById(R.id.txtAppName)
            val checkBox: CheckBox = view.findViewById(R.id.checkApp)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocker_app, parent, false)
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
            holder.checkBox.isChecked = selectedApps.contains(app.packageName)
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
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            }
        }
        override fun getItemCount(): Int = apps.size
    }
}
