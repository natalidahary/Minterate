package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityChangeCreditMenuBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.properties.Delegates

class ChangeCreditMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangeCreditMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var defaultCardNum: AppCompatTextView
    private lateinit var defaultName: AppCompatTextView
    private lateinit var defaultExpiration: AppCompatTextView
    private lateinit var changeCreditMenuBTNExitButton: MaterialButton
    private lateinit var creditNumber: AppCompatEditText
    private lateinit var cvv: AppCompatEditText
    private lateinit var monthYearCredit: AppCompatEditText
    private lateinit var changeCreditMenuBTNSave: MaterialButton
    private lateinit var userToken: String

    private lateinit var changeCreditHeader: AppCompatTextView
    private lateinit var defaultPaymentHeader: AppCompatTextView
    private lateinit var progressBarChangeCredit: ProgressBar
    private lateinit var userData: UserDataResponse
    private lateinit var newCvv: String
    private lateinit var newMonthYear: String

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeCreditMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        progressBarChangeCredit.visibility = View.INVISIBLE
        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        updateUI()

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        binding.changeCreditMenuEDTMonthYear.addTextChangedListener(object : TextWatcher {
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
                    binding.changeCreditMenuEDTMonthYear.setText(newText)
                    binding.changeCreditMenuEDTMonthYear.setSelection(newText.length)
                } else if (text.length > 5) {
                    // Truncate the text if it exceeds 5 characters
                    val truncatedText = text.substring(0, 5)
                    binding.changeCreditMenuEDTMonthYear.setText(truncatedText)
                    binding.changeCreditMenuEDTMonthYear.setSelection(truncatedText.length)
                }
            }
        })

        binding.changeCreditMenuBTNSave.setOnClickListener {
            val creditNumber = binding.changeCreditMenuEDTCreditNumber.text.toString()
            newCvv = binding.changeCreditMenuEDTCvv.text.toString()
            newMonthYear = binding.changeCreditMenuEDTMonthYear.text.toString()

            if (creditNumber.isNotEmpty() && newMonthYear.isNotEmpty() && newCvv.isNotEmpty()) {
                isCardNumberValid(creditNumber) { isValidCardNumber ->
                    if (isValidCardNumber && isExpirationDateValid(newMonthYear) && isCVVValid(newCvv)) {
                        soundManager.playClickSound()
                        val lastFourDigits = creditNumber.substring(creditNumber.length - 4)
                        val hashedCredit = hashCredit(creditNumber)
                        updateCredit(
                            userToken,
                            lastFourDigits,
                            hashedCredit,
                            newMonthYear,
                            newCvv
                        )
                        recreate()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        soundManager.playWrongClickSound()
                        // Show the corresponding error message
                        if (!isValidCardNumber) {
                            Toast.makeText(
                                this@ChangeCreditMenuActivity,
                                "Invalid card number",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (!isExpirationDateValid(newMonthYear)) {
                            Toast.makeText(
                                this@ChangeCreditMenuActivity,
                                "Invalid expiration date",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (!isCVVValid(newCvv)) {
                            Toast.makeText(
                                this@ChangeCreditMenuActivity,
                                "Invalid CVV.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ChangeCreditMenuActivity,
                                "All fields are mandatory",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }


        binding.changeCreditMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        defaultCardNum = findViewById(R.id.change_credit_menu_TVW_defaultCardNum)
        defaultName = findViewById(R.id.change_credit_menu_TVW_defaultName)
        defaultExpiration = findViewById(R.id.change_credit_menu_TVW_defaultExpiration)
        creditNumber =findViewById(R.id.change_credit_menu_EDT_creditNumber)
        cvv = findViewById(R.id.change_credit_menu_EDT_cvv)
        monthYearCredit = findViewById(R.id.change_credit_menu_EDT_monthYear)
        changeCreditMenuBTNExitButton = findViewById(R.id.change_credit_menu_BTN_exitButton)
        changeCreditMenuBTNSave = findViewById(R.id.change_credit_menu_BTN_save)
        progressBarChangeCredit = findViewById(R.id.change_credit_menu_progressBar)
        changeCreditHeader = findViewById(R.id.change_credit_menu_CreditHeader)
        defaultPaymentHeader =findViewById(R.id.change_credit_menu_TVW_defaultPaymentHeader)
    }


    private fun maskCreditCardNumber(creditCardNumber: String): String {
        // Check if the input string is not null and has exactly 16 digits
        if (creditCardNumber.length == 16) {
            // Use substring to extract the last four digits
            val lastFourDigits = creditCardNumber.substring(12)
            // Use replace to replace the first 12 digits with stars
            val maskedNumber = "**** **** **** $lastFourDigits"
            return maskedNumber
        } else {
            // If the input is invalid, return the original string
            return creditCardNumber
        }
    }


    private fun updateCredit(userToken: String, lastFourDigits: String, cardNumber: String, monthYear: String, cvv: String) {
        val updateRequest = com.example.myapplication.requestResponse.CreditUpdateRequest(
            lastFourDigits,
            cardNumber,
            monthYear,
            cvv
        )
        val call: Call<Void> = retrofitInterface.updateUserCredit(userToken,updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // updateUI()
                    Toast.makeText(
                        this@ChangeCreditMenuActivity,
                        "Credit information updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangeCreditMenuActivity,
                        "Failed to update credit information",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeCreditMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateUI() {
        defaultCardNum.text = "**** **** **** ${(userData?.lastFourDigits ?: "**** **** **** 1111")}"
        defaultName.text ="${userData?.firstName ?: ""} ${userData?.lastName ?: ""}"
        defaultExpiration.text ="${userData?.monthYear?: "09/29"}"
    }

    private fun isCardNumberValid(cardNumber: String, callback: (Boolean) -> Unit) {
        // Perform the local credit card number validation
        val localCardValidation = cardNumber.length == 16 && cardNumber.all { it.isDigit() }

        if (localCardValidation) {
            val hashedCardNumber = hashCredit(cardNumber)
            // Credit card format is valid, check if it already exists
            val map = HashMap<String, String>().apply {
                put("cardNumber", hashedCardNumber)
                put("monthYear", newMonthYear)
                put("cvv", newCvv)
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
        Toast.makeText(this@ChangeCreditMenuActivity, message, Toast.LENGTH_SHORT).show()
    }


    private fun isCVVValid(cvv: String): Boolean {
        // Check length
        if (cvv.length != 3 && cvv.length != 4) {
            return false
        }
        // Check numeric value
        return cvv.all { it.isDigit() }
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


    private fun hashCredit(credit: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val creditBytes = credit.toByteArray(StandardCharsets.UTF_8)
        val hashedBytes = messageDigest.digest(creditBytes)
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            defaultCardNum, defaultName, defaultExpiration,
            creditNumber, cvv, monthYearCredit,
            changeCreditHeader, defaultPaymentHeader
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