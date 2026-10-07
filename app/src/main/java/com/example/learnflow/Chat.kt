package com.example.learnflow

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private lateinit var handler: android.os.Handler
    private var typingRunnable: Runnable? = null

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

        handler = android.os.Handler(mainLooper)

        var btnSend : ImageView;
        var editMessage : EditText;
        var chatRepository = ChatRepository(RetrofitClient.api);
        var name : TextView;

        btnSend = findViewById(R.id.btnSend);
        editMessage = findViewById(R.id.editMessage);
        name = findViewById(R.id.name);


        val typingIndicator = findViewById<View>(R.id.typingIndicator)
        recyclerView = findViewById(R.id.recyclerView)
        messageAdapter = MessageAdapter(messages);
        recyclerView.layoutManager = LinearLayoutManager(this);
        recyclerView.adapter = messageAdapter;

        btnSend.setOnClickListener{
            messages.add(MessageAdapterModel(true, editMessage.text.toString()))
            var mes = editMessage.text.toString();
            editMessage.setText("");
            messageAdapter.notifyItemInserted(messages.lastIndex);

            lifecycleScope.launch{
                typingIndicator.visibility = View.VISIBLE
                startTypingAnimation()
                try{
                    var response = chatRepository.sendMessage(ChatRequest(1,mes));
                    messages.add(MessageAdapterModel(false, response.response.toString()))
                    messageAdapter.notifyItemInserted(messages.lastIndex)
                }catch(e:Exception){
                    messages.add(MessageAdapterModel(false,"Error occured while trying to connect"));
                    name.text = e.toString();
                }finally{
                    stopTypingAnimation()
                    typingIndicator.visibility = View.GONE
                }
            }
        }
    }

    private fun startTypingAnimation() {
        val dots = listOf(
            findViewById<View>(R.id.dot1),
            findViewById<View>(R.id.dot2),
            findViewById<View>(R.id.dot3)
        )
        var current = 0
        typingRunnable = object : Runnable {
            override fun run() {
                dots.forEach {
                    it.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            getColor(R.color.dark_text_secondary)
                        )
                }
                dots[current].backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        getColor(R.color.orange)
                    )
                current = (current + 1) % dots.size
                handler.postDelayed(this, 400)
            }
        }
        handler.post(typingRunnable!!)
    }
    private fun stopTypingAnimation() {
        typingRunnable?.let {
            handler.removeCallbacks(it)
        }
        typingRunnable = null
    }
}
