package com.example.learnflow.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.R
import com.example.learnflow.data.model.McqTestQuestion
import com.example.learnflow.data.model.TestResultSender
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.TestRepository
import kotlinx.coroutines.launch

class McqTest : AppCompatActivity() {

    private lateinit var questionList : ArrayList<McqTestQuestion>
    private var index : Int = -1;

    private lateinit var testId : String;
    private lateinit var userId : String;
    private lateinit var tvQuestionLabel : TextView;
    private lateinit var tvQuestion: TextView;
    private lateinit var btnPrevious: FrameLayout;
    private lateinit var btnNext : FrameLayout;
    private lateinit var tvPrevious: TextView;
    private lateinit var tvNext : TextView;
    private lateinit var optionA: RadioButton
    private lateinit var optionB: RadioButton
    private lateinit var optionC: RadioButton
    private lateinit var optionD: RadioButton
    private lateinit var rgOptions: RadioGroup
    private lateinit var progressBar : ProgressBar
    private lateinit var tvProgressPercent : TextView
    private lateinit var submitProgress:ProgressBar
    private var testRepository= TestRepository(RetrofitClient.api);
    private val userAnswers = mutableListOf<String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mcq_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvQuestionLabel = findViewById(R.id.tvQuestionLabel);
        tvQuestion = findViewById(R.id.tvQuestion);
        btnPrevious = findViewById(R.id.btnPrevious);
        tvPrevious = findViewById(R.id.tvPrevious);
        tvNext = findViewById(R.id.tvNext);
        submitProgress = findViewById(R.id.submitProgress);
        btnNext = findViewById(R.id.btnNext);
        optionA = findViewById(R.id.optionA)
        optionB = findViewById(R.id.optionB)
        optionC = findViewById(R.id.optionC)
        optionD = findViewById(R.id.optionD)
        rgOptions = findViewById(R.id.rgOptions)
        progressBar = findViewById(R.id.quizProgress);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);

        questionList = intent.getSerializableExtra("questionList") as? ArrayList<McqTestQuestion>
            ?: run {
                Toast.makeText(this, "No questions received", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        testId = intent.getStringExtra("testId").toString();
        userId = intent.getStringExtra("userId").toString();
        if (questionList.isEmpty()) {
            Toast.makeText(this, "No questions available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        userAnswers.addAll(List(questionList.size) { null })
        btnPrevious.visibility = View.GONE
        questionNext()
        btnNext.setOnClickListener{
            questionNext();
        }
        btnPrevious.setOnClickListener{
            questionPrev();
        }
    }

    private fun questionNext() {
        btnPrevious.visibility = View.VISIBLE;
        saveAnswer();
        if (index < questionList.size - 1) {
            index++
            val progress = ((index) * 100) / questionList.size
            tvProgressPercent.setText(progress.toString());
            progressBar.progress = progress
            if (index == questionList.size - 1) {
                tvNext.text = "Submit"
            }
            tvQuestionLabel.text = (index + 1).toString()
            tvQuestion.text = questionList[index].question
            val options = questionList[index].options
            optionA.text = options[0]
            optionB.text = options[1]
            optionC.text = options[2]
            optionD.text = options[3]
            rgOptions.clearCheck()
        } else {
            evaluateTest();
        }
    }
    private fun questionPrev() {
        if (index > 0) {
            saveAnswer()
            index--
            btnPrevious.visibility = View.VISIBLE
            tvQuestionLabel.text = (index + 1).toString()
            tvQuestion.text = questionList[index].question
            optionA.text = questionList[index].options[0]
            optionB.text = questionList[index].options[1]
            optionC.text = questionList[index].options[2]
            optionD.text = questionList[index].options[3]
            rgOptions.clearCheck()
            when (userAnswers[index]) {
                optionA.text.toString() -> optionA.isChecked = true
                optionB.text.toString() -> optionB.isChecked = true
                optionC.text.toString() -> optionC.isChecked = true
                optionD.text.toString() -> optionD.isChecked = true
            }
            tvNext.text = "Next"
            if (index == 0) {
                btnPrevious.visibility = View.GONE
            }
        }
    }
    private fun saveAnswer() {
        val selectedId = rgOptions.checkedRadioButtonId
        if (selectedId != -1) {
            val selectedOption = findViewById<RadioButton>(selectedId)
            userAnswers[index] = selectedOption.text.toString()
        }
    }
    private fun evaluateTest() {
        saveAnswer()
        var score = 0
        var wrong = 0
        for (i in questionList.indices) {
            if (userAnswers[i] != null) {
                if (userAnswers[i] == questionList[i].correctAnswer) {
                    score++
                } else {
                    wrong++
                }
            }
        }

        if(userId.isEmpty() || testId.isEmpty()){
            Toast.makeText(this@McqTest,"Please wait setting up your page",Toast.LENGTH_SHORT).show();
            return;
        }

        lifecycleScope.launch{
            try{
                btnNext.isEnabled = false;
                tvNext.text = " ";
                submitProgress.visibility = View.VISIBLE;
                var response = testRepository.testResult(TestResultSender(testId,score,wrong));
                if(response.success){
                    val unanswered = userAnswers.count { it == null }
                    val intent = Intent(this@McqTest, McqTestResult::class.java)
                    intent.putExtra("correct", score)
                    intent.putExtra("wrong", wrong)
                    intent.putExtra("unanswered", unanswered)
                    intent.putExtra("total", questionList.size)
                    startActivity(intent)
                    finish()
                }else{
                    Toast.makeText(this@McqTest,response.mes,Toast.LENGTH_SHORT).show();
                }
            }catch(e:Exception){
                Toast.makeText(this@McqTest,e.message,Toast.LENGTH_SHORT).show();
            }
            finally{
                tvNext.text = "Submit";
                submitProgress.visibility = View.GONE;
                btnNext.isEnabled = true;
            }

        }
    }

    private fun getUnansweredQuestions(): ArrayList<McqTestQuestion> {
        val unansweredQuestions = ArrayList<McqTestQuestion>()

        for (i in questionList.indices) {
            if (userAnswers[i] == null) {
                unansweredQuestions.add(questionList[i])
            }
        }

        return unansweredQuestions
    }
}