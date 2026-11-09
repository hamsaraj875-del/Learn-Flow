package com.example.learnflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SessionCreate : AppCompatActivity() {

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_create)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editDuration = findViewById<EditText>(R.id.editDuration)
        val editSessionName = findViewById<EditText>(R.id.editSessionName)
        val sessionCreateBtn = findViewById<MaterialButton>(R.id.btnCreateSession)
        val createSessionLoader = findViewById<ProgressBar>(R.id.createSessionLoader)
        val btnChatHistory = findViewById<MaterialButton>(R.id.btnChatHistory);
        val chip25Min = findViewById<Chip>(R.id.chip25Min)
        val chip45Min = findViewById<Chip>(R.id.chip45Min)
        val chip60Min = findViewById<Chip>(R.id.chip60Min)
        val chip90Min = findViewById<Chip>(R.id.chip90Min)

        val sessionRepository = SessionRepository(RetrofitClient.api)
        btnChatHistory.visibility = View.GONE;

        userId = intent.getStringExtra("userId") ?: ""
        createSessionLoader.visibility = View.GONE

        chip25Min.setOnClickListener {
            editDuration.setText("25")
        }

        chip45Min.setOnClickListener {
            editDuration.setText("45")
        }

        chip60Min.setOnClickListener {
            editDuration.setText("60")
        }

        chip90Min.setOnClickListener {
            editDuration.setText("90")
        }

        lifecycleScope.launch{
            try{
                var response = sessionRepository.sessionList(Request(userId));
                if(response.success && !response.sessionList.isEmpty()){
                    btnChatHistory.visibility = View.VISIBLE;
                }
            }catch(e:Exception){
                println(e.message);
            }
        }

        btnChatHistory.setOnClickListener{
            var intent = Intent(this@SessionCreate,SessionHistory::class.java);
            intent.putExtra("userId",userId);
            startActivity(intent);
        }

        sessionCreateBtn.setOnClickListener {
            val sessionName = editSessionName.text.toString().trim()
            val duration = editDuration.text.toString().toLongOrNull()

            if (userId.isEmpty()) {
                Toast.makeText(this, "We are setting up please wait", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sessionName.isEmpty()) {
                editSessionName.error = "Enter session name"
                return@setOnClickListener
            }

            if (duration == null || duration <= 0L) {
                editDuration.error = "Enter a valid duration"
                return@setOnClickListener
            }

            createSessionLoader.visibility = View.VISIBLE
            sessionCreateBtn.text = ""

            val startTime = System.currentTimeMillis()

            lifecycleScope.launch {
                try {
                    val response = sessionRepository.createSession(
                        Session( "",userId, sessionName, startTime.toDouble(), 0L, duration, 0L, "pending"))

                    if (response.success) {
                        val sessionIntent = Intent(this@SessionCreate, SessionTracker::class.java)
                        sessionIntent.putExtra("userId", userId)
                        sessionIntent.putExtra("sessionId",response.mes);
                        sessionIntent.putExtra("sessionName", sessionName)
                        sessionIntent.putExtra("plannedDuration", duration)
                        sessionIntent.putExtra("startTime", startTime)
                        sessionIntent.putExtra("id",response.mes);
                        startActivity(sessionIntent)
                        finish();
                    }

                } catch (err: Exception) {
                    Toast.makeText(this@SessionCreate, err.message ?: "Something went wrong", Toast.LENGTH_SHORT).show()
                } finally {
                    createSessionLoader.visibility = View.GONE
                    sessionCreateBtn.text = "Create session →"
                }
            }
        }
    }
}