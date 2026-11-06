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
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import com.example.learnflow.data.repository.SessionRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SessionCreate : AppCompatActivity() {
    private lateinit var userId:String;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_create)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var editDuration = findViewById<EditText>(R.id.editDuration);
        var editSessionName = findViewById<EditText>(R.id.editSessionName);
        var sessionCreateBtn = findViewById<MaterialButton>(R.id.btnCreateSession);
        var createSessionLoader = findViewById<ProgressBar>(R.id.createSessionLoader);
        var chip25Min = findViewById<Chip>(R.id.chip25Min);
        var chip45Min = findViewById<Chip>(R.id.chip45Min);
        var chip60Min = findViewById<Chip>(R.id.chip60Min);
        var chip90Min = findViewById<Chip>(R.id.chip90Min);

        var sessionRepository = SessionRepository(RetrofitClient.api);

        userId = intent.getStringExtra("userId")?:""
        createSessionLoader.visibility = View.GONE;


        chip25Min.setOnClickListener{
            editDuration.setText("25");
        }

        chip45Min.setOnClickListener{
            editDuration.setText("45");
        }

        chip60Min.setOnClickListener{
            editDuration.setText("60");
        }

        chip90Min.setOnClickListener{
            editDuration.setText("90");
        }

        sessionCreateBtn.setOnClickListener{
            createSessionLoader.visibility = View.VISIBLE;
            sessionCreateBtn.text = "";
            var startTime = System.currentTimeMillis();
            if(userId.isEmpty()){
                Toast.makeText(this@SessionCreate,"We are setting up please wait",Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }
            lifecycleScope.launch{
                try{
                    var response = sessionRepository.createSession(Session(userId,editSessionName.text.toString(),startTime,0,editDuration.text.toString().toLong(),0,"pending"));
                    if(response.success){
                        intent = Intent(this@SessionCreate,SessionTracker::class.java);
                        intent.putExtra("userId",userId);
                        startActivity(intent);
                    }
                    Toast.makeText(this@SessionCreate,response.mes, Toast.LENGTH_SHORT).show();
                }catch(err:Exception){
                    println(err);
                    Toast.makeText(this@SessionCreate,err.message,Toast.LENGTH_SHORT).show();
                }finally{
                    createSessionLoader.visibility = View.GONE;
                    sessionCreateBtn.text = "Create session →";
                }
            }
        }
    }
}