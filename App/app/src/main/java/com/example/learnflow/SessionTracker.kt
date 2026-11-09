package com.example.learnflow

import android.animation.ArgbEvaluator
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionTracker : AppCompatActivity() {

    // Timer card
    private lateinit var txtTimer: TextView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var btnEnd: Button

    // Current session card
    private lateinit var statusChip: MaterialCardView
    private lateinit var txtSessionStatus: TextView
    private lateinit var txtSessionName: TextView
    private lateinit var sessionProgress: LinearProgressIndicator
    private lateinit var txtSessionPercent: TextView
    private lateinit var txtPlanned: TextView
    private lateinit var txtFocused: TextView
    private lateinit var txtStartedAt: TextView

    private lateinit var userId: String
    private lateinit var sessionName: String
    private lateinit var sessionId: String

    private var plannedDuration = 0L
    private var plannedMillis = 0L

    private var startTime = 0L
    private var endTime = 0L

    private val studyDuration = 25 * 60 * 1000L
    private val breakDuration = 5 * 60 * 1000L

    private var remainingStudyTime = 0L
    private var currentStudyBlock = 0L
    private var remainingTime = 0L

    private var isBreakPhase = false
    private var isTimerRunning = false
    private var sessionStarted = false
    private var sessionSaved = false
    private var isSaving = false

    private var timer: CountDownTimer? = null

    private lateinit var sessionRepository: SessionRepository

    // Colors
    private val colorRed = Color.parseColor("#E53935")
    private val colorAmber = Color.parseColor("#FFB300")
    private val colorGreen = Color.parseColor("#43A047")
    private val colorBlue = Color.parseColor("#29B6F6")
    private val colorGrey = Color.parseColor("#555555")
    private val argb = ArgbEvaluator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_tracker)

        txtTimer = findViewById(R.id.txtTimer)
        circularProgress = findViewById(R.id.circularProgress)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        btnEnd = findViewById(R.id.btnEnd)

        statusChip = findViewById(R.id.statusChip)
        txtSessionStatus = findViewById(R.id.txtSessionStatus)
        txtSessionName = findViewById(R.id.txtSessionName)
        sessionProgress = findViewById(R.id.sessionProgress)
        txtSessionPercent = findViewById(R.id.txtSessionPercent)
        txtPlanned = findViewById(R.id.txtPlanned)
        txtFocused = findViewById(R.id.txtFocused)
        txtStartedAt = findViewById(R.id.txtStartedAt)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        sessionRepository = SessionRepository(RetrofitClient.api)

        // Null-safe: getStringExtra(...).toString() would turn null into the text "null"
        userId = intent.getStringExtra("userId") ?: ""
        sessionName = intent.getStringExtra("sessionName") ?: ""
        sessionId = intent.getStringExtra("sessionId") ?: ""
        plannedDuration = intent.getLongExtra("plannedDuration", 0L)

        if (userId.isBlank() || sessionName.isBlank() || plannedDuration <= 0L) {
            Toast.makeText(this, "Invalid session details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        plannedMillis = plannedDuration * 60 * 1000L
        remainingStudyTime = plannedMillis

        circularProgress.max = 100
        txtSessionName.text = sessionName
        txtPlanned.text = "$plannedDuration min"

        updateTimerDisplay(remainingStudyTime)
        updateProgress()
        updateButtons()

        btnStart.setOnClickListener { startSession() }
        btnPause.setOnClickListener { pauseTimer() }
        btnResume.setOnClickListener { resumeTimer() }
        btnEnd.setOnClickListener { endTimer() }
    }

    private fun startSession() {
        if (sessionStarted || sessionSaved || isSaving) return

        sessionStarted = true
        startTime = System.currentTimeMillis()
        txtStartedAt.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startTime))

        startStudyPhase()
        updateButtons()
    }

    private fun startStudyPhase() {
        if (sessionSaved || isSaving) return
        isBreakPhase = false
        currentStudyBlock = minOf(studyDuration, remainingStudyTime)
        remainingTime = currentStudyBlock
        startCountdown(currentStudyBlock)
    }

    private fun startBreakPhase() {
        if (sessionSaved || isSaving) return
        isBreakPhase = true
        remainingTime = breakDuration
        startCountdown(breakDuration)
    }

    private fun startCountdown(duration: Long) {
        timer?.cancel()
        isTimerRunning = true
        updateButtons()
        timer = object : CountDownTimer(duration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                txtTimer.text = if (isBreakPhase) {
                    "Break\n${formatTime(millisUntilFinished)}"
                } else {
                    "Study\n${formatTime(millisUntilFinished)}"
                }
                updateProgress()
            }

            override fun onFinish() {
                remainingTime = 0L
                isTimerRunning = false
                if (isBreakPhase) {
                    startStudyPhase()
                } else {
                    remainingStudyTime =
                        (remainingStudyTime - currentStudyBlock).coerceAtLeast(0L)
                    if (remainingStudyTime <= 0L) {
                        remainingStudyTime = 0L
                        finishSession()
                    } else {
                        startBreakPhase()
                    }
                }
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return
        timer?.cancel()
        isTimerRunning = false
        updateButtons()
    }

    private fun resumeTimer() {
        if (isTimerRunning || sessionSaved || isSaving) return
        if (remainingTime <= 0L) return
        startCountdown(remainingTime)
    }

    private fun endTimer() {
        if (!sessionStarted || sessionSaved || isSaving) return
        timer?.cancel()
        isTimerRunning = false
        finishSession()
    }

    private fun finishSession() {
        if (sessionSaved || isSaving) return
        timer?.cancel()
        isTimerRunning = false
        endTime = System.currentTimeMillis()
        val completedNaturally = remainingStudyTime <= 0L
        saveSession(status = if (completedNaturally) "completed" else "incomplete")
        updateButtons()
    }

    /** Study time done so far, in milliseconds (breaks are not counted). */
    private fun actualStudyMillis(): Long {
        val completedStudyTime = plannedMillis - remainingStudyTime
        val currentBlockProgress = if (!isBreakPhase && sessionStarted) {
            (currentStudyBlock - remainingTime).coerceAtLeast(0L)
        } else {
            0L
        }
        return (completedStudyTime + currentBlockProgress).coerceIn(0L, plannedMillis)
    }

    private fun calculateActualDuration(): Long = actualStudyMillis() / 60000L

    private fun saveSession(status: String) {
        if (sessionSaved || isSaving) return
        if (sessionId.isBlank()) {
            Toast.makeText(this, "Setting up the session, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        if (startTime == 0L) return
        isSaving = true
        updateButtons()
        val actualDuration = calculateActualDuration()
        val session = Session(
            sessionId, userId, sessionName, startTime.toDouble(), endTime,
            plannedDuration, actualDuration, status
        )

        lifecycleScope.launch {
            try {
                val response = sessionRepository.updateSession(session)
                if (response.success) {
                    sessionSaved = true
                    Toast.makeText(this@SessionTracker, "Session saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SessionTracker, "Failed to save session. Press End to retry.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SessionTracker, "Network error. Press End to retry.", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
                updateButtons()
            }
        }
    }

    private fun progressColor(fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        return if (f < 0.5f) {
            argb.evaluate(f / 0.5f, colorRed, colorAmber) as Int
        } else {
            argb.evaluate((f - 0.5f) / 0.5f, colorAmber, colorGreen) as Int
        }
    }

    private fun updateProgress() {
        if (plannedMillis <= 0L) return

        val fraction = actualStudyMillis().toFloat() / plannedMillis
        val percent = (fraction * 100).toInt().coerceIn(0, 100)
        val color = progressColor(fraction)

        circularProgress.setProgressCompat(percent, true)
        circularProgress.setIndicatorColor(color)

        sessionProgress.setProgressCompat(percent, true)
        sessionProgress.setIndicatorColor(color)
        txtSessionPercent.text = "$percent% of planned time"

        updateSessionCard()
    }

    private fun updateSessionCard() {
        val (label, color) = when {
            sessionSaved -> "Saved" to colorGreen
            isSaving -> "Saving…" to colorBlue
            !sessionStarted -> "Not started" to colorGrey
            isTimerRunning && isBreakPhase -> "On break" to colorBlue
            isTimerRunning -> "Studying" to colorGreen
            else -> "Paused" to colorAmber
        }
        txtSessionStatus.text = label
        statusChip.setCardBackgroundColor(color)
        txtFocused.text = "${calculateActualDuration()} min"
    }

    private fun updateTimerDisplay(time: Long) {
        txtTimer.text = formatTime(time)
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun updateButtons() {
        val canResume = sessionStarted && !isTimerRunning && !sessionSaved
        val canEnd = sessionStarted && !sessionSaved

        btnStart.visibility = if (!sessionStarted) View.VISIBLE else View.GONE
        btnPause.visibility = if (isTimerRunning && !sessionSaved) View.VISIBLE else View.GONE
        btnResume.visibility = if (canResume) View.VISIBLE else View.GONE
        btnEnd.visibility = if (canEnd) View.VISIBLE else View.GONE

        btnStart.isEnabled = !isSaving
        btnPause.isEnabled = isTimerRunning && !isSaving
        btnResume.isEnabled = canResume && !isSaving
        btnEnd.isEnabled = canEnd && !isSaving

        updateSessionCard()
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
    }
}