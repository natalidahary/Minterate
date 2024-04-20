package com.example.myapplication.loginActivity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.ResendTokenWrapper
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.requestResponse.Transaction
import com.example.myapplication.databinding.ActivitySigninOtpBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class SigninOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninOtpBinding

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
    private lateinit var progressBar : ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var resendOTP: AppCompatTextView
    private lateinit var verifyButton: MaterialButton
    private  lateinit var otp: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var userToken: String
    private lateinit var mobile: String
    private lateinit var mobileOTP: AppCompatTextView
    private lateinit var exitButton: MaterialButton

    private lateinit var otpVerificationHeader: AppCompatTextView
    private lateinit var enterOtpHeader: AppCompatTextView
    private lateinit var appData: AppData
    private lateinit var soundManager: SoundManager

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        findViews()

        otp = intent.getStringExtra("otp").toString()

        @Suppress("DEPRECATION")

        val resendTokenWrapper = intent.getParcelableExtra<ResendTokenWrapper>("resendTokenWrapper")
        resendToken = resendTokenWrapper?.resendToken!!

        resendOTP.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        addTextChangeListener()
        resendOTPvVisibility()

        resendOTP.setOnClickListener{
            resendVerificationCode()
            resendOTPvVisibility()
        }

        userToken = intent.getStringExtra("userToken").toString()
        mobile = intent.getStringExtra("mobile").toString()
        mobileOTP.text = mobile
        soundManager = SoundManager(this)

        appData = AppData.getInstance()

        appData.userToken = userToken
        //soundManager.setSoundEnabled(userData.sounds)
        binding.signInOtpBTNVerify.setOnClickListener{

            val typdOTP = (input1OTP.text.toString() + input2OTP.text.toString() + input3OTP.text.toString() + input4OTP.text.toString()+ input5OTP.text.toString()+ input6OTP.text.toString())
            if(typdOTP.isNotEmpty()){
                if(typdOTP.length==6){
                    hideKeyboard()
                    val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(otp, typdOTP)
                    progressBar.visibility = View.VISIBLE
                    //signupOTP(typdOTP)
                    signInWithPhoneAuthCredential(credential)

                }else{
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@SigninOtpActivity, "Please enter correct OTP", Toast.LENGTH_SHORT).show()
                }
            }else{
                soundManager.playWrongClickSound()
                Toast.makeText(this@SigninOtpActivity, "Please enter OTP", Toast.LENGTH_SHORT).show()

            }
        }

        binding.signInOtpBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@SigninOtpActivity, LoginActivity::class.java))
            finish()
        }

    }


    private fun findViews() {
        input1OTP = findViewById(R.id.signIn_otp_EDT_input1OTP)
        input2OTP = findViewById(R.id.signIn_otp_EDT_input2OTP)
        input3OTP = findViewById(R.id.signIn_otp_EDT_input3OTP)
        input4OTP = findViewById(R.id.signIn_otp_EDT_input4OTP)
        input5OTP = findViewById(R.id.signIn_otp_EDT_input5OTP)
        input6OTP = findViewById(R.id.signIn_otp_EDT_input6OTP)
        progressBar = findViewById(R.id.signIn_otp_progressBar)
        exitButton = findViewById(R.id.signIn_otp_BTN_exitButton)
        otpVerificationHeader= findViewById(R.id.signIn_otp_TVW_verificationHeader)
        enterOtpHeader = findViewById(R.id.signIn_otp_TVW_header)
        resendOTP = findViewById(R.id.signIn_otp_TVW_resendOTP)
        verifyButton = findViewById(R.id.signIn_otp_BTN_verify)
        mobileOTP = findViewById(R.id.signIn_otp_TVW_mobileOTP)

    }

    private fun resendOTPvVisibility(){
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
                    getUserDataByToken(userToken)
                    fetchUserTransactions(userToken)
                    Log.d("##############", "###### failed: $userToken")
                    // Sign in success, update UI with the signed-in user's information
                    progressBar.visibility = View.VISIBLE
                    Toast.makeText(this, "Authenticate Successfully", Toast.LENGTH_SHORT).show()
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5000)
                        sendToMainActivity()
                    }
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
        soundManager = SoundManager(this@SigninOtpActivity)
        soundManager.playLogin()
        val intent = Intent(this, MainActivity::class.java)
        //intent.putExtra("userToken", userToken)
        //intent.putExtra("user", userData)
        /*if (userTransactions != null) {
            intent.putExtra("transactions", ArrayList(userTransactions))
        }*/
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusView = currentFocus
        currentFocusView?.let {
            inputMethodManager.hideSoftInputFromWindow(
                it.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
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
        private val editTextList = listOf(this@SigninOtpActivity.input1OTP, input2OTP, input3OTP, input4OTP, input5OTP, input6OTP)

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

    private fun getUserDataByToken(userToken: String) {
        val call: Call<UserDataResponse> = retrofitInterface.getUserDataByToken(userToken)

        call.enqueue(object : Callback<UserDataResponse> {
            override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        appData.userData = result
                    } else {
                        Log.e(ContentValues.TAG, "Null response body")
                    }

                } else {
                    Log.e(ContentValues.TAG, "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                Log.e(ContentValues.TAG, "Network call failed: ${t.message}")
            }
        })
    }

    private fun fetchUserTransactions(userToken: String) {
        val call: Call<List<Transaction>> = retrofitInterface.getUserTransactions(userToken)

        call.enqueue(object : Callback<List<Transaction>> {
            override fun onResponse(call: Call<List<Transaction>>, response: Response<List<Transaction>>) {
                if (response.isSuccessful) {
                    val transactions = response.body()
                    if (!transactions.isNullOrEmpty()) {
                        // Sort transactions by date in descending order
                        var userTransactions = transactions.sortedByDescending { it.date }
                        val appData = AppData.getInstance()
                        appData.transactions = userTransactions
                        //updateUI()
                        Log.d(ContentValues.TAG, "Transactions received and sorted in descending order successfully: $userTransactions")
                    } else {
                        Log.d(ContentValues.TAG, "No transactions found for the user")
                        // Handle the case where there are no transactions
                        // For example, you could show a message to the user
                    }
                } else {
                    Log.e(ContentValues.TAG, "Unsuccessful response: ${response.code()}")
                    // Handle the case where the request was not successful
                }
            }

            override fun onFailure(call: Call<List<Transaction>>, t: Throwable) {
                Log.e(ContentValues.TAG, "Network call failed: ${t.message}")
                // Handle the case where the network call failed
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

}
