package com.example.learnflow

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.data.adapter.ChatAdapter
import com.example.learnflow.data.adapter.MessageAdapter
import com.example.learnflow.data.model.ChatHistory
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.Message
import com.example.learnflow.data.model.MessageHistoryRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.sin

class Chat : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView

    private lateinit var chatRecyclerView:RecyclerView
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var chatAdapter : ChatAdapter

    private lateinit var conversationId:String

    private var typingAnimator: ValueAnimator? = null
    val chatRepository = ChatRepository(RetrofitClient.api)
    private var pdfBytes : ByteArray?=null
    private var selectedPdfName : String?=null

    private var selectedPdfUri: Uri? = null
    private val chatList = mutableListOf<ChatHistory>()

    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        var userId = intent.getStringExtra("userId");
        conversationId = intent.getStringExtra("conversationId")?:"" ;

        val btnSend = findViewById<ImageView>(R.id.btnSend)
        val editMessage = findViewById<EditText>(R.id.editMessage)
        val txtAttachmentName = findViewById<TextView>(R.id.txtAttachmentName)
        val btnRemoveAttachment = findViewById<ImageView>(R.id.btnRemoveAttachment)
        val attachmentContainer = findViewById<LinearLayout>(R.id.attachmentContainer)
        val typingIndicator = findViewById<View>(R.id.typingIndicator)
        val documentView = findViewById<View>(R.id.documentView)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnSidebar = findViewById<ImageButton>(R.id.btnSidebar)
        val btnCloseSidebar = findViewById<ImageButton>(R.id.btnCloseSidebar);
        val chatListLoader = findViewById<ProgressBar>(R.id.chatListLoader)


        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageAdapter = MessageAdapter(messages)
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter


        chatRecyclerView = findViewById(R.id.chatsRecyclerView)
        chatAdapter = ChatAdapter(chatList) { chat ->
            if (chat.conversationId != "0") {
                conversationId = chat.conversationId

                lifecycleScope.launch{
                    var response = chatRepository.getMessages(
                        MessageHistoryRequest(
                            userId,
                            conversationId
                        )
                    )
                    if (response.success) {
                        messages.clear()
                        messages.addAll(response.messageList)
                        messageAdapter.notifyDataSetChanged()
                        scrollToBottom()
                    }
                }
            }
        }
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        //calling new chat

        val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    handleSelectedDocument(uri,txtAttachmentName,attachmentContainer,documentView)
                }
            }

        btnSidebar.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        btnCloseSidebar.setOnClickListener{
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        btnAttach.setOnClickListener {
            documentPicker.launch(arrayOf("application/pdf"))
        }

        btnRemoveAttachment.setOnClickListener {
            selectedPdfUri = null
            attachmentContainer.visibility = View.GONE
            documentView.visibility = View.GONE
        }


        lifecycleScope.launch{
            var list:List<ChatHistory> = emptyList();
            chatRecyclerView.visibility = View.GONE;
            chatListLoader.visibility = View.VISIBLE;
            try{
                val response = chatRepository.chatList()

                if(response.success){
                    if(response.chatList.size==0){
                        list = listOf(ChatHistory("No Chat yet", "0", "0",));
                    }else{
                        list = response.chatList;
                    }
                }else{
                    list = listOf(ChatHistory("Error occurred, please try again!", "0", "0",))
                }
            }
            catch(e:Exception){
                list = listOf(ChatHistory("Error occurred, please try again!", "0", "0",))
            }finally{
                chatList.clear();
                chatList.addAll(list);
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.visibility = View.VISIBLE;
                chatListLoader.visibility = View.GONE;
            }
        }

        btnSend.setOnClickListener {
            val mes = editMessage.text.toString()
            if (mes.isBlank() && pdfBytes==null) {
                return@setOnClickListener
            }
            editMessage.setText("")

            if (pdfBytes != null) {
                messages.add(Message(true, mes, selectedPdfName))
                messageAdapter.notifyItemInserted(messages.lastIndex)
                attachmentContainer.visibility = View.GONE
                documentView.visibility = View.GONE
                scrollToBottom()
                val pdfBody = pdfBytes!!.toRequestBody(
                    "application/pdf".toMediaType()
                )
                val pdfPart = MultipartBody.Part.createFormData(
                    "pdf",
                    selectedPdfName ?: "document.pdf",
                    pdfBody
                )
                val questionBody = mes.toRequestBody(
                    "text/plain".toMediaType()
                )
                lifecycleScope.launch {
                    typingIndicator.visibility = View.VISIBLE
                    btnSend.isEnabled = false
                    startTypingAnimation()
                    try {
                        val response = chatRepository.uploadDocument(
                            pdfPart,
                            questionBody
                        )
                        messages.add(Message(false, response.response))
                        messageAdapter.notifyItemInserted(messages.lastIndex)
                        scrollToBottom()
                        pdfBytes = null
                        selectedPdfName = null
                        selectedPdfUri = null
                        selectedPdfName = ""
                        txtAttachmentName.text = ""
                    } catch (e: Exception) {
                        messages.removeAt(messages.lastIndex)
                        editMessage.setText(mes.toString())
                        attachmentContainer.visibility = View.VISIBLE
                        documentView.visibility = View.VISIBLE
                        messageAdapter.notifyItemRemoved(messages.lastIndex)
                        messages.add(
                            Message(
                                false,
                                "Error occurred while connecting to the server."
                            )
                        )
                        messageAdapter.notifyItemInserted(messages.lastIndex)
                        scrollToBottom()
                    } finally {
                        stopTypingAnimation()
                        btnSend.isEnabled = true
                        typingIndicator.visibility = View.GONE
                    }
                }
            }else{
                messages.add(Message(true, mes))
                messageAdapter.notifyItemInserted(messages.lastIndex)
                scrollToBottom()
                lifecycleScope.launch {
                    typingIndicator.visibility = View.VISIBLE
                    btnSend.isEnabled = false
                    startTypingAnimation()
                    try {
                        val response = chatRepository.sendMessage(
                            ChatRequest(
                                userId =userId,
                                conversationId = conversationId,
                                question = mes
                            )
                        )
                        messages.add(
                            Message(
                                false,
                                response.response
                            )
                        )
                        messageAdapter.notifyItemInserted(messages.lastIndex)
                        scrollToBottom()
                    } catch (e: Exception) {
                        editMessage.setText(mes.toString())
                        messages.removeAt(messages.lastIndex)
                        messageAdapter.notifyItemRemoved(messages.lastIndex)
                        messages.add(
                            Message(
                                false,
                                "Error occurred while connecting to the server."
                            )
                        )
                        messageAdapter.notifyItemInserted(messages.lastIndex)
                        scrollToBottom()
                    } finally {
                        stopTypingAnimation()
                        btnSend.isEnabled = true
                        typingIndicator.visibility = View.GONE
                    }
                }

            }

        }
    }

    private fun scrollToBottom() {
        messageRecyclerView.post {
            messageRecyclerView.smoothScrollToPosition(messages.lastIndex)
        }
    }

    private fun startTypingAnimation() {

        val dots = listOf(
            findViewById<View>(R.id.dot1),
            findViewById<View>(R.id.dot2),
            findViewById<View>(R.id.dot3)
        )
        val secondaryColor = getColor(R.color.dark_text_secondary)
        val orangeColor = getColor(R.color.orange)
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                dots.forEachIndexed { index, dot ->
                    val offset = index * 0.2f
                    val value = (progress + offset) % 1f
                    val wave = sin(value * Math.PI).toFloat()
                    dot.scaleX = 1f + (0.25f * wave)
                    dot.scaleY = 1f + (0.25f * wave)
                    dot.alpha = 0.45f + (0.55f * wave)
                    dot.translationY = -6f * wave
                    dot.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.animation.ArgbEvaluator().evaluate(
                                wave,
                                secondaryColor,
                                orangeColor
                            ) as Int
                        )
                }
            }
        }
        typingAnimator = animator
        animator.start()
    }

    private fun stopTypingAnimation() {
        typingAnimator?.cancel()
        typingAnimator = null
        val dots = listOf(
            findViewById<View>(R.id.dot1),
            findViewById<View>(R.id.dot2),
            findViewById<View>(R.id.dot3)
        )
        val secondaryColor = getColor(R.color.dark_text_secondary)
        dots.forEach { dot ->
            dot.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .alpha(1f)
                .setDuration(150)
                .start()
            dot.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    secondaryColor
                )
        }
    }
    override fun onDestroy() {
        typingAnimator?.cancel()
        typingAnimator = null
        super.onDestroy()
    }

    private fun handleSelectedDocument(uri: Uri,txtAttachmentName:TextView,attachmentContainer:LinearLayout,documentView:View) {
        selectedPdfUri = uri

        if(uri!=null){
            pdfBytes = getPdfBytes(uri)
            if(pdfBytes!=null){
                Toast.makeText(this@Chat,"Size of the pdf ${pdfBytes!!.size}", Toast.LENGTH_SHORT).show()
            }
        }

        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        val fileName = cursor?.use {
            if (it.moveToFirst()) {
                it.getString(0)
            } else {
                "Selected document.pdf"
            }
        } ?: "Selected document.pdf"

        selectedPdfName = fileName
        txtAttachmentName.text = fileName
        attachmentContainer.visibility = View.VISIBLE
        documentView.visibility = View.VISIBLE
    }

    private fun getPdfBytes(uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    }

    //new chat creation

}