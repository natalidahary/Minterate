package com.example.myapplication.signupActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivitySignupSecondOtpBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SignupSecondOtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupSecondOtpBinding

    private lateinit var input1OTP: AppCompatEditText
    private lateinit var input2OTP: AppCompatEditText
    private lateinit var input3OTP: AppCompatEditText
    private lateinit var input4OTP: AppCompatEditText
    private lateinit var input5OTP: AppCompatEditText
    private lateinit var input6OTP: AppCompatEditText
    private lateinit var otpVerificationHeader: AppCompatTextView
    private lateinit var enterOtpHeader: AppCompatTextView
    private lateinit var progressBar : ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var resendOTP: AppCompatTextView
    private lateinit var verifyButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private  lateinit var otp: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var mobileOTP: AppCompatTextView
    private lateinit var soundManager: SoundManager

    private lateinit var email: String
    private lateinit var password: String
    private lateinit var mobile: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupSecondOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        findViews()

        extractIntentExtras()

        resendOTP.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        addTextChangeListener()
        resendOTPvVisibility()

        resendOTP.setOnClickListener{
            resendVerificationCode()
            resendOTPvVisibility()
        }


       // mobileOTP.text = userData.mobile
        soundManager = SoundManager(this)
        binding.signupSecondOtpBTNVerify.setOnClickListener{

            val typdOTP = (input1OTP.text.toString() + input2OTP.text.toString() + input3OTP.text.toString() + input4OTP.text.toString()+ input5OTP.text.toString()+ input6OTP.text.toString())
            if(typdOTP.isNotEmpty()){
                if(typdOTP.length==6){
                    val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(otp, typdOTP)
                    progressBar.visibility = View.VISIBLE
                    //signupOTP(typdOTP)
                    signInWithPhoneAuthCredential(credential)

                }else{
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@SignupSecondOtpActivity, "Please enter correct OTP", Toast.LENGTH_SHORT).show()
                }
            }else{
                soundManager.playWrongClickSound()
                Toast.makeText(this@SignupSecondOtpActivity, "Please enter OTP", Toast.LENGTH_SHORT).show()

            }
        }

        binding.signupSecondOtpBTNExitButton.setOnClickListener {
            startActivity(Intent(this@SignupSecondOtpActivity, SignupFirstActivity::class.java))
            finish()
        }

    }

    private fun findViews() {
        input1OTP = findViewById(R.id.signup_second_otp_EDT_input1OTP)
        input2OTP = findViewById(R.id.signup_second_otp_EDT_input2OTP)
        input3OTP = findViewById(R.id.signup_second_otp_EDT_input3OTP)
        input4OTP = findViewById(R.id.signup_second_otp_EDT_input4OTP)
        input5OTP = findViewById(R.id.signup_second_otp_EDT_input5OTP)
        input6OTP = findViewById(R.id.signup_second_otp_EDT_input6OTP)
        otpVerificationHeader = findViewById(R.id.signup_second_otp_TVW_verificationHeader)
        enterOtpHeader = findViewById(R.id.signup_second_otp_TVW_header)
        exitButton = findViewById(R.id.signup_second_otp_BTN_exitButton)
        resendOTP = findViewById(R.id.signup_second_otp_TVW_resendOTP)
        verifyButton = findViewById(R.id.signup_second_otp_BTN_verify)
        progressBar = findViewById(R.id.signup_second_otp_progressBar)
        mobileOTP = findViewById(R.id.signup_second_otp_TVW_mobileOTP)
    }

    private fun extractIntentExtras() {
        otp = intent.getStringExtra("otp").toString()
        @Suppress("DEPRECATION")
        resendToken = intent.getParcelableExtra("resendToken")!!
        email = intent.getStringExtra("email").toString()
        password = intent.getStringExtra("password").toString()
        mobile = intent.getStringExtra("mobile").toString()
        mobileOTP.text = mobile
    }


    private fun  resendOTPvVisibility(){
        input1OTP.setText("")
        input2OTP.setText("")
        input3OTP.setText("")
        input4OTP.setText("")
        input5OTP.setText("")
        input6OTP.setText("")
        resendOTP.isEnabled = false

        Handler(Looper.myLooper()!!).postDelayed(Runnable {
            resendOTP.isEnabled = true
        }, 60000)
    }

    private fun resendVerificationCode(){
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(mobile).setTimeout(60L, TimeUnit.SECONDS).setActivity(this).setCallbacks(callbacks).setForceResendingToken(resendToken).build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.d("TAG", "onVerificationCompleted: Verification successful")
            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Log.d("TAG", "onVerificationFailed: ${e.toString()}")
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Log.d("TAG", "onVerificationFailed: ${e.toString()}")
            }
            progressBar.visibility = View.VISIBLE
            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            otp = verificationId
            resendToken = token
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    progressBar.visibility = View.VISIBLE
                    Toast.makeText(this, "Authenticate Successfully", Toast.LENGTH_SHORT).show()
                    sendToActivityThird()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.d("TAG", "signInWithCredential failed: ${task.exception}")
                    Log.d("TAG", "signInWithCredential: ${task.exception.toString()}")
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
                progressBar.visibility = View.VISIBLE
            }
    }


    private fun sendToActivityThird() {
        soundManager = SoundManager(this@SignupSecondOtpActivity)
        soundManager.playClickSound()
        val intent = Intent(this, SignupThirdActivity::class.java)
        intent.putExtra("email" , email)
        intent.putExtra("password" , password)
        intent.putExtra("mobile" , mobile)
        startActivity(intent)
    }


    private fun addTextChangeListener(){
        input1OTP.addTextChangedListener(EditTextWatcher(input1OTP))
        input2OTP.addTextChangedListener(EditTextWatcher(input2OTP))
        input3OTP.addTextChangedListener(EditTextWatcher(input3OTP))
        input4OTP.addTextChangedListener(EditTextWatcher(input4OTP))
        input5OTP.addTextChangedListener(EditTextWatcher(input5OTP))
        input6OTP.addTextChangedListener(EditTextWatcher(input6OTP))

    }


    inner class EditTextWatcher(input1OTP: AppCompatEditText) : TextWatcher {
        private val editTextList = listOf(this@SignupSecondOtpActivity.input1OTP, input2OTP, input3OTP, input4OTP, input5OTP, input6OTP)

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun afterTextChanged(p0: Editable?) {
            val text = p0.toString()
            val currentFocusedIndex = getCurrentFocusedIndex()
            if (text.length == 1 && currentFocusedIndex != -1) {
                if (currentFocusedIndex < editTextList.size - 1) {
                    editTextList[currentFocusedIndex + 1].requestFocus()
                }
            } else if (text.isEmpty() && currentFocusedIndex != -1) {
                if (currentFocusedIndex > 0) {
                    editTextList[currentFocusedIndex - 1].requestFocus()
                }
            }
        }

        private fun getCurrentFocusedIndex(): Int {
            for (i in editTextList.indices) {
                if (editTextList[i].isFocused) {
                    return i
                }
            }
            return -1
        }
    }
}