package com.example.learnflow

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.adapter.SessionAdapter
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class SessionTracker : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var sessionAdapter: SessionAdapter

    private lateinit var txtTimer: TextView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var btnStart: MaterialButton
    private lateinit var btnPause: MaterialButton
    private lateinit var btnResume: MaterialButton
    private lateinit var btnEnd: MaterialButton

    private var countDownTimer: CountDownTimer? = null
    private var totalTime = 25 * 60 * 1000L
    private var remainingTime = totalTime
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_tracker)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtTimer = findViewById(R.id.txtTimer)
        circularProgress = findViewById(R.id.circularProgress)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        btnEnd = findViewById(R.id.btnEnd)
        btnStart.setOnClickListener {
            startTimer()
        }
        btnPause.setOnClickListener {
            pauseTimer()
        }
        btnResume.setOnClickListener {
            resumeTimer()
        }
        btnEnd.setOnClickListener {
            endTimer()
        }
        updateTimerDisplay()
        circularProgress.progress = 0
        val sessionRepository = SessionRepository(RetrofitClient.api)
        sessionAdapter = SessionAdapter(mutableListOf()) { session ->
            sessionAdapter.removeSession(session)

            // Call delete API here
        }
        val sessionRecyclerView = findViewById<RecyclerView>(R.id.sessionRecyclerView)
        sessionRecyclerView.layoutManager = LinearLayoutManager(this)
        sessionRecyclerView.adapter = sessionAdapter
        userId = intent.getStringExtra("userId") ?: ""
        lifecycleScope.launch {
            try {
                val response = sessionRepository.sessionList(Request(userId))
                if (response.success) {
                    sessionAdapter.setSessions(response.sessionList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return
        if (remainingTime <= 0) {
            remainingTime = totalTime
        }
        btnStart.visibility = View.GONE
        btnPause.visibility = View.VISIBLE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.VISIBLE
        runTimer()
    }

    private fun runTimer() {
        countDownTimer?.cancel()
        isTimerRunning = true
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateTimerDisplay()
                val progress = 100 - (remainingTime * 100 / totalTime).toInt()
                circularProgress.progress = progress
                updateProgressColor(progress)
            }

            override fun onFinish() {
                remainingTime = 0
                isTimerRunning = false
                txtTimer.text = "00:00"
                circularProgress.progress = 0
                btnStart.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
                btnResume.visibility = View.GONE
                btnEnd.visibility = View.GONE
                Toast.makeText(
                    this@SessionTracker,
                    "Study session completed!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return
        countDownTimer?.cancel()
        isTimerRunning = false
        btnPause.visibility = View.GONE
        btnResume.visibility = View.VISIBLE
    }

    private fun resumeTimer() {
        if (isTimerRunning || remainingTime <= 0) return
        btnResume.visibility = View.GONE
        btnPause.visibility = View.VISIBLE
        runTimer()
    }

    private fun endTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isTimerRunning = false
        remainingTime = totalTime
        updateTimerDisplay()
        circularProgress.progress = 100
        btnStart.visibility = View.VISIBLE
        btnPause.visibility = View.GONE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.GONE
    }

    private fun updateTimerDisplay() {
        val minutes = remainingTime / 60000
        val seconds = (remainingTime % 60000) / 1000
        txtTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgressColor(progress: Int) {

        val startColor = android.graphics.Color.rgb(255, 87, 34)
        val middleColor = android.graphics.Color.rgb(255, 193, 7)
        val endColor = android.graphics.Color.rgb(76, 175, 80)

        val color = when {
            progress < 50 -> {
                android.animation.ArgbEvaluator().evaluate(
                    progress / 50f,
                    startColor,
                    middleColor
                ) as Int
            }
            else -> {
                android.animation.ArgbEvaluator().evaluate(
                    (progress - 50) / 50f,
                    middleColor,
                    endColor
                ) as Int
            }
        }

        circularProgress.setIndicatorColor(color)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}