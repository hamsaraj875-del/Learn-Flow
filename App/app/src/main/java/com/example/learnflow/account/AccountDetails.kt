
package com.example.learnflow.account

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.learnflow.R

class AccountDetails : AppCompatActivity() {

    private var glowAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_details)

        window.statusBarColor = android.graphics.Color.rgb(8, 11, 20)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 20)

        val name = intent.getStringExtra("name") ?: "X"
        val email = intent.getStringExtra("email") ?: ""

        findViewById<TextView>(R.id.tvAccountName).text = name
        findViewById<TextView>(R.id.tvName).text = name
        findViewById<TextView>(R.id.tvEmail).text = if (email.isNotEmpty()) email else "Email not available"
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            Toast.makeText(this, "Edit Profile", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.rowChangePassword).setOnClickListener {
            Toast.makeText(this, "Change Password", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.rowNotifications).setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.rowLogout).setOnClickListener {
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
        }
        val content = findViewById<View>(R.id.accountContent)
        val avatar = findViewById<ImageView>(R.id.imgAvatar)
        glowAnimator = ObjectAnimator.ofFloat(avatar, View.SCALE_X, 1f, 1.045f, 1f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }

        ObjectAnimator.ofFloat(avatar, View.SCALE_Y, 1f, 1.045f, 1f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
    }

    override fun onDestroy() {
        glowAnimator?.cancel()
        super.onDestroy()
    }
}
