package com.example.myapplication.signupActivity

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivitySignupFirstBinding
import com.example.myapplication.requestResponse.MobileCheckResponse
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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class SignupFirstActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupFirstBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var signupEmail: AppCompatEditText
    private lateinit var signupPassword: AppCompatEditText
    private lateinit var signupConfirmPassword: AppCompatEditText
    private lateinit var signupMobile: AppCompatEditText
    private lateinit var nextButton: MaterialButton
    private lateinit var signup_country_code: CountryCodePicker
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneProgressBar: ProgressBar
    private lateinit var signUpHeader: AppCompatTextView
    private lateinit var verificationCodeHeader: AppCompatTextView
    private lateinit var exitButton: MaterialButton

    private lateinit var soundManager: SoundManager
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var mobile: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupFirstBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        findViews()
        phoneProgressBar.visibility = View.INVISIBLE

        signup_country_code.registerCarrierNumberEditText(signupMobile)
        soundManager = SoundManager(this)

        binding.signupFirstBTNNextButton.setOnClickListener {
            val signupEmail = binding.signupFirstEDTEmail.text.toString()
            val signupPassword = binding.signupFirstEDTSignupPassword.text.toString()
            val signupConfirmPassword = binding.signupFirstEDTConfirmPassword.text.toString()
            val signupMobile = binding.signupFirstEDTMobile.text.toString()

            isValidEmail(signupEmail) { isEmailValid ->
                if (isEmailValid) {
                    validateMobile(signupMobile) { isMobileValid ->
                        if (isMobileValid && isValidPassword(signupPassword, signupConfirmPassword)) {
                            // All validations passed, proceed with registration
                            email = signupEmail
                            password = hashPassword(signupPassword)

                            if (mobile.isNotEmpty()) {
                                // Check if the user with the provided email already exists in authentication
                                @Suppress("DEPRECATION")
                                auth.fetchSignInMethodsForEmail(email)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val signInMethods = task.result?.signInMethods
                                            if (!signInMethods.isNullOrEmpty()) {
                                                // User with the provided email already exists
                                                soundManager.playWrongClickSound()
                                                Toast.makeText(
                                                    this@SignupFirstActivity,
                                                    "User with this email already exists. Choose a different email.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                phoneProgressBar.visibility = View.INVISIBLE
                                            } else {
                                                // User with the provided email does not exist, proceed with registration
                                                phoneProgressBar.visibility = View.VISIBLE
                                                val options = PhoneAuthOptions.newBuilder(auth)
                                                    .setPhoneNumber(mobile)       // Phone number to verify
                                                    .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                                    .setActivity(this)                 // Activity (for callback binding)
                                                    .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                                                    .build()

                                                // Move PhoneAuthProvider.verifyPhoneNumber inside this block
                                                // PhoneAuthProvider.verifyPhoneNumber(options)
                                                auth.createUserWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener(this) { registrationTask ->
                                                        if (registrationTask.isSuccessful) {
                                                            // Registration success
                                                            // Now, initiate phone number verification
                                                            PhoneAuthProvider.verifyPhoneNumber(options)
                                                        } else {
                                                            // If sign-in fails, display a message to the user.
                                                            Log.w(
                                                                ContentValues.TAG,
                                                                "createUserWithEmail:failure",
                                                                registrationTask.exception
                                                            )
                                                            Toast.makeText(
                                                                baseContext,
                                                                "Authentication failed: ${registrationTask.exception?.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            phoneProgressBar.visibility = View.INVISIBLE
                                                        }
                                                    }
                                            }
                                        } else {
                                            // Handle the exception if fetching sign-in methods fails
                                            soundManager.playWrongClickSound()
                                            Toast.makeText(
                                                this@SignupFirstActivity,
                                                "Error checking user existence. Please try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                        } else {
                            // Mobile validation failed
                            soundManager.playWrongClickSound()
                            Toast.makeText(
                                this@SignupFirstActivity,
                                "Please enter a valid mobile number",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Email validation failed
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@SignupFirstActivity,
                        "Please fill in all fields correctly",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.signupFirstBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@SignupFirstActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun findViews() {
        signupEmail = findViewById(R.id.signup_first_EDT_email)
        signupPassword = findViewById(R.id.signup_first_EDT_signupPassword)
        signupConfirmPassword = findViewById(R.id.signup_first_EDT_confirmPassword)
        signupMobile = findViewById(R.id.signup_first_EDT_mobile)
        signup_country_code = findViewById(R.id.signup_first_countryCode)
        nextButton = findViewById(R.id.signup_first_BTN_nextButton)
        exitButton = findViewById(R.id.signup_first_BTN_exitButton)
        phoneProgressBar = findViewById(R.id.signup_first_progressBar)
        signUpHeader = findViewById(R.id.signup_first_TVW_signUpHeader)
        verificationCodeHeader = findViewById(R.id.signup_first_TVW_verificationCodeHeader)
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
            soundManager = SoundManager(this@SignupFirstActivity)
            soundManager.playClickSound()
            val intent = Intent(this@SignupFirstActivity , SignupSecondOtpActivity::class.java)
            intent.putExtra("otp" , verificationId)
            intent.putExtra("resendToken" , token)
            intent.putExtra("email" , email)
            intent.putExtra("password" , password)
            intent.putExtra("mobile" , mobile)
            startActivity(intent)
            phoneProgressBar.visibility = View.INVISIBLE
        }
    }

    private fun isValidEmail(email: String?, callback: (Boolean) -> Unit) {
        // Validate email format
        if (email.isNullOrBlank() ||
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
            email.length > MAX_EMAIL_LENGTH ||
            email.indexOf('@') <= 0 ||
            email.indexOf('.') >= email.length - 1 ||
            !isDomainValid(email.substring(email.indexOf('@') + 1))
        ) {
            Toast.makeText(this@SignupFirstActivity, "Invalid email address", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Check if email exists
        val map = HashMap<String, String>().apply {
            put("email", email)
        }
        val call = retrofitInterface.checkEmailExists(map)

        call.enqueue(object : Callback<com.example.myapplication.requestResponse.EmailCheckResponse> {
            override fun onResponse(call: Call<com.example.myapplication.requestResponse.EmailCheckResponse>, response: Response<com.example.myapplication.requestResponse.EmailCheckResponse>) {
                if (response.isSuccessful) {
                    val emailExists = response.body()?.exists == true
                    if (!emailExists) {
                        callback(true)
                    } else {
                        Toast.makeText(this@SignupFirstActivity, "Email already exists", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                } else {
                    Toast.makeText(this@SignupFirstActivity, "Server response error", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }

            override fun onFailure(call: Call<com.example.myapplication.requestResponse.EmailCheckResponse>, t: Throwable) {
                // Handle network or other failures
                callback(false)
            }
        })
    }


    private fun isValidPassword(password: String?, confirmPassword: String?): Boolean {
        var isPasswordValid = !password.isNullOrBlank() &&
                password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                containsRequiredCharacterTypes(password) &&
                !isCommonPassword(password) &&
                !isDictionaryWord(password) &&
                !hasRepeatedCharacters(password) &&
                !isPasswordInHistory(password)

        if (!isPasswordValid) {
            Toast.makeText(this@SignupFirstActivity, "Invalid password", Toast.LENGTH_SHORT).show()
        }
        if (password != confirmPassword) {
            isPasswordValid = false
            Toast.makeText(this@SignupFirstActivity, "Passwords must match", Toast.LENGTH_SHORT).show()
        }

        return isPasswordValid
    }


    private fun isDomainValid(domain: String): Boolean {
        // val validDomains = listOf("example.com", "yourdomain.com", "otherdomain.org") // Add your valid domains to the list
        // Check if the given domain is in the list of valid domains
        //return validDomains.contains(domain.lowercase(Locale.getDefault()))
        return true
    }

    private fun containsRequiredCharacterTypes(password: String): Boolean {
        val containsUppercase = password.any { it.isUpperCase() }
        val containsLowercase = password.any { it.isLowerCase() }
        val containsDigit = password.any { it.isDigit() }
        val containsSpecialChar = password.any { !it.isLetterOrDigit() }

        return containsUppercase && containsLowercase && containsDigit && containsSpecialChar
    }

    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = listOf("password123", "123456", "qwerty", "admin")
        return commonPasswords.contains(password)
    }

    private fun isDictionaryWord(password: String): Boolean {
        val dictionaryWords = listOf("password", "admin", "letmein", "welcome")
        return dictionaryWords.contains(password.lowercase(Locale.getDefault()))
    }

    private fun hasRepeatedCharacters(password: String): Boolean {
        return password.windowed(2).any { it[0] == it[1] }
    }

    private fun isPasswordInHistory(password: String): Boolean {
        val passwordHistory = listOf("previousPassword1", "previousPassword2", "previousPassword3")
        return passwordHistory.contains(password)
    }

    companion object {
        private const val MAX_EMAIL_LENGTH = 255
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 128
    }


    private fun isValidMobile(mobile: String?): Boolean {
        return !mobile.isNullOrBlank() &&
                mobile.all { it.isDigit() || it == '-' } &&
                signup_country_code.isValidFullNumber
    }


    private fun validateMobile(signupMobile: String, callback: (Boolean) -> Unit) {
        if (!isValidMobile(signupMobile)) {
            Toast.makeText(this@SignupFirstActivity, "Mobile number not valid", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Mobile number is valid, check if it already exists
        mobile = signup_country_code.fullNumberWithPlus
        val map = hashMapOf("mobile" to mobile)
        val call = retrofitInterface.checkMobileExists(map)

        call.enqueue(object : Callback<MobileCheckResponse> {
            override fun onResponse(call: Call<MobileCheckResponse>, response: Response<MobileCheckResponse>) {
                if (response.isSuccessful) {
                    val mobileCheckResponse = response.body()
                    if (mobileCheckResponse != null) {
                        if (mobileCheckResponse.exists) {
                            // Server response indicates mobile number already exists
                            Toast.makeText(this@SignupFirstActivity, "Mobile already exists", Toast.LENGTH_SHORT).show()
                            callback(false)
                        } else {
                            // Mobile is valid and does not exist
                            callback(true)
                        }
                    }
                } else {
                    // Server response error
                    Toast.makeText(this@SignupFirstActivity, "Server response error", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }

            override fun onFailure(call: Call<MobileCheckResponse>, t: Throwable) {
                // Handle network or other failures
                Toast.makeText(this@SignupFirstActivity, "Network error", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

}