package com.example.learnflow

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.text.TextWatcher
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class Chat : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter

    private val messages = mutableListOf<MessageAdapterModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
         }

        var btnSend : ImageView;
        var editMessage : EditText;
        var chatRepository = ChatRepository(RetrofitClient.api);
        var name : TextView;

        btnSend = findViewById(R.id.btnSend);
        editMessage = findViewById(R.id.editMessage);
        name = findViewById(R.id.name);


        recyclerView = findViewById(R.id.recyclerView)
        messageAdapter = MessageAdapter(messages);
        recyclerView.layoutManager = LinearLayoutManager(this);
        recyclerView.adapter = messageAdapter;


        btnSend.setOnClickListener{
            messages.add(MessageAdapterModel(true, editMessage.text.toString()))
            messageAdapter.notifyItemInserted(messages.lastIndex)
            name.text = "fetching";
            lifecycleScope.launch{
                try{
                    var response = chatRepository.sendMessage(ChatRequest(1,editMessage.text.toString()));
                    messages.add(MessageAdapterModel(false, response.response.toString()))
                    messageAdapter.notifyItemInserted(messages.lastIndex)
                }catch(e:Exception){
                    messages.add(MessageAdapterModel(false,"Error occured while trying to connect"));
                    name.text = e.toString();
                }
            }
        }

    }
}
