package com.example.learnflow.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.R
import com.example.learnflow.data.model.WrittenTestQuestion
import com.example.learnflow.data.model.WrittenTestQuestionList
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.TestRepository
import kotlinx.coroutines.launch

class WrittenTest : AppCompatActivity() {
    private lateinit var questionList : ArrayList<WrittenTestQuestion>;
    private lateinit var testId:String;
    private lateinit var userId:String;
    private lateinit var tvQuestionLabel : TextView;
    private lateinit var tvQuestion : TextView;
    private lateinit var etWrittenTest: EditText;
    private lateinit var btnPrevious : FrameLayout
    private lateinit var btnNext : FrameLayout;
    private lateinit var tvNext : TextView;
    private lateinit var tvProgressPercent:TextView;
    private lateinit var submitProgress : ProgressBar;
    private lateinit var progressBar: ProgressBar;
    private lateinit var tvQuestionNumber:TextView;
    private var testRepository = TestRepository(RetrofitClient.api)
    private var index = 0;
    private lateinit var answer : MutableList<WrittenTestQuestion>;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_written_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvQuestionLabel = findViewById(R.id.tvQuestionLabel);
        tvQuestion = findViewById(R.id.tvQuestion);
        etWrittenTest = findViewById(R.id.etWrittenAnswer);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvNext = findViewById(R.id.tvNext);
        progressBar = findViewById(R.id.quizProgress);
        submitProgress = findViewById(R.id.submitProgress);
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);


        val receivedList = intent.getSerializableExtra("questionList") as? ArrayList<*>
        if (receivedList.isNullOrEmpty()) {
            Toast.makeText(this, "No questions received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        questionList = ArrayList(receivedList.filterIsInstance<WrittenTestQuestion>())
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Invalid question data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        testId = intent.getStringExtra("testId").toString();
        userId = intent.getStringExtra("userId").toString();

        displayQuestion();
        btnNext.setOnClickListener{
            questionNext();
        }
        btnPrevious.setOnClickListener{
            questionPrev();
        }
        btnPrevious.visibility = View.GONE;
    }

    private fun questionNext() {
        btnPrevious.visibility = View.VISIBLE;
        saveAnswer();
        if (index < questionList.size - 1) {
            index++;
            displayQuestion();
        } else {
            evaluateTest();
        }
    }

    private fun displayQuestion(){
        if(index== questionList.size-1){
            tvNext.setText("Submit");
        }
        tvQuestionLabel.text = "QUESTION "+(index+1).toString();
        tvQuestionNumber.text = (index+1).toString()+" of "+questionList.size;
        tvProgressPercent.text = (((index + 1) * 100) / questionList.size).toString();
        progressBar.setProgress(((index + 1) * 100) / questionList.size);
        tvQuestionLabel.text = (index+1).toString();
        tvQuestion.text = questionList.get(index).question;
        etWrittenTest.setText(questionList.get(index).answer.toString());
    }

    private fun saveAnswer(){
        if (index in questionList.indices) {
            questionList[index].answer = etWrittenTest.text.toString()
        }
    }

    private fun questionPrev() {
        saveAnswer();
        if(index>0){
            index--;
            tvQuestionLabel.text = "QUESTION "+(index+1).toString();
            tvQuestionNumber.text = index.toString()+" of "+questionList.size;
            displayQuestion();
        }
        if(index == 0){
            btnPrevious.visibility = View.GONE;
        }
    }

    private fun evaluateTest(){
        lifecycleScope.launch{
            try{
                submitProgress.visibility = View.VISIBLE;
                tvNext.text = "";
                btnNext.isEnabled = false;
                var response = testRepository.writtenTestResult(WrittenTestQuestionList(true,testId,questionList));
                if (response.success) {
                    Toast.makeText(
                        this@WrittenTest,
                        "${response.correct} ${response.wrong}",
                        Toast.LENGTH_SHORT
                    ).show()

                    val correct = response.correct.toInt()
                    val wrong = response.wrong.toInt()
                    val unanswered = questionList.size - correct - wrong;
                    val intent = Intent(this@WrittenTest, WrittenTestResult::class.java)
                    intent.putExtra("correct", correct)
                    intent.putExtra("wrong", wrong)
                    intent.putExtra("totalMarks",response.total);
                    intent.putExtra("unanswered", unanswered)
                    startActivity(intent)
                    finish()
                }
            }catch(e:Exception){
                Toast.makeText(this@WrittenTest,e.message,Toast.LENGTH_SHORT).show();
            }
            finally{
                btnNext.isEnabled = true;
                submitProgress.visibility = View.GONE;
                tvNext.text = "Submit";
            }
        }
    }
}