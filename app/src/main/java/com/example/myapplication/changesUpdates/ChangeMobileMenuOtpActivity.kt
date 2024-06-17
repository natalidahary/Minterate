package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityChangeMobileMenuOtpBinding
import com.example.myapplication.requestResponse.MobileUpdateRequest
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

class ChangeMobileMenuOtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeMobileMenuOtpBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var input1OTP: AppCompatEditText
    private lateinit var input2OTP: AppCompatEditText
    private lateinit var input3OTP: AppCompatEditText
    private lateinit var input4OTP: AppCompatEditText
    private lateinit var input5OTP: AppCompatEditText
    private lateinit var input6OTP: AppCompatEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var changeMobileOtpMenuBTNExitButton: MaterialButton
    private lateinit var resendOTP: AppCompatTextView
    private lateinit var receiveOTP: AppCompatTextView
    private lateinit var changeMobileOtpMenuBTNVerify: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var otp: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var userToken: String
    private lateinit var mobile: String
    private lateinit var mobileOTP: AppCompatTextView
    private lateinit var otpVerificationHeader: AppCompatTextView
    private lateinit var enterOtpHeader: AppCompatTextView
    private lateinit var minterateLogo: AppCompatImageView
    private lateinit var codePicker: CountryCodePicker
    private lateinit var codePickerBw: CountryCodePicker

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeMobileMenuOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        findViews()
        otp = intent.getStringExtra("otp").toString()

        @Suppress("DEPRECATION")
        resendToken = intent.getParcelableExtra("resendToken")!!

        resendOTP.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()
        applyBlackAndWhiteMode()

        addTextChangeListener()
        resendOTPvVisibility()

        resendOTP.setOnClickListener {
            resendVerificationCode()
            resendOTPvVisibility()
        }

        userToken = intent.getStringExtra("userToken").toString()
        mobile = intent.getStringExtra("mobile").toString()
        mobileOTP.text = mobile

        soundManager = SoundManager(this)
        //soundManager.setSoundEnabled(userData.sounds)
        binding.changeMobileOtpMenuBTNVerify.setOnClickListener {

            val typdOTP =
                (input1OTP.text.toString() + input2OTP.text.toString() + input3OTP.text.toString() + input4OTP.text.toString() + input5OTP.text.toString() + input6OTP.text.toString())
            if (typdOTP.isNotEmpty()) {
                if (typdOTP.length == 6) {
                    val credential: PhoneAuthCredential =
                        PhoneAuthProvider.getCredential(otp, typdOTP)
                    progressBar.visibility = View.VISIBLE
                    signInWithPhoneAuthCredential(credential)

                } else {
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangeMobileMenuOtpActivity,
                        "Please enter correct OTP",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(this@ChangeMobileMenuOtpActivity, "Please enter OTP", Toast.LENGTH_SHORT)
                    .show()

            }
        }

        binding.changeMobileOtpMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@ChangeMobileMenuOtpActivity, ChangeMobileMenuActivity::class.java))
            finish()
        }

    }

    private fun findViews() {
        input1OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input1OTP)
        input2OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input2OTP)
        input3OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input3OTP)
        input4OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input4OTP)
        input5OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input5OTP)
        input6OTP = findViewById(R.id.change_mobile_otp_menu_EDT_input6OTP)
        progressBar = findViewById(R.id.change_mobile_otp_menu_progressBar)
        changeMobileOtpMenuBTNExitButton = findViewById(R.id.change_mobile_otp_menu_BTN_exitButton)
        resendOTP = findViewById(R.id.change_mobile_otp_menu_TVW_resendOTP)
        receiveOTP = findViewById(R.id.change_mobile_otp_menu_TVW_receiveOTP)
        changeMobileOtpMenuBTNVerify =findViewById(R.id.change_mobile_otp_menu_BTN_verify)
        mobileOTP = findViewById(R.id.change_mobile_otp_menu_TVW_mobileOTP)
        otpVerificationHeader = findViewById(R.id.change_mobile_otp_menu_TVW_verificationHeader)
        enterOtpHeader = findViewById(R.id.change_mobile_otp_menu_TVW_header)
        exitButton = findViewById(R.id.change_mobile_otp_menu_BTN_exitButton)
        minterateLogo = findViewById(R.id.logo)
        codePicker = findViewById(R.id.change_mobile_menu_newCountryCode)
        codePickerBw = findViewById(R.id.change_mobile_menu_newCountryCode_bw)
    }

    private fun resendOTPvVisibility() {
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

    private fun resendVerificationCode() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(mobile).setTimeout(60L, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(callbacks).setForceResendingToken(resendToken).build()
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
                    updateMobile(userToken, mobile)
                    Toast.makeText(this, "Authenticate Successfully", Toast.LENGTH_SHORT).show()
                    sendToMainActivity()
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


    private fun sendToMainActivity() {
        soundManager = SoundManager(this@ChangeMobileMenuOtpActivity)
        soundManager.playLogin()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userToken", userToken)
        startActivity(intent)
    }


    private fun addTextChangeListener() {
        input1OTP.addTextChangedListener(EditTextWatcher(input1OTP))
        input2OTP.addTextChangedListener(EditTextWatcher(input2OTP))
        input3OTP.addTextChangedListener(EditTextWatcher(input3OTP))
        input4OTP.addTextChangedListener(EditTextWatcher(input4OTP))
        input5OTP.addTextChangedListener(EditTextWatcher(input5OTP))
        input6OTP.addTextChangedListener(EditTextWatcher(input6OTP))

    }


    inner class EditTextWatcher(input1OTP: AppCompatEditText) : TextWatcher {
        private val editTextList = listOf(this@ChangeMobileMenuOtpActivity.input1OTP, input2OTP, input3OTP, input4OTP, input5OTP, input6OTP)

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

    private fun updateMobile(userToken: String, newMobile: String) {
        val mobileUpdateRequest = MobileUpdateRequest(newMobile)
        val call: Call<Void> = retrofitInterface.updateMobile(userToken, mobileUpdateRequest)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ChangeMobileMenuOtpActivity,
                        "Mobile number updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response, display an error message or perform appropriate actions
                    progressBar.visibility = View.INVISIBLE
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangeMobileMenuOtpActivity,
                        "Failed to update mobile number. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle network or other failures
                progressBar.visibility = View.INVISIBLE
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeMobileMenuOtpActivity,
                    "Network error. Please check your connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            input1OTP, input2OTP, input3OTP, input4OTP, input5OTP, input6OTP,
            otpVerificationHeader, enterOtpHeader, resendOTP, changeMobileOtpMenuBTNVerify, mobileOTP
            // Include other TextViews and EditTexts as needed
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    private fun applyBlackAndWhiteMode() {
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isBWMode = preferences.getBoolean("isBlackAndWhiteMode", false)

        val elements = listOf(
            input1OTP, input2OTP, input3OTP, input4OTP, input5OTP, input6OTP
        )

        if (isBWMode) {
            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorWhiteGray))

            elements.forEach { element ->
                element.setTextColor(Color.WHITE)
                element.setBackgroundResource(R.drawable.rectangle_input_black)
            }
            otpVerificationHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            enterOtpHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            resendOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            receiveOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            mobileOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeMobileOtpMenuBTNVerify.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            changeMobileOtpMenuBTNVerify.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            progressBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))
            minterateLogo.setImageResource(R.drawable.minterate_b_and_w)
            minterateLogo.layoutParams.width = 160.dpToPx(this)
            minterateLogo.layoutParams.height = 80.dpToPx(this)
            minterateLogo.scaleType = ImageView.ScaleType.FIT_CENTER

            codePicker.visibility = View.INVISIBLE
            codePickerBw.visibility = View.VISIBLE
            codePickerBw.setBackgroundResource(R.drawable.rectangle_input_gray)


        } else {
            // Restore default colors
            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
            rootLayout.setBackgroundResource(R.drawable.general_background)

            elements.forEach { element ->
                element.setTextColor(Color.WHITE)
                element.setBackgroundResource(R.drawable.rectangle_input)
            }
            otpVerificationHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            enterOtpHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            resendOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            receiveOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            mobileOTP.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            changeMobileOtpMenuBTNVerify.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeMobileOtpMenuBTNVerify.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorYellow))
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorLightBlue))
            progressBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))
            minterateLogo.setImageResource(R.drawable.icon_minterate)

            codePicker.visibility = View.VISIBLE
            codePickerBw.visibility = View.INVISIBLE
            codePicker.setBackgroundResource(R.drawable.rectangle_input)
        }
    }

    // Extension function to convert dp to px
    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()


    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}