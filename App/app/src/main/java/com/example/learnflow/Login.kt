package com.example.learnflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var signUp : TextView;
        var userEmail : EditText;
        var userPassword : EditText;
        var btnLogin : AppCompatButton;
        var loginLoader : ProgressBar;

        signUp = findViewById(R.id.tvSignup);
        userEmail = findViewById(R.id.etEmail);
        userPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginLoader = findViewById(R.id.loginLoader);
        var chatRepository = ChatRepository(RetrofitClient.api);

        signUp.setOnClickListener{
            var intent = Intent(this@Login, SignUp::class.java);
            startActivity(intent);
        }

        loginLoader.visibility = View.GONE;

        btnLogin.setOnClickListener{
            var email = userEmail.text.toString();
            var password = userPassword.text.toString();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this@Login,"Fields cannot be empty",Toast.LENGTH_SHORT).show();
            }else{
                btnLogin.text = "";
                btnLogin.isEnabled = false;
                loginLoader.visibility = View.VISIBLE;
                lifecycleScope.launch{
                    try{
                        var response = chatRepository.login(LoginRequest(email, password));
                        if(response.success){
                            var intent = Intent(this@Login,MainActivity::class.java);
                            startActivity(intent);
                        }else{
                            Toast.makeText(this@Login,"Invalid Credentials",Toast.LENGTH_SHORT).show();
                        }
                    }catch(e:Exception){
                        Toast.makeText(this@Login,"Internal server error occured please try again",Toast.LENGTH_SHORT).show();
                    }finally{
                        btnLogin.text = "Sign In  →"
                        btnLogin.isEnabled = true;
                        loginLoader.visibility = View.GONE;
                    }
                }

            }
        }
    }
}