package com.example.learnflow.signup

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
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
import com.example.learnflow.R
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.remote.RetrofitClient
import com.example.learnflow.data.repository.LoginRepository
import com.example.learnflow.main.MainActivity
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject

class Otp : AppCompatActivity() {

    private lateinit var tvResend: TextView
    private lateinit var otpFields: List<EditText>
    private lateinit var verifyLoader: ProgressBar
    private lateinit var btnVerify: AppCompatButton
    private lateinit var repository: LoginRepository

    private var resendTimer: CountDownTimer? = null
    private var isPasting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        otpFields = listOf(
            findViewById(R.id.etOtp1),
            findViewById(R.id.etOtp2),
            findViewById(R.id.etOtp3),
            findViewById(R.id.etOtp4),
            findViewById(R.id.etOtp5),
            findViewById(R.id.etOtp6)
        )

        btnVerify = findViewById(R.id.btnVerify)
        verifyLoader = findViewById(R.id.verifyLoader)
        tvResend = findViewById(R.id.tvResend)

        repository = LoginRepository(RetrofitClient.api)

        verifyLoader.visibility = View.GONE

        setupOtpFields()
        startResendTimer(300000L)

        btnVerify.setOnClickListener {
            verifyOtp()
        }

        tvResend.setOnClickListener {
            if (tvResend.isEnabled) {
                resendOtp()
            }
        }

        findViewById<TextView>(R.id.tvBack).setOnClickListener {
            finish()
        }
    }

    private fun setupOtpFields() {
        otpFields.forEachIndexed { index, field ->

            field.filters = arrayOf(InputFilter.LengthFilter(6))

            field.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (isPasting || s.isNullOrEmpty()) return

                    if (s.length > 1) {
                        val digits = s.toString()
                            .filter { it.isDigit() }
                            .take(6)

                        if (digits.length == 6) {
                            pasteOtp(digits)
                        }
                        return
                    }

                    if (index < otpFields.size - 1) {
                        otpFields[index + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            field.setOnKeyListener { _, keyCode, event ->
                if (
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    field.text.isEmpty() &&
                    index > 0
                ) {
                    otpFields[index - 1].requestFocus()
                    otpFields[index - 1].setSelection(
                        otpFields[index - 1].text.length
                    )
                }
                false
            }
        }

        otpFields[0].requestFocus()
    }

    private fun pasteOtp(otp: String) {
        if (otp.length != 6 || !otp.all { it.isDigit() }) return

        isPasting = true

        otpFields.forEachIndexed { index, field ->
            field.setText(otp[index].toString())
        }

        isPasting = false
        otpFields[5].requestFocus()
    }

    private fun verifyOtp() {
        val otp = otpFields.joinToString("") {
            it.text.toString()
        }

        if (otp.length != 6) {
            Toast.makeText(
                this,
                "Enter the complete OTP",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnVerify.text = ""
        btnVerify.isEnabled = false
        verifyLoader.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = repository.otp(OtpRequest(otp))

                if (response.success) {
                    val intent = Intent(
                        this@Otp,
                        MainActivity::class.java
                    )
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@Otp,
                        response.mes ?: "Invalid OTP",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: HttpException) {
                val message = try {
                    JSONObject(
                        e.response()?.errorBody()?.string() ?: "{}"
                    ).optString("mes", "OTP verification failed")
                } catch (ex: Exception) {
                    "OTP verification failed"
                }

                Toast.makeText(
                    this@Otp,
                    message,
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@Otp,
                    "Network error. Please try again",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {
                btnVerify.text = "Verify  →"
                btnVerify.isEnabled = true
                verifyLoader.visibility = View.GONE
            }
        }
    }

    private fun resendOtp() {
        tvResend.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repository.resendOtp()

                if (response.success) {
                    otpFields.forEach {
                        it.text.clear()
                    }

                    otpFields[0].requestFocus()

                    Toast.makeText(
                        this@Otp,
                        response.mes ?: "OTP sent successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    startResendTimer(300000L)

                } else {
                    Toast.makeText(
                        this@Otp,
                        response.mes ?: "Unable to resend OTP",
                        Toast.LENGTH_SHORT
                    ).show()

                    tvResend.isEnabled = true
                }

            } catch (e: HttpException) {

                val errorBody = try {
                    JSONObject(
                        e.response()?.errorBody()?.string() ?: "{}"
                    )
                } catch (ex: Exception) {
                    JSONObject()
                }

                val message = errorBody.optString(
                    "mes",
                    "Unable to resend OTP"
                )

                Toast.makeText(
                    this@Otp,
                    message,
                    Toast.LENGTH_SHORT
                ).show()

                if (e.code() == 429) {
                    val remainingTime = errorBody.optLong(
                        "remainingTime",
                        300000L
                    )

                    startResendTimer(remainingTime)
                } else {
                    tvResend.isEnabled = true
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@Otp,
                    "Network error. Please try again",
                    Toast.LENGTH_SHORT
                ).show()

                tvResend.isEnabled = true
            }
        }
    }

    private fun startResendTimer(duration: Long) {
        resendTimer?.cancel()

        tvResend.isEnabled = false

        resendTimer = object : CountDownTimer(duration, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                tvResend.text = String.format(
                    "Resend (%02d:%02d)",
                    minutes,
                    remainingSeconds
                )
            }

            override fun onFinish() {
                tvResend.text = "Resend"
                tvResend.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
}