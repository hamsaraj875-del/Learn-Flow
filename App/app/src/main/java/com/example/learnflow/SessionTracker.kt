package com.example.learnflow

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.adapter.SessionAdapter
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class SessionTracker : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var sessionName: String

    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var sessionRepository: SessionRepository

    private lateinit var id: String

    private lateinit var txtTimer: TextView
    private lateinit var circularProgress: CircularProgressIndicator

    private lateinit var btnStart: View
    private lateinit var btnPause: View
    private lateinit var btnResume: View
    private lateinit var btnEnd: View

    private var plannedDuration = 0L

    private val studyDuration = 25 * 60 * 1000L
    private val breakDuration = 5 * 60 * 1000L

    private val startColor = Color.parseColor("#E53935") // red
    private val endColor = Color.parseColor("#43A047")   // green

    // How long the final (pure end color) frame stays visible before the next phase
    private val phaseHoldMillis = 600L
    private val tickInterval = 100L

    private var totalTime = 0L
    private var remainingTime = 0L

    private var startTime = 0.0
    private var remainingStudyTime = 0L
    private var currentStudyBlock = 0L

    private var isTimerRunning = false
    private var isBreakPhase = false
    private var sessionSaved = false

    private var countDownTimer: CountDownTimer? = null
    private var pendingTransition: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_tracker)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        userId = intent.getStringExtra("userId") ?: ""
        sessionName = intent.getStringExtra("sessionName") ?: ""
        plannedDuration = intent.getLongExtra("plannedDuration", 0L)
        id = intent.getStringExtra("id").toString()
        txtTimer = findViewById(R.id.txtTimer)
        circularProgress = findViewById(R.id.circularProgress)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        btnEnd = findViewById(R.id.btnEnd)
        val sessionRecyclerView = findViewById<RecyclerView>(R.id.sessionRecyclerView)
        sessionRepository = SessionRepository(RetrofitClient.api)
        sessionAdapter = SessionAdapter(mutableListOf()) { session ->
            sessionAdapter.removeSession(session)
        }
        sessionRecyclerView.layoutManager = LinearLayoutManager(this)
        sessionRecyclerView.adapter = sessionAdapter

        remainingStudyTime = plannedDuration * 60 * 1000L
        currentStudyBlock = minOf(studyDuration, remainingStudyTime)
        totalTime = currentStudyBlock
        remainingTime = currentStudyBlock
        circularProgress.max = 100
        circularProgress.progress = 0
        updateTimerDisplay()
        updateProgressColor()

        btnStart.setOnClickListener {
            if (startTime == 0.0) {
                startTime = System.currentTimeMillis().toDouble()
            }
            startTimer()
        }
        btnPause.setOnClickListener { pauseTimer() }
        btnResume.setOnClickListener { resumeTimer() }
        btnEnd.setOnClickListener { endTimer() }
        loadSessions()
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            try {
                val response = sessionRepository.sessionList(Request(userId))
                if (response.success) {
                    sessionAdapter.setSessions(response.sessionList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SessionTracker, "Failed to load sessions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer() {
        if (isTimerRunning || remainingStudyTime <= 0L) {
            return
        }
        if (isBreakPhase) {
            runTimer()
        } else {
            startStudyPhase()
        }
    }

    private fun startStudyPhase() {
        if (remainingStudyTime <= 0L) {
            finishSession()
            return
        }
        isBreakPhase = false
        currentStudyBlock = minOf(studyDuration, remainingStudyTime)
        totalTime = currentStudyBlock
        remainingTime = currentStudyBlock
        btnStart.visibility = View.GONE
        btnPause.visibility = View.VISIBLE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.VISIBLE
        updateTimerDisplay()
        updateProgressColor()
        runTimer()
    }

    private fun startBreakPhase() {
        isBreakPhase = true
        totalTime = breakDuration
        remainingTime = breakDuration
        btnStart.visibility = View.GONE
        btnPause.visibility = View.VISIBLE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.VISIBLE
        updateTimerDisplay()
        updateProgressColor()
        runTimer()
    }

    private fun runTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingTime, tickInterval) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateTimerDisplay()
                updateProgressColor()
            }

            override fun onFinish() {
                isTimerRunning = false
                remainingTime = 0L
                updateTimerDisplay()
                updateProgressColor()
                val transition = Runnable {
                    pendingTransition = null
                    if (sessionSaved) return@Runnable // user already ended the session
                    if (isBreakPhase) {
                        startStudyPhase()
                    } else {
                        remainingStudyTime -= currentStudyBlock
                        if (remainingStudyTime > 0L) {
                            startBreakPhase()
                        } else {
                            finishSession()
                        }
                    }
                }
                pendingTransition = transition
                txtTimer.postDelayed(transition, phaseHoldMillis)
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return
        countDownTimer?.cancel()
        isTimerRunning = false
        btnPause.visibility = View.GONE
        btnResume.visibility = View.VISIBLE
        btnEnd.visibility = View.VISIBLE
    }

    private fun resumeTimer() {
        if (isTimerRunning || remainingTime <= 0L) return
        btnPause.visibility = View.VISIBLE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.VISIBLE
        runTimer()
    }

    private fun cancelPendingTransition() {
        pendingTransition?.let { txtTimer.removeCallbacks(it) }
        pendingTransition = null
    }

    private fun endTimer() {
        cancelPendingTransition()
        countDownTimer?.cancel()
        isTimerRunning = false
        saveSession()
        btnStart.visibility = View.GONE
        btnPause.visibility = View.GONE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.GONE
        Toast.makeText(this, "Session ended", Toast.LENGTH_SHORT).show()
    }

    private fun finishSession() {
        cancelPendingTransition()
        countDownTimer?.cancel()
        isTimerRunning = false
        remainingStudyTime = 0L
        remainingTime = 0L
        updateTimerDisplay()
        updateProgressColor()
        saveSession()
        btnStart.visibility = View.GONE
        btnPause.visibility = View.GONE
        btnResume.visibility = View.GONE
        btnEnd.visibility = View.GONE
        Toast.makeText(this, "Session completed", Toast.LENGTH_SHORT).show()
    }

    private fun saveSession() {
        if(id.isEmpty()){
            Toast.makeText(this@SessionTracker,"Please wait! setting up the page",Toast.LENGTH_SHORT).show();
            return;
        }
        if (startTime == 0.0 || sessionSaved) return
        sessionSaved = true
        val plannedMillis = plannedDuration * 60 * 1000L
        val completedStudyMillis = plannedMillis - remainingStudyTime
        val currentBlockStudied = if (!isBreakPhase) {
            (currentStudyBlock - remainingTime).coerceAtLeast(0L)
        } else {
            0L
        }
        val studiedMillis = (completedStudyMillis + currentBlockStudied).coerceIn(0L, plannedMillis)
        val actualDuration = studiedMillis / 60000L
        val endTime = System.currentTimeMillis()
        val status = if (studiedMillis >= plannedMillis) {
            "completed"
        } else {
            "incomplete"
        }
        val session = Session(id, userId, sessionName, startTime, endTime, plannedDuration, actualDuration, status)
        lifecycleScope.launch {
            try {
                var response = sessionRepository.updateSession(session)
                if(response.success){
                    Toast.makeText(this@SessionTracker, "Session saved", Toast.LENGTH_SHORT).show()
                    loadSessions()
                }
            } catch (e: Exception) {
                sessionSaved = false
                Toast.makeText(this@SessionTracker, "Failed to save session", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (remainingTime + 999L) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        txtTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgressColor() {
        if (totalTime <= 0L) return
        val fraction = ((totalTime - remainingTime).toFloat() / totalTime).coerceIn(0f, 1f)
        circularProgress.progress = Math.round(fraction * 100)
        val color = if (isBreakPhase) {
            interpolateColor(endColor, startColor, fraction)
        } else {
            interpolateColor(startColor, endColor, fraction)
        }
        circularProgress.setIndicatorColor(color)
    }

    private fun interpolateColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val r = fraction.coerceIn(0f, 1f)
        val red = Math.round(Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * r)
        val green = Math.round(Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * r)
        val blue = Math.round(Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * r)
        return Color.rgb(red, green, blue)
    }

    override fun onDestroy() {
        cancelPendingTransition()
        countDownTimer?.cancel()
        super.onDestroy()
    }
}