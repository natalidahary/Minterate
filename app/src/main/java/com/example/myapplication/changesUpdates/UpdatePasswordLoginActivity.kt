package com.example.myapplication.changesUpdates

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityUpdatePasswordLoginBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.hbb20.CountryCodePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class UpdatePasswordLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdatePasswordLoginBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var updatePasswordHeader: AppCompatTextView
    private lateinit var signEmail: AppCompatEditText
    private lateinit var signMobile: AppCompatEditText
    private lateinit var updatePasswordLoginBTNNextButton: MaterialButton
    private lateinit var updatePasswordLoginBTNExitButton: MaterialButton
    private lateinit var signupCountryCode: CountryCodePicker
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneProgressBar: ProgressBar
    private lateinit var verificationCodeHeader: AppCompatTextView


    private lateinit var soundManager: SoundManager
    private lateinit var email: String
    private lateinit var mobile: String


    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePasswordLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        findViews()
        phoneProgressBar.visibility = View.INVISIBLE
        signupCountryCode.registerCarrierNumberEditText(signMobile)
        soundManager = SoundManager(this)

        binding.updatePasswordLoginBTNNextButton.setOnClickListener {
            val signEmail = binding.updatePasswordLoginEDTSignEmail.text.toString()
            val signMobile = binding.updatePasswordLoginEDTSignMobile.text.toString()

                if(isValidEmail(signEmail)) {
                    email = signEmail
                    validateMobile(email,signMobile) { isMobileValid ->
                        if (isMobileValid) {
                            // All validations passed, proceed with registration
                            val options = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(mobile)       // Phone number to verify
                                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                .setActivity(this@UpdatePasswordLoginActivity)                 // Activity (for callback binding)
                                .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                                .build()
                            PhoneAuthProvider.verifyPhoneNumber(options)

                        } else {
                            // Mobile validation failed
                            soundManager.playWrongClickSound()
                            Toast.makeText(
                                this@UpdatePasswordLoginActivity,
                                "Please enter a valid mobile number",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Email validation failed
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@UpdatePasswordLoginActivity,
                        "Please fill in all fields correctly",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        binding.updatePasswordLoginBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@UpdatePasswordLoginActivity, LoginActivity::class.java))
            finish()
        }
    }


    private fun findViews() {
        updatePasswordHeader = findViewById(R.id.update_password_login_TVW_header)
        signEmail = findViewById(R.id.update_password_login_EDT_signEmail)
        signMobile = findViewById(R.id.update_password_login_EDT_signMobile)
        updatePasswordLoginBTNNextButton = findViewById(R.id.update_password_login_BTN_nextButton)
        updatePasswordLoginBTNExitButton = findViewById(R.id.update_password_login_BTN_exitButton)
        signupCountryCode = findViewById(R.id.update_password_login_countryCode)
        phoneProgressBar = findViewById(R.id.update_password_login_progressBar)
        verificationCodeHeader = findViewById(R.id.update_password_login_TVW_verificationCodeHeader)
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(this , "Authenticate Successfully" , Toast.LENGTH_SHORT).show()
                    // sendToActivitySecondOtp()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.d("TAG", "signInWithPhoneAuthCredential: ${task.exception.toString()}")
                    if(task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
                phoneProgressBar.visibility = View.INVISIBLE
            }
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

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Log.d("TAG", "onVerificationFailed: ${e.toString()}")
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Log.d("TAG", "onVerificationFailed: ${e.toString()}")
            }
            phoneProgressBar.visibility = View.VISIBLE
            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            // Save verification ID and resending token so we can use them later
            soundManager = SoundManager(this@UpdatePasswordLoginActivity)
            soundManager.playClickSound()
            val intent = Intent(this@UpdatePasswordLoginActivity , UpdatePasswordOtpLoginActivity::class.java)
            intent.putExtra("otp" , verificationId)
            intent.putExtra("resendToken" , token)
            intent.putExtra("email" , email)
            intent.putExtra("mobile" , mobile)
            startActivity(intent)
            phoneProgressBar.visibility = View.INVISIBLE
        }
    }

    companion object {
        private const val MAX_EMAIL_LENGTH = 255
    }

    private fun isValidEmail(email: String?): Boolean {
        // Validate email format
        return if (email.isNullOrBlank() ||
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
            email.length > MAX_EMAIL_LENGTH ||
            email.indexOf('@') <= 0 ||
            email.indexOf('.') >= email.length - 1 ||
            !isDomainValid(email.substring(email.indexOf('@') + 1))
        ) {
            Toast.makeText(this@UpdatePasswordLoginActivity, "Invalid email address", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }


    private fun isDomainValid(domain: String): Boolean {
        // val validDomains = listOf("example.com", "yourdomain.com", "otherdomain.org") // Add your valid domains to the list
        // Check if the given domain is in the list of valid domains
        //return validDomains.contains(domain.lowercase(Locale.getDefault()))
        return true
    }




    private fun isValidMobile(mobile: String?): Boolean {
        return !mobile.isNullOrBlank() &&
                mobile.all { it.isDigit() || it == '-' } &&
                signupCountryCode.isValidFullNumber
    }


    private fun validateMobile(email: String, signupMobile: String, callback: (Boolean) -> Unit) {
        if (!isValidMobile(signupMobile)) {
            Toast.makeText(this@UpdatePasswordLoginActivity, "Mobile number not valid", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Mobile number is valid, check if it matches the existing mobile for the user's email
        //val map = hashMapOf("email" to email)
        mobile = signupCountryCode.fullNumberWithPlus
        val call = retrofitInterface.getMobileForEmail(email)
        call.enqueue(object : Callback<com.example.myapplication.requestResponse.GetMobileByEmailResponse> {
            override fun onResponse(call: Call<com.example.myapplication.requestResponse.GetMobileByEmailResponse>, response: Response<com.example.myapplication.requestResponse.GetMobileByEmailResponse>) {
                if (response.isSuccessful) {
                    val mobileCheckResponse = response.body()
                    if (mobileCheckResponse != null) {
                        val existingMobile = mobileCheckResponse.mobile
                        if (existingMobile == mobile) {
                            // Mobile number matches the existing mobile for the user's email
                            callback(true)
                        } else {
                            // Mobile number does not match the existing mobile
                            Toast.makeText(this@UpdatePasswordLoginActivity, "Mobile number does not match the existing mobile", Toast.LENGTH_SHORT).show()
                            callback(false)
                        }
                    }
                } else {
                    // Server response error
                    Toast.makeText(this@UpdatePasswordLoginActivity, "Email or mobile not found!", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }

            override fun onFailure(call: Call<com.example.myapplication.requestResponse.GetMobileByEmailResponse>, t: Throwable) {
                // Handle network or other failures
                Toast.makeText(this@UpdatePasswordLoginActivity, "Network error", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

}