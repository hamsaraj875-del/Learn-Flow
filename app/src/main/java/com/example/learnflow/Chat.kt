package com.example.learnflow

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class Chat : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
         }

        var name : TextView;
        var progressBar : ProgressBar;
        val chatRepository = ChatRepository(RetrofitClient.api)

        progressBar = findViewById(R.id.progressBar);
        name = findViewById(R.id.name);


        progressBar.visibility = ProgressBar.VISIBLE
        name.text = "Fetching..."

        lifecycleScope.launch {
            try {
                val response = chatRepository.sendMessage(
                    ChatRequest(conversationId="1",question="who is the ceo of google")
                )
                name.text = response.toString();
            } catch (e: Exception) {
                e.printStackTrace()
                name.text = "${e.javaClass.simpleName}: ${e.message}"
            }finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }
}