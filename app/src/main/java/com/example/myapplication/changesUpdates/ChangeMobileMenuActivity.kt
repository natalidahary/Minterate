package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityChangeMobileMenuBinding
import com.example.myapplication.requestResponse.MobileCheckResponse
import com.example.myapplication.requestResponse.UserDataResponse
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
import kotlin.properties.Delegates

class ChangeMobileMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeMobileMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var oldMobileEditText: AppCompatEditText
    private lateinit var newMobileEditText: AppCompatEditText
    private lateinit var changeMobileMenuSave: MaterialButton
    private lateinit var changeMobileMenuBTNExitButton: MaterialButton
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var progressBar: ProgressBar
    private lateinit var oldMobile: String
    private lateinit var mobile: String
    private lateinit var userToken: String
    private lateinit var changeMobileHeader: AppCompatTextView
    private lateinit var verificationCodeHeader: AppCompatTextView
    private lateinit var userData: UserDataResponse


    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeMobileMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        findViews()

        progressBar.visibility = View.INVISIBLE
        countryCodePicker.registerCarrierNumberEditText(newMobileEditText)

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        soundManager = SoundManager(this@ChangeMobileMenuActivity)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        binding.changeMobileMenuSave.setOnClickListener {
            oldMobile = binding.changeMobileMenuEDTOldMobile.text.toString()
            mobile = binding.changeMobileMenuEDTNewMobile.text.toString()
            if (oldMobile.isNotEmpty() && mobile.isNotEmpty() && countryCodePicker.isValidFullNumber) {
                if (oldMobile == userData.mobile) {
                            validateMobile(mobile) { isMobileValid ->
                                if (isMobileValid) {
                                    var fullNewMobile = countryCodePicker.fullNumberWithPlus
                                    if (fullNewMobile.toString() != oldMobile) {
                                        progressBar.visibility = View.VISIBLE
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(mobile)       // Phone number to verify
                                            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                            .setActivity(this)                 // Activity (for callback binding)
                                            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } else {
                                        soundManager.playWrongClickSound()
                                        newMobileEditText.error =
                                            "Please enter different phone number"
                                    }
                                } else {
                                    soundManager.playWrongClickSound()
                                    Toast.makeText(
                                        this@ChangeMobileMenuActivity,
                                        "Invalid new mobile",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                } else{
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangeMobileMenuActivity,
                        "Incorrect old mobile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeMobileMenuActivity,
                    "Please fill in all fields correctly",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        binding.changeMobileMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun findViews() {
        oldMobileEditText = findViewById(R.id.change_mobile_menu_EDT_oldMobile)
        newMobileEditText = findViewById(R.id.change_mobile_menu_EDT_newMobile)
        changeMobileMenuSave = findViewById(R.id.change_mobile_menu_save)
        changeMobileMenuBTNExitButton = findViewById(R.id.change_mobile_menu_BTN_exitButton)
        countryCodePicker = findViewById(R.id.change_mobile_menu_newCountryCode)
        progressBar = findViewById(R.id.change_mobile_menu_progressBar)
        changeMobileHeader = findViewById(R.id.change_mobile_menu_TVW_header)
        verificationCodeHeader = findViewById(R.id.change_mobile_menu_TVW_verificationCodeHeader)
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
                progressBar.visibility = View.INVISIBLE
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
            progressBar.visibility = View.VISIBLE
            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            soundManager = SoundManager(this@ChangeMobileMenuActivity)
            soundManager.playClickSound()
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            // Save verification ID and resending token so we can use them later
            //updateMobile(email, mobile)
            val intent = Intent(this@ChangeMobileMenuActivity , ChangeMobileMenuOtpActivity::class.java)
            intent.putExtra("otp" , verificationId)
            intent.putExtra("resendToken" , token)
            intent.putExtra("mobile" , mobile)
            intent.putExtra("userToken", userToken)
            startActivity(intent)
            progressBar.visibility = View.INVISIBLE
        }
    }



    private fun isValidMobile(mobile: String?): Boolean {
        return !mobile.isNullOrBlank() &&
                mobile.all { it.isDigit() || it == '-'  } &&
                countryCodePicker.isValidFullNumber
    }


    private fun validateMobile(signupMobile: String, callback: (Boolean) -> Unit) {
        if (!isValidMobile(signupMobile)) {
            Toast.makeText(this@ChangeMobileMenuActivity, "Mobile number not valid", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Mobile number is valid, check if it already exists
        mobile = countryCodePicker.fullNumberWithPlus
        val map = hashMapOf("mobile" to mobile)
        val call = retrofitInterface.checkMobileExists(map)

        call.enqueue(object : Callback<MobileCheckResponse> {
            override fun onResponse(call: Call<MobileCheckResponse>, response: Response<MobileCheckResponse>) {
                if (response.isSuccessful) {
                    val mobileCheckResponse = response.body()
                    if (mobileCheckResponse != null) {
                        if (mobileCheckResponse.exists) {
                            // Server response indicates mobile number already exists
                            Toast.makeText(this@ChangeMobileMenuActivity, "Mobile already exists", Toast.LENGTH_SHORT).show()
                            callback(false)
                        } else {
                            // Mobile is valid and does not exist
                            callback(true)
                        }
                    }
                } else {
                    // Server response error
                    Toast.makeText(this@ChangeMobileMenuActivity, "Server response error", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }

            override fun onFailure(call: Call<MobileCheckResponse>, t: Throwable) {
                // Handle network or other failures
                Toast.makeText(this@ChangeMobileMenuActivity, "Network error", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            oldMobileEditText, newMobileEditText,
            changeMobileHeader, verificationCodeHeader
            // Include other TextViews and EditTexts as needed
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}