package com.example.learnflow.test

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.learnflow.R

class WrittenTestResult : AppCompatActivity() {
    private lateinit var tvAccuracy : TextView;
    private lateinit var tvScore : TextView;
    private lateinit var tvCorrect:TextView;
    private lateinit var tvWrong:TextView;
    private lateinit var tvUnanswered:TextView;

    private lateinit var btnHome: FrameLayout;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_test_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvAccuracy = findViewById(R.id.tvPercentage);
        tvScore = findViewById(R.id.tvScore);
        tvCorrect = findViewById(R.id.tvCorrect);
        tvWrong = findViewById(R.id.tvWrong);
        tvUnanswered = findViewById(R.id.tvUnanswered);
        btnHome = findViewById(R.id.btnHome);


        btnHome.setOnClickListener{
            finish();
        }

        val correct = intent.getIntExtra("correct", 0)
        val wrong = intent.getIntExtra("wrong", 0)
        val totalMarks = intent.getIntExtra("totalMarks",0);
        val unanswered = intent.getIntExtra("unanswered", 0)
        val total = correct + wrong + unanswered
        if (total > 0) {
            val accuracy = (correct.toFloat() / total * 100).toInt()
            tvAccuracy.text = "$accuracy%"
            tvScore.text = "$totalMarks/$total"
            tvCorrect.text = correct.toString()
            tvWrong.text = wrong.toString()
            tvUnanswered.text = unanswered.toString()
        } else {
            Toast.makeText(
                this@WrittenTestResult,
                "No test results available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}