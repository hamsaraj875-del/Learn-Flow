package com.example.learnflow.test

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.learnflow.R

class McqTestResult : AppCompatActivity() {
    private lateinit var tvAccuracy : TextView;
    private lateinit var tvScore : TextView;
    private lateinit var tvCorrect:TextView;
    private lateinit var tvWrong:TextView;
    private lateinit var tvUnanswered:TextView;

    private lateinit var btnHome: FrameLayout;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mcq_test_result)
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

        var correct = intent.getIntExtra("correct",0);
        var wrong = intent.getIntExtra("wrong",0);
        var unanswered = intent.getIntExtra("unanswered",0);

        var total = correct+wrong+unanswered;

        tvAccuracy.text = (((total - wrong - unanswered).toFloat() / total) * 100).toInt().toString() + "%"
        tvScore.text = "$correct/$total"
        tvCorrect.text = correct.toString()
        tvWrong.text = wrong.toString()
        tvUnanswered.text = unanswered.toString()
    }
}