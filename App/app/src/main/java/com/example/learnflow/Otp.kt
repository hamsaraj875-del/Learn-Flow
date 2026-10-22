package com.example.learnflow

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class Otp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var etOtp1 : EditText;
        var etOtp2 : EditText;
        var etOtp3 : EditText;
        var etOtp4 : EditText;
        var etOtp5 : EditText;
        var etOtp6 : EditText;
        var verifyLoader : ProgressBar;
        var btnVerify : AppCompatButton;

        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        btnVerify = findViewById(R.id.btnVerify);
        verifyLoader = findViewById(R.id.verifyLoader);
        var chatRepository = ChatRepository(RetrofitClient.api);

        verifyLoader.visibility = View.GONE;

        btnVerify.setOnClickListener{
            if(etOtp1.text.toString().isEmpty() || etOtp2.text.toString().isEmpty() || etOtp3.text.toString().isEmpty() || etOtp4.text.toString().isEmpty() || etOtp5.text.toString().isEmpty() || etOtp6.text.toString().isEmpty()){
                Toast.makeText(this@Otp,"Otp fields cannot be empty",Toast.LENGTH_SHORT).show();
            }else{
                btnVerify.text = "";
                verifyLoader.visibility = View.VISIBLE;
                btnVerify.isEnabled = false;
                var otp = etOtp1.text.toString()+etOtp2.text.toString()+etOtp3.text.toString()+etOtp4.text.toString()+etOtp5.text.toString()+etOtp6.text.toString();
                try{
                    lifecycleScope.launch{
                        var response = chatRepository.otp(OtpRequest(otp));
                        if(response.success){
                            var intent = Intent(this@Otp,MainActivity::class.java);
                            startActivity(intent);
                        }else{
                            Toast.makeText(this@Otp,"Invalid otp",Toast.LENGTH_SHORT).show();
                        }
                    }
                }catch(e:Exception){
                    Toast.makeText(this@Otp,"Internal server error occured",Toast.LENGTH_SHORT).show();
                }finally{
                    btnVerify.text = "Verify  →";
                    verifyLoader.visibility = View.GONE;
                    btnVerify.isEnabled = true;
                }
            }
        }

    }
}