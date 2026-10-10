package com.example.learnflow

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlin.math.sin

class Chat : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter

    private var typingAnimator: ValueAnimator? = null
    private var pdfBytes : ByteArray?=null

    private var selectedPdfUri: Uri? = null

    private val messages = mutableListOf<MessageAdapterModel>()

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

        val btnSend = findViewById<ImageView>(R.id.btnSend)
        val editMessage = findViewById<EditText>(R.id.editMessage)
        val txtAttachmentName = findViewById<TextView>(R.id.txtAttachmentName);
        val btnRemoveAttachment = findViewById<ImageView>(R.id.btnRemoveAttachment);
        val attachmentContainer = findViewById<LinearLayout>(R.id.attachmentContainer);
        val typingIndicator = findViewById<View>(R.id.typingIndicator)
        val documentView = findViewById<View>(R.id.documentView);
        val chatRepository = ChatRepository(RetrofitClient.api)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)

        recyclerView = findViewById(R.id.recyclerView)
        messageAdapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter



        val documentPicker =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    handleSelectedDocument(uri,txtAttachmentName,attachmentContainer,documentView);
                }
            }

        btnAttach.setOnClickListener {
            documentPicker.launch(arrayOf("application/pdf"))
        }

        btnRemoveAttachment.setOnClickListener {
            selectedPdfUri = null
            attachmentContainer.visibility = View.GONE
            documentView.visibility = View.GONE;
        }

        btnSend.setOnClickListener {
            val mes = editMessage.text.toString()
            if (mes.isBlank()) {
                return@setOnClickListener
            }
            messages.add(
                MessageAdapterModel(
                    true,
                    mes
                )
            )
            editMessage.setText("")
            messageAdapter.notifyItemInserted(messages.lastIndex)
            scrollToBottom()
            lifecycleScope.launch {
                typingIndicator.visibility = View.VISIBLE
                btnSend.isEnabled = false;
                startTypingAnimation()
                try {
                    val response = chatRepository.sendMessage(
                        ChatRequest(
                            conversationId = 1,
                            question = mes
                        )
                    )
                    messages.add(
                        MessageAdapterModel(
                            false,
                            response.response
                        )
                    )
                    messageAdapter.notifyItemInserted(messages.lastIndex)
                    scrollToBottom()
                } catch (e: Exception) {
                    messages.add(
                        MessageAdapterModel(
                            false,
                            "Error occurred while connecting to the server."
                        )
                    )
                    messageAdapter.notifyItemInserted(messages.lastIndex)
                    scrollToBottom();
                } finally {
                    stopTypingAnimation()
                    btnSend.isEnabled = true;
                    typingIndicator.visibility = View.GONE
                }
            }
        }
    }

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(messages.lastIndex)
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
            pdfBytes = getPdfBytes(uri);
            if(pdfBytes!=null){
                Toast.makeText(this@Chat,"Size of the pdf ${pdfBytes!!.size}", Toast.LENGTH_SHORT).show();
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
        txtAttachmentName.text = fileName
        attachmentContainer.visibility = View.VISIBLE
        documentView.visibility = View.VISIBLE;
    }

    private fun getPdfBytes(uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    }
}