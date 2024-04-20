package com.example.myapplication.loginActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.ResendTokenWrapper
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.changesUpdates.UpdatePasswordLoginActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.requestResponse.LoginResponse
import com.example.myapplication.signupActivity.SignupFirstActivity
import com.example.myapplication.userPreferences.SoundManager
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var loginEmail: String
    private lateinit var soundManager: SoundManager
    private lateinit var changePasswordRedirect: AppCompatTextView
    private lateinit var mobile: String
    private lateinit var userToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        soundManager = SoundManager(this)
        findViews()

        binding.loginBTNLoginButton.setOnClickListener {
            loginEmail = binding.loginEDTEmail.text.toString()
            val loginPassword = binding.loginEDTPassword.text.toString()
            when {
                loginEmail.isEmpty() || loginPassword.isEmpty() -> {
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@LoginActivity, "All fields are mandatory", Toast.LENGTH_SHORT).show()
                }
                !isValidEmail(loginEmail) -> {
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@LoginActivity, "Invalid email format", Toast.LENGTH_SHORT).show()
                }
                !isValidPassword(loginPassword) -> {
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@LoginActivity, "Invalid password format", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val hashedPassword = hashPassword(loginPassword)

                    val map = HashMap<String, String>().apply {
                        put("email", loginEmail)
                        put("password", hashedPassword)
                    }

                    val call = retrofitInterface.executeLogin(map)

                    call.enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                            if (response.code() == 200) {
                                val result = response.body()

                                // Store email and mobile in your variables
                                userToken = result?.token.toString()
                                mobile = result?.mobile.toString()

                                val options = PhoneAuthOptions.newBuilder(auth)
                                    .setPhoneNumber(mobile)       // Phone number to verify
                                    .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                    .setActivity(this@LoginActivity)                 // Activity (for callback binding)
                                    .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                                    .build()
                                PhoneAuthProvider.verifyPhoneNumber(options)

                            } else if (response.code() == 404) {
                                Toast.makeText(this@LoginActivity, "Wrong Credentials", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                            Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        }

        binding.loginTVWSignupRedirect.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@LoginActivity, SignupFirstActivity::class.java))
            finish()
        }

        binding.loginTVWChangePasswordRedirect.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@LoginActivity, UpdatePasswordLoginActivity::class.java))
            finish()
        }
    }

    private fun findViews() {
        changePasswordRedirect = findViewById(R.id.login_TVW_changePasswordRedirect)
    }

    private fun hashPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        val hashedBytes = messageDigest.digest(passwordBytes)
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
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
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
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
            val intent = Intent(this@LoginActivity , SigninOtpActivity::class.java)
            intent.putExtra("otp" , verificationId)
            intent.putExtra("resendTokenWrapper", ResendTokenWrapper(token))
            intent.putExtra("userToken" , userToken)
            intent.putExtra("mobile", mobile)
            startActivity(intent)
        }
    }


    private fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.length <= MAX_EMAIL_LENGTH &&
                email.indexOf('@') > 0 &&
                email.indexOf('.') < email.length - 1 &&
                isDomainValid(email.substring(email.indexOf('@') + 1))
    }

    private fun isValidPassword(password: String?): Boolean {
        return !password.isNullOrBlank() &&
                password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                containsRequiredCharacterTypes(password) &&
                !isCommonPassword(password) &&
                !isDictionaryWord(password) &&
                !hasRepeatedCharacters(password) &&
                !isPasswordInHistory(password)
    }

    private fun isDomainValid(domain: String): Boolean {
        // Implement domain validation logic (e.g., check against a list of valid domains)
        return true
    }

    private fun containsRequiredCharacterTypes(password: String): Boolean {
        // Implement character type validation logic (e.g., uppercase, lowercase, numbers, special characters)
        return true
    }

    private fun isCommonPassword(password: String): Boolean {
        // Implement check against a list of common passwords
        return false
    }

    private fun isDictionaryWord(password: String): Boolean {
        // Implement check against a dictionary of common words
        return false
    }

    private fun hasRepeatedCharacters(password: String): Boolean {
        // Implement check for repeated characters (e.g., "aa" or "123123")
        return false
    }

    private fun isPasswordInHistory(password: String): Boolean {
        // Implement check against password history
        return false
    }

    companion object {
        private const val MAX_EMAIL_LENGTH = 255
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 128
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}