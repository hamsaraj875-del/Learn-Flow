
package com.example.learnflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Notification permission denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        var userId: String? = ""
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var progressBar = findViewById<ProgressBar>(R.id.loader)
        var btn = findViewById<FrameLayout>(R.id.navAI)
        var focus = findViewById<LinearLayout>(R.id.navFocus)
        var session = findViewById<LinearLayout>(R.id.navSession);

        val chatRepository = ChatRepository(RetrofitClient.api)
        btn.visibility = View.GONE

        lifecycleScope.launch {
            try {
                var response = chatRepository.userIsLoggedIn()
                if (!response.success) {
                    var intent = Intent(this@MainActivity, Promo::class.java)
                    startActivity(intent)
                    finish();
                    return@launch
                } else {
                    userId = response.userId
                    progressBar.visibility = View.GONE
                    btn.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                var intent = Intent(this@MainActivity, Promo::class.java)
                startActivity(intent)
            } finally {
                progressBar.visibility = View.GONE
            }
        }

        btn.setOnClickListener {
            var intent = Intent(this@MainActivity, ChatTopic::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        focus.setOnClickListener {
            var intent = Intent(this@MainActivity, AppBlockerActivity::class.java)
            startActivity(intent)
        }

        session.setOnClickListener{
            var intent = Intent(this@MainActivity,SessionCreate::class.java);
            intent.putExtra("userId",userId);
            startActivity(intent);
        }
    }
}
