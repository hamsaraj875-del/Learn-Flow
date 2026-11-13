
package com.example.learnflow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.TestCreate
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.TestRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TestCreator : AppCompatActivity() {

    private lateinit var etTopic: EditText
    private lateinit var etQuestionCount: EditText
    private lateinit var toggleQuestionType: MaterialButtonToggleGroup
    private lateinit var uploadDocumentContainer: LinearLayout
    private lateinit var tvSelectedFile: TextView
    private lateinit var btnCreateTest: Button
    private var testRepository = TestRepository(RetrofitClient.api)
    private lateinit var userId : String;
    private var selectedDocumentUri: Uri? = null

    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedDocumentUri = uri
                val fileName = getFileName(uri)
                tvSelectedFile.text = "Selected: $fileName"
                tvSelectedFile.visibility = View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_creator)

        etTopic = findViewById(R.id.etTopic)
        etQuestionCount = findViewById(R.id.etQuestionCount)
        toggleQuestionType = findViewById(R.id.toggleQuestionType)
        uploadDocumentContainer = findViewById(R.id.uploadDocumentContainer)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        btnCreateTest = findViewById(R.id.btnCreateTest)


        userId = intent.getStringExtra("userId").toString();

        // Open PDF picker
        uploadDocumentContainer.setOnClickListener {
            documentPicker.launch(
                arrayOf("application/pdf")
            )
        }

        // Create Test
        btnCreateTest.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            val questionCount = etQuestionCount.text.toString().toIntOrNull()
            val questionType = when (toggleQuestionType.checkedButtonId) {
                R.id.btnMCQ -> "mcq"
                R.id.btnQuestionAnswer -> "question_answer"
                else -> ""
            }
            if (topic.isEmpty()) {
                etTopic.error = "Enter a topic"
                return@setOnClickListener
            }
            if (questionCount == null || questionCount <= 0) {
                etQuestionCount.error = "Enter a valid number"
                return@setOnClickListener
            }
            if (questionType.isEmpty()) {
                Toast.makeText(
                    this,
                    "Select a question type",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val pdfUri = selectedDocumentUri

            if(pdfUri==null){
                lifecycleScope.launch{
                    val response = testRepository.createTest(TestCreate(userId,topic,questionCount));
                    if(response.success){
                        var intent = Intent(this@TestCreator,Test::class.java);
                        intent.putExtra("questionList",ArrayList(response.questionList));
                        startActivity(intent);
                    }
                }
            } else{
                val pdfBytes = contentResolver.openInputStream(pdfUri)?.use {
                    it.readBytes()
                }
                if (pdfBytes == null) {
                    Toast.makeText(this, "Unable to read PDF", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val pdfBody = pdfBytes.toRequestBody(
                    "application/pdf".toMediaType()
                )

                val pdf = MultipartBody.Part.createFormData(
                    "pdf",
                    getFileName(pdfUri),
                    pdfBody
                )
                lifecycleScope.launch{
                    val response = testRepository.createDocumentTest(pdf,TestCreate(userId,topic,questionCount));
                }
            }

            Toast.makeText(
                this,
                "Test details validated successfully",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Selected document.pdf"
        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}
