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
import com.example.learnflow.data.model.SignUpRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.ChatRepository
import kotlinx.coroutines.launch

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var login : TextView;
        var editName : EditText;
        var editEmail : EditText;
        var editPassword : EditText;
        var btnSignUp : AppCompatButton;
        var signupLoader : ProgressBar;

        login = findViewById(R.id.tvLogin);
        editName = findViewById(R.id.etName);
        editEmail = findViewById(R.id.etEmail);
        editPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignup);
        signupLoader = findViewById(R.id.signupLoader);
        var chatRepository = ChatRepository(RetrofitClient.api);

        login.setOnClickListener{
            var intent = Intent(this@SignUp, Login::class.java);
            startActivity(intent);
        }

        btnSignUp.setOnClickListener{
            var userName = editName.text.toString();
            var email = editEmail.text.toString();
            var password = editPassword.text.toString();

            if(userName.isEmpty() || email.isEmpty() || password.isEmpty()){
                Toast.makeText(this@SignUp,"Fields cannot be empty",Toast.LENGTH_SHORT).show();
            }else{
                try{
                    signupLoader.visibility = View.VISIBLE;
                    btnSignUp.text = "";
                    btnSignUp.isEnabled = false;
                    lifecycleScope.launch{
                        var response = chatRepository.signUp(SignUpRequest(userName, email, password));
                        if(response.success){
                            var intent = Intent(this@SignUp,MainActivity::class.java);
                            startActivity(intent);
                        }else{
                            Toast.makeText(this@SignUp,"Internal server error occured please try again",Toast.LENGTH_SHORT).show();
                        }
                    }
                }catch(e:Exception){
                    Toast.makeText(this@SignUp,"Internal server error occured please try again",Toast.LENGTH_SHORT).show();
                }
                finally{
                    signupLoader.visibility = View.GONE;
                    btnSignUp.text = "Create Account  →";
                    btnSignUp.isEnabled = true;
                }
            }
        }

    }
}