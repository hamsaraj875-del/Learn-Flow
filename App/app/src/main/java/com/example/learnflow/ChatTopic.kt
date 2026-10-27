package com.example.learnflow

import android.content.Intent
import android.graphics.Color
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
import com.example.learnflow.data.model.NewChatRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ChatTopic : AppCompatActivity() {

    private lateinit var conversationId:String;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_topic)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var cardProgramming = findViewById<MaterialCardView>(R.id.cardProgramming);
        var cardScience = findViewById<MaterialCardView>(R.id.cardScience);
        var cardAnything = findViewById<MaterialCardView>(R.id.cardAnything);
        var startChat = findViewById<MaterialButton>(R.id.btnStartChat);
        var editChatName = findViewById<EditText>(R.id.editChatName);
        var loader = findViewById<ProgressBar>(R.id.startChatLoader);
        var btnChatHistory = findViewById<MaterialButton>(R.id.btnChatHistory);

        val chatRepository = ChatRepository(RetrofitClient.api);
        btnChatHistory.visibility = View.GONE;

        var userId = intent.getStringExtra("userId");
        val cards = listOf(cardProgramming,cardScience,cardAnything);
        var topic = "anything"

        loader.visibility = View.GONE;

        fun select(selected: MaterialCardView) {
            cards.forEach { it.strokeColor = Color.parseColor("#2A2A2A") }
            selected.strokeColor = Color.parseColor("#FF8A00")

            topic = when (selected.id) {
                R.id.cardProgramming -> "programming"
                R.id.cardScience -> "science"
                else -> "anything"
            }
        }

        cards.forEach { card -> card.setOnClickListener { select(card) } }
        select(findViewById(R.id.cardAnything))


        lifecycleScope.launch{
            try{
                var response = chatRepository.getLastConversation(userId);
                if(response.success){
                    conversationId = response.conversationId;
                    btnChatHistory.visibility = View.VISIBLE;
                }
            }
            catch(e:Exception){
                print(e);
            }
        }

        btnChatHistory.setOnClickListener{
            intent = Intent(this@ChatTopic,Chat::class.java);
            intent.putExtra("userId",userId);
            intent.putExtra("conversationId",conversationId);
            startActivity(intent);
        }

        startChat.setOnClickListener{
            var chatName = editChatName.text.toString();
            if(chatName == ""){
                Toast.makeText(this@ChatTopic,"Enter the chat name",Toast.LENGTH_SHORT).show();
            }else{
                lifecycleScope.launch{
                    startChat.isEnabled = false;
                    loader.visibility = View.VISIBLE;
                    startChat.text = "";
                    try{
                        var response = chatRepository.createNewChat(NewChatRequest(userId,topic,chatName));
                        if(response.success){
                            conversationId = response.conversationId;
                            var intent = Intent(this@ChatTopic,Chat::class.java);
                            intent.putExtra("conversationId",conversationId);
                            intent.putExtra("userId",userId);
                            startActivity(intent);
                        }else{
                            finish();
                            Toast.makeText(this@ChatTopic,"Unable to create a new chat ",Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch(e:Exception){
                        Toast.makeText(this@ChatTopic,"Internal server error ,try again",Toast.LENGTH_SHORT).show();
                    }finally{
                        startChat.isEnabled = true;
                        loader.visibility = View.GONE;
                        startChat.text = "Start learning →";
                    }
                }
            }
        }

    }
}