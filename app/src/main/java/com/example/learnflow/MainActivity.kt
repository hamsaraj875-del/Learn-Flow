package com.example.learnflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var btn : Button;
        var progressBar : ProgressBar;

        progressBar = findViewById(R.id.progressBar);
        btn = findViewById(R.id.btn);
        val chatRepository = ChatRepository(RetrofitClient.api)

        btn.visibility = View.GONE;

        lifecycleScope.launch{
            try{
                var response = chatRepository.userIsLoggedIn();
                if(!response.success){
                    var intent = Intent(this@MainActivity,Login::class.java);
                    startActivity(intent);
                }else{
                    progressBar.visibility = View.GONE;
                    btn.visibility = View.VISIBLE;
                }
            }catch(e:Exception){
                var intent = Intent(this@MainActivity,Login::class.java);
                startActivity(intent);
            }finally{
                progressBar.visibility = View.GONE;
            }
        }

        btn.setOnClickListener{
            var intent = Intent(this@MainActivity, Chat::class.java);
            startActivity(intent);
        }
    }
}