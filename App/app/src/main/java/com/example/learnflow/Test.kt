package com.example.learnflow

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.learnflow.data.model.TestQuestion
import kotlin.properties.Delegates

class Test : AppCompatActivity() {

    private lateinit var questionList : ArrayList<TestQuestion>
    private var index : Int = -1;
    private lateinit var tvQuestionLabel : TextView;
    private lateinit var tvQuestion:TextView;
    private lateinit var btnPrevious: FrameLayout;
    private lateinit var btnNext : FrameLayout;
    private lateinit var tvPrevious:TextView;
    private lateinit var tvNext : TextView;
    private lateinit var optionA: RadioButton
    private lateinit var optionB: RadioButton
    private lateinit var optionC: RadioButton
    private lateinit var optionD: RadioButton
    private lateinit var rgOptions: RadioGroup
    private val userAnswers = mutableListOf<String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test)
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
        btnNext = findViewById(R.id.btnNext);
        optionA = findViewById(R.id.optionA)
        optionB = findViewById(R.id.optionB)
        optionC = findViewById(R.id.optionC)
        optionD = findViewById(R.id.optionD)
        rgOptions = findViewById(R.id.rgOptions)

        questionList = intent.getSerializableExtra("questionList") as? ArrayList<TestQuestion>
            ?: run {
                Toast.makeText(this, "No questions received", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
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
        for (i in questionList.indices) {
            if (userAnswers[i] == questionList[i].correctAnswer) {
                score++
            }
        }
        val totalQuestions = questionList.size
        Toast.makeText(
            this,
            "Your score: $score / $totalQuestions",
            Toast.LENGTH_LONG
        ).show()
    }
}