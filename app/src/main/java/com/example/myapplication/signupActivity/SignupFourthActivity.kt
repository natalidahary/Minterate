package com.example.myapplication.signupActivity

import android.app.Dialog
import android.content.Intent
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivitySignupFourthBinding
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class SignupFourthActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupFourthBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private lateinit var cardNumEditText: AppCompatEditText
    private lateinit var monthYearEditText: AppCompatEditText
    private lateinit var cvvEditText: AppCompatEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var confirmationCheckBox: CheckBox
    private lateinit var mProgressBar: ProgressBar
    private lateinit var termsLink: AppCompatTextView
    private lateinit var exitButton: MaterialButton
    private lateinit var almostThereHeader: AppCompatTextView
    private lateinit var creditInformationHeader: AppCompatTextView

    private lateinit var soundManager: SoundManager
    private var isSigningUp = false

    private lateinit var email: String
    private lateinit var password: String
    private lateinit var mobile: String
    //personal information
    private lateinit var firstName: String
    private lateinit var lastName: String
    private lateinit var dob: String
    private lateinit var id: String
    private lateinit var address: String
    private lateinit var city: String
    private lateinit var state: String
    //credit information
    private lateinit var signupCardNumber: String
    private lateinit var creditLastFourDigits: String
    private lateinit var signupMonthYear: String
    private lateinit var signupCvv: String
    private lateinit var defaultCurrency: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupFourthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        mProgressBar.visibility = View.INVISIBLE

        extractIntentExtras()

        defaultCurrency = "Israeli new shekel (ILS ₪)"

        soundManager = SoundManager(this)
        binding.signupFourthTVWTermsLink.setOnClickListener {
            soundManager.playClickSound()
            val dialog = Dialog(this@SignupFourthActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.activity_terms_privacy) // Replace with the actual layout of the terms and privacy dialog

            val exitButton = dialog.findViewById<Button>(R.id.update_password_login_BTN_exitButton)
            exitButton.setOnClickListener {
                soundManager.playClickSound()
                dialog.dismiss()
            }
            dialog.show()
        }

        binding.signupFourthEDTMonthYear.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed as text changes
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.length == 2 && !text.contains("/")) {
                    // Insert '/' after typing the month
                    val newText = StringBuilder(text)
                    newText.insert(2, "/")
                    binding.signupFourthEDTMonthYear.setText(newText)
                    binding.signupFourthEDTMonthYear.setSelection(newText.length)
                } else if (text.length > 5) {
                    // Truncate the text if it exceeds 5 characters
                    val truncatedText = text.substring(0, 5)
                    binding.signupFourthEDTMonthYear.setText(truncatedText)
                    binding.signupFourthEDTMonthYear.setSelection(truncatedText.length)
                }
            }
        })

        binding.signupFourthBTNSignupButton.setOnClickListener {
            if (!isSigningUp) {  // Check if signup is not already in progress
                isSigningUp = true
                // Disable the button to prevent multiple clicks
                binding.signupFourthBTNSignupButton.isEnabled = false

                signupCardNumber = binding.signupFourthEDTCardNumber.text.toString()
                signupMonthYear = binding.signupFourthEDTMonthYear.text.toString()
                signupCvv = binding.signupFourthEDTCvv.text.toString()
                val isConfirmed: Boolean = confirmationCheckBox.isChecked

                // Use the callback for isCardNumberValid
                isCardNumberValid(signupCardNumber) { isValidCardNumber ->
                    if (isValidCardNumber && isExpirationDateValid(signupMonthYear) && isCVVValid(signupCvv) && isConfirmed) {
                        signupUser()
                    } else {
                        soundManager.playWrongClickSound()
                        // Show the corresponding error message
                        if (!isValidCardNumber) {
                            Toast.makeText(this@SignupFourthActivity, "Invalid card number", Toast.LENGTH_SHORT).show()
                        } else if (!isExpirationDateValid(signupMonthYear)) {
                            Toast.makeText(this@SignupFourthActivity, "Invalid expiration date", Toast.LENGTH_SHORT).show()
                        } else if (!isCVVValid(signupCvv)) {
                            Toast.makeText(this@SignupFourthActivity, "Invalid CVV.", Toast.LENGTH_SHORT).show()
                        } else if (!isConfirmed) {
                            Toast.makeText(this@SignupFourthActivity, "Please confirm the terms and conditions", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SignupFourthActivity, "All fields are mandatory", Toast.LENGTH_SHORT).show()
                        }
                        isSigningUp = false
                        binding.signupFourthBTNSignupButton.isEnabled = true  // Re-enable the button
                    }
                }
            }
        }
        binding.signupFourthBTNExitButton.setOnClickListener {
            startActivity(Intent(this@SignupFourthActivity, SignupThirdActivity::class.java))
            finish()
        }
    }

    private fun findViews() {
        cardNumEditText = findViewById(R.id.signup_fourth_EDT_cardNumber)
        monthYearEditText = findViewById(R.id.signup_fourth_EDT_monthYear)
        cvvEditText = findViewById(R.id.signup_fourth_EDT_cvv)
        signupButton = findViewById(R.id.signup_fourth_BTN_signupButton)
        confirmationCheckBox = findViewById(R.id.signup_fourth_confirmationCheckBox)
        mProgressBar = findViewById(R.id.signup_fourth_progressBar)
        termsLink = findViewById(R.id.signup_fourth_TVW_termsLink)
        exitButton = findViewById(R.id.signup_fourth_BTN_exitButton)
        almostThereHeader = findViewById(R.id.signup_fourth_TVW_almostThereHeader)
        creditInformationHeader = findViewById(R.id.signup_fourth_TVW_creditInformationHeader)
    }

    private fun extractIntentExtras() {
        email = intent.getStringExtra("email").toString()
        password = intent.getStringExtra("password").toString()
        mobile = intent.getStringExtra("mobile").toString()
        firstName = intent.getStringExtra("firstName").toString()
        lastName = intent.getStringExtra("lastName").toString()
        dob = intent.getStringExtra("dob").toString()
        id = intent.getStringExtra("id").toString()
        address = intent.getStringExtra("address").toString()
        city = intent.getStringExtra("city").toString()
        state = intent.getStringExtra("state").toString()
    }



    private fun signupUser() {
        soundManager = SoundManager(this@SignupFourthActivity)

        val personalInfo = HashMap<String, Any>().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            put("dob", dob)
            put("id", id)
            put("address", address)
            put("city", city)
            put("state", state)
        }

        val credentials = HashMap<String, Any>().apply {
            put("lastFourDigits", creditLastFourDigits)
            put("cardNumber", signupCardNumber)
            put("monthYear", signupMonthYear)
            put("cvv", signupCvv)
        }

        val user = HashMap<String, Any>().apply {
            put("email", email)
            put("password", password)
            put("mobile", mobile)
            put("personalInfo", personalInfo)
            put("credentials", credentials)
            put("totalBalance", 0.0)
            put("textScalar", 0.36)
            put("sounds", true)
            put("currency", defaultCurrency)
        }

        retrofitInterface.executeSignup(user).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 200) {
                    // Retrofit call was successful, show success message
                    soundManager.playClickSound()
                    Toast.makeText(
                        this@SignupFourthActivity, "Signed up successfully", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignupFourthActivity, LoginActivity::class.java))
                    finish()
                } else if (response.code() == 400) {
                    Toast.makeText(this@SignupFourthActivity, "Already registered", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@SignupFourthActivity, t.message, Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun isExpirationDateValid(expirationDate: String): Boolean {
        // Assuming the format is XX/XX
        val dateParts = expirationDate.split("/")
        if (dateParts.size == 2) {
            val month = dateParts[0].toIntOrNull()
            val year = dateParts[1].toIntOrNull()

            // Check if month and year are valid
            if (month in 1..12 && year != null) {
                // Get the current date
                val currentDate = Calendar.getInstance()

                // Set the expiration date
                val expirationCalendar = Calendar.getInstance()
                if (month != null) {
                    expirationCalendar.set(Calendar.MONTH, month - 1)
                }  // Month is 0-based
                expirationCalendar.set(Calendar.YEAR, year + 2000)  // Assuming the year is given as YY

                // Check if the expiration date is at least 3 years from the current date
                expirationCalendar.add(Calendar.YEAR, -3)
                return expirationCalendar.after(currentDate)
            }
        }
        return false
    }

    private fun isCardNumberValid(cardNumber: String, callback: (Boolean) -> Unit) {
        // Perform the local credit card number validation
        val localCardValidation = cardNumber.length == 16 && cardNumber.all { it.isDigit() }

        if (localCardValidation) {
            creditLastFourDigits = getLastFourChars(cardNumber)
            signupCardNumber = hashCredit(cardNumber)
            // Credit card format is valid, check if it already exists
            val map = HashMap<String, String>().apply {
                put("cardNumber", signupCardNumber)
                put("monthYear", signupMonthYear)
                put("cvv", signupCvv)
            }
            val call = retrofitInterface.checkCreditExists(map)

            call.enqueue(object : Callback<com.example.myapplication.requestResponse.CreditCheckResponse> {
                override fun onResponse(call: Call<com.example.myapplication.requestResponse.CreditCheckResponse>, response: Response<com.example.myapplication.requestResponse.CreditCheckResponse>) {
                    if (response.isSuccessful) {
                        val creditCheckResponse = response.body()
                        if (creditCheckResponse != null) {
                            if (creditCheckResponse.exists) {
                                // Credit card number is valid
                                showToast("Credit card is valid")
                                callback(true)
                            } else {
                                // Server response indicates credit card number already exists
                                showToast("Credit card number already exists")
                                callback(false)
                            }
                        }
                    } else {
                        // Handle different HTTP status codes
                        when (response.code()) {
                            409 -> showToast("Credit card number already exists")
                            500 -> showToast("Server response error")
                            404 -> showToast("Credit card not recognized")
                            else -> showToast("Unexpected error")
                        }
                        callback(false)
                    }
                }

                override fun onFailure(call: Call<com.example.myapplication.requestResponse.CreditCheckResponse>, t: Throwable) {
                    // Handle network or other failures
                    showToast("Network error")
                    callback(false)
                }
            })
        } else {
            showToast("Invalid credit card number")
            callback(false)
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this@SignupFourthActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun isCVVValid(cvv: String): Boolean {
        // Check length
        if (cvv.length != 3 && cvv.length != 4) {
            return false
        }
        // Check numeric value
        return cvv.all { it.isDigit() }
    }

    private fun hashCredit(credit: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val creditBytes = signupCardNumber.toByteArray(StandardCharsets.UTF_8)
        val hashedBytes = messageDigest.digest(creditBytes)
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }

    private fun getLastFourChars(input: String): String {
        return if (input.length >= 4) {
            input.substring(input.length - 4)
        } else {
            // If the input string is less than 4 characters, return the entire string
            input
        }
    }
}