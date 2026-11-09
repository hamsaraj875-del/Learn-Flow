package com.example.learnflow

import android.content.Intent
import android.os.Bundle
import android.view.PixelCopy.request
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.adapter.SessionAdapter
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.LearnFlowApi
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import okhttp3.internal.notify

annotation class Sessionrepository

class SessionHistory : AppCompatActivity() {

    private lateinit var sessionRecyclerView: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var userId:String;

    private lateinit var totalSession:TextView;
    private lateinit var completedSession:TextView;
    private lateinit var learningStatus:TextView;
    private lateinit var progressIndicator: LinearProgressIndicator;
    private lateinit var txtCompletionPercentage:TextView;

    private val sessionList = mutableListOf<Session>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        userId = intent.getStringExtra("userId").toString();
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            var intent = Intent(this@SessionHistory, MainActivity::class.java);
            startActivity(intent);
        }

        totalSession = findViewById<TextView>(R.id.txtTotalSessions);
        completedSession = findViewById<TextView>(R.id.txtCompletedSessions);
        learningStatus = findViewById<TextView>(R.id.txtLearningStatus);
        progressIndicator = findViewById<LinearProgressIndicator>(R.id.overallProgressBar);
        txtCompletionPercentage = findViewById(R.id.txtCompletionPercentage);

        var sessionRepository = SessionRepository(RetrofitClient.api);

        sessionRecyclerView = findViewById(R.id.sessionRecyclerView)
        sessionAdapter = SessionAdapter(
            sessions = sessionList,
            onDelete = { session ->
            }
        )
        sessionRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SessionHistory)
            adapter = sessionAdapter
            setHasFixedSize(true)
        }
        fetchSessions(sessionRepository,userId)
    }


    private fun fetchSessions(sessionRepository:SessionRepository, userId:String) {
        lifecycleScope.launch{
            try{
                var response = sessionRepository.sessionList(Request(userId));
                if(response.success){
                    sessionAdapter.setSessions(response.sessionList);

                    var completed = 0
                    var actualDuration = 0L
                    var plannedDuration = 0L

                    for (session in response.sessionList) {
                        if (session.status.equals("completed", ignoreCase = true)) {
                            completed++
                        }

                        actualDuration += session.actualDuration
                        plannedDuration += session.plannedDuration
                    }

                    val percentage = if (plannedDuration > 0) {
                        ((actualDuration * 100) / plannedDuration).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    val status = when {
                        percentage >= 95 -> "Very Impressive"
                        percentage >= 85 -> "Very Good"
                        percentage >= 75 -> "Good"
                        else -> "You can do Better"
                    }

                    txtCompletionPercentage.text = percentage.toString()+"%";
                    totalSession.text = response.sessionList.size.toString()
                    completedSession.text = completed.toString()
                    learningStatus.text = status
                    progressIndicator.max = 100
                    progressIndicator.setProgress(percentage, true)

                }
            }
            catch(e:Exception){
                println(e.message);
            }
        }
    }

}