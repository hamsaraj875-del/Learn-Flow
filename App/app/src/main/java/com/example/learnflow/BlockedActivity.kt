
package com.example.learnflow

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName =
            intent.getStringExtra("blocked_app") ?: "This app"

        showBlockedScreen(appName)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val appName =
            intent.getStringExtra("blocked_app") ?: "This app"

        showBlockedScreen(appName)
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
                returnToHome()
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(button)

        setContentView(layout)
    }

    private fun returnToHome() {

        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        startActivity(home)
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        returnToHome()
    }
}
