package com.example.learnflow.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.R
import com.example.learnflow.data.model.TestCreate
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.TestRepository
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log

class TestCreator : AppCompatActivity() {

    private lateinit var etTopic: EditText
    private lateinit var etQuestionCount: EditText
    private lateinit var toggleQuestionType: MaterialButtonToggleGroup
    private lateinit var uploadDocumentContainer: LinearLayout
    private lateinit var tvSelectedFile: TextView
    private lateinit var btnCreateTest: FrameLayout
    private var testRepository = TestRepository(RetrofitClient.api)
    private lateinit var userId : String;
    private var selectedDocumentUri: Uri? = null
    private lateinit var txtCreateTest:TextView;
    private lateinit var progressCreateTest : ProgressBar;

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
        txtCreateTest = findViewById(R.id.txtCreateTest);
        progressCreateTest = findViewById(R.id.progressCreateTest);


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
            var questionType = when (toggleQuestionType.checkedButtonId) {
                R.id.btnMCQ -> "mcq"
                R.id.btnQuestionAnswer -> "question_answer"
                else -> ""
            }
            val selectedId = toggleQuestionType.checkedButtonId
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
                if (selectedId == R.id.btnMCQ) {
                    lifecycleScope.launch {
                        try{
                            txtCreateTest.text = "";
                            progressCreateTest.visibility = View.VISIBLE;
                            btnCreateTest.isEnabled = false;
                            val response = testRepository.createMcqTest(
                                TestCreate(
                                    userId,
                                    topic,
                                    questionCount
                                )
                            );
                            if(response.success){
                                var intent = Intent(this@TestCreator, McqTest::class.java);
                                intent.putExtra("questionList",ArrayList(response.questionList));
                                intent.putExtra("testId",response.testId);
                                intent.putExtra("userId",userId);
                                startActivity(intent);
                                finish();
                            }
                        }catch(e:Exception){
                            Toast.makeText(this@TestCreator,e.message,Toast.LENGTH_SHORT).show();
                        }finally{
                            txtCreateTest.text = "Create Test  →";
                            progressCreateTest.visibility = View.GONE;
                            btnCreateTest.isEnabled = true;
                        }
                    }
                }else{
                    lifecycleScope.launch {
                        try{
                            txtCreateTest.text = "";
                            progressCreateTest.visibility = View.VISIBLE;
                            btnCreateTest.isEnabled = false;
                            val response = testRepository.createQuestionTest(
                                TestCreate(
                                    userId,
                                    topic,
                                    questionCount
                                )
                            );
                            Toast.makeText(this@TestCreator,response.toString(),Toast.LENGTH_SHORT).show();
                            if(response.success){
                                var intent = Intent(this@TestCreator, WrittenTest::class.java);
                                intent.putExtra("questionList",ArrayList(response.questionList));
                                intent.putExtra("testId",response.testId);
                                intent.putExtra("userId",userId);
                                Toast.makeText(this@TestCreator,response.questionList.toString(),Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                                finish();
                            }
                        }catch (e: Exception) {
                            e.printStackTrace()

                            Toast.makeText(
                                this@TestCreator,
                                "${e.javaClass.simpleName}: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }finally{
                            txtCreateTest.text = "Create Test  →";
                            progressCreateTest.visibility = View.GONE;
                            btnCreateTest.isEnabled = true;
                        }

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

                if (selectedId == R.id.btnMCQ) {
                    lifecycleScope.launch {
                        try {
                            val questionCountBody = questionCount.toString().toRequestBody("text/plain".toMediaType())
                            val topicBody = topic.toRequestBody("text/plain".toMediaType())
                            val userIdBody = userId.toRequestBody("text/plain".toMediaType())
                            txtCreateTest.text = "";
                            progressCreateTest.visibility = View.VISIBLE;
                            btnCreateTest.isEnabled = false;
                            val response = testRepository.createMcqDocumentTest(
                                pdf,
                                questionCountBody,
                                topicBody,
                                userIdBody
                            )

                            if (response.success) {
                                var intent = Intent(this@TestCreator, McqTest::class.java);
                                intent.putExtra("questionList", ArrayList(response.questionList));
                                intent.putExtra("testId", response.testId);
                                intent.putExtra("userId", userId);
                                startActivity(intent);
                                finish();
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@TestCreator, e.message, Toast.LENGTH_SHORT).show();
                        } finally {
                            txtCreateTest.text = "Create Test  →";
                            progressCreateTest.visibility = View.GONE;
                            btnCreateTest.isEnabled = true;
                        }
                    }
                }else{
                    lifecycleScope.launch{
                            val questionCountBody = questionCount.toString().toRequestBody("text/plain".toMediaType())
                            val topicBody = topic.toRequestBody("text/plain".toMediaType())
                            val userIdBody = userId.toRequestBody("text/plain".toMediaType())
                            txtCreateTest.text = "";
                            progressCreateTest.visibility = View.VISIBLE;
                            btnCreateTest.isEnabled = false;
                            val response = testRepository.createWrittenDocumentTest(
                                pdf,
                                questionCountBody,
                                topicBody,
                                userIdBody
                            )
                            if (response.success) {
                                var intent = Intent(this@TestCreator, WrittenTest::class.java);
                                intent.putExtra("questionList", ArrayList(response.questionList));
                                intent.putExtra("testId", response.testId);
                                intent.putExtra("userId", userId);
                                startActivity(intent);
                                finish();
                            }
                            txtCreateTest.text = "Create Test  →";
                            progressCreateTest.visibility = View.GONE;
                            btnCreateTest.isEnabled = true;

                    }
                }
            }
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