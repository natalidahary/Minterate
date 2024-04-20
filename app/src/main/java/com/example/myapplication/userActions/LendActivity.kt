package com.example.myapplication.userActions

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityLendBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import com.example.myapplication.requestResponse.GetILSToUserCurrencyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.text.ParseException
import java.util.Calendar
import java.util.Locale
import kotlin.properties.Delegates

class LendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLendBinding


    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var amountText: AppCompatEditText
    private lateinit var rateText: AppCompatEditText
    private lateinit var periodText: AppCompatEditText
    private lateinit var expirationText: AppCompatEditText
    private lateinit var allowLoanSyndication: CheckBox
    private lateinit var allowLoanConsortium: CheckBox
    private lateinit var approveButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var progressBarLend: ProgressBar

    private lateinit var lendHeader: AppCompatTextView
    private lateinit var conditionHeader: AppCompatTextView
    private lateinit var interestHeader: AppCompatTextView
    private lateinit var periodHeader: AppCompatTextView
    private lateinit var syndicationHeader: AppCompatTextView
    private lateinit var consortiumHeader: AppCompatTextView
    private lateinit var publishOfferHeader: AppCompatTextView

    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String
    private lateinit var amount: String
    private lateinit var period: String
    private lateinit var rate: String
    private lateinit var expiration: String

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    //private var isAprrove = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        progressBarLend.visibility = View.INVISIBLE

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!


        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        allowLoanSyndication.isChecked
        allowLoanConsortium.isChecked

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        binding.lendEDTExpirationText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed as text changes
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.length == 2 && !text.contains("/")) {
                    // Insert '/' after typing the day
                    val newText = StringBuilder(text)
                    newText.insert(2, "/")
                    binding.lendEDTExpirationText.setText(newText)
                    binding.lendEDTExpirationText.setSelection(newText.length)
                } else if (text.length == 5 && !text.substring(3).contains("/")) {
                    // Insert '/' after typing the month
                    val newText = StringBuilder(text)
                    newText.insert(5, "/")
                    binding.lendEDTExpirationText.setText(newText)
                    binding.lendEDTExpirationText.setSelection(newText.length)
                } else if (text.length > 10) {
                    // Truncate the text if it exceeds 10 characters
                    val truncatedText = text.substring(0, 10)
                    binding.lendEDTExpirationText.setText(truncatedText)
                    binding.lendEDTExpirationText.setSelection(truncatedText.length)
                }
            }
        })


        binding.lendBTNApproveButton.setOnClickListener {

            amount = binding.lendEDTAmountText.text.toString()
            rate = binding.lendEDTRateText.text.toString()
            period = binding.lendEDTPeriodText.text.toString()
            expiration = binding.lendEDTExpirationText.text.toString()

            val isSyndication: Boolean = allowLoanSyndication.isChecked
            val isConsortium: Boolean = allowLoanConsortium.isChecked

            validateAmount { isValid ->
                if (isValid && validateRate() && validatePeriod() && validateExpiration()) {
                    soundManager.playClickSound()
                    val intent = Intent(this, LoanAgreementActivity::class.java)
                    intent.putExtra("amount" ,amount)
                    intent.putExtra("rate" ,rate)
                    intent.putExtra("period" ,period)
                    intent.putExtra("expiration" ,expiration)
                    startActivity(intent)
                } else {
                    soundManager.playWrongClickSound()
                    Toast.makeText(this@LendActivity, "All fields are mandatory", Toast.LENGTH_SHORT).show()
                    binding.lendBTNApproveButton.isEnabled = true  // Re-enable the button
                }
            }
        }
        binding.lendBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        amountText = findViewById(R.id.lend_EDT_amountText)
        rateText = findViewById(R.id.lend_EDT_rateText)
        periodText = findViewById(R.id.lend_EDT_periodText)
        expirationText = findViewById(R.id.lend_EDT_expirationText)
        allowLoanSyndication = findViewById(R.id.lend_allowLoanSyndication)
        allowLoanConsortium = findViewById(R.id.lend_allowLoanConsortium)
        approveButton = findViewById(R.id.lend_BTN_approveButton)
        exitButton = findViewById(R.id.lend_BTN_exitButton)
        progressBarLend = findViewById(R.id.lend_progressBar)
        lendHeader= findViewById(R.id.lend_BTN_header)
        conditionHeader = findViewById(R.id.lend_TVW_conditionHeader)
        interestHeader = findViewById(R.id.lend_TVW_interestHeader)
        periodHeader = findViewById(R.id.lend_TVW_periodHeader)
        syndicationHeader =  findViewById(R.id.lend_TVW_syndicationHeader)
        consortiumHeader = findViewById(R.id.lend_TVW_consortiumHeader)
        publishOfferHeader = findViewById(R.id.lend_TVW_publishOfferHeader)
    }

    private fun validateAmount(callback: (Boolean) -> Unit) {
        val amount = amount.toDoubleOrNull()
        var maxAmount: Double? = null
        retrieveMaxAmount { result ->
            maxAmount = result
            if (amount == null || maxAmount == null || amount !in 100.0..maxAmount!!) {
                Toast.makeText(this, "Amount must be between 100 and ${maxAmount ?: 0.0}", Toast.LENGTH_SHORT).show()
                callback(false)
            } else {
                callback(true)
            }
        }
    }

    private fun retrieveMaxAmount(callback: (Double) -> Unit) {
        if (userData.currency.contains("ILS")) {
            callback(30000.0)
        } else {
            val call = retrofitInterface.getILSToUserCurrencyExchangeRate(userToken)
            call.enqueue(object : Callback<GetILSToUserCurrencyResponse> {
                override fun onResponse(call: Call<GetILSToUserCurrencyResponse>, response: Response<GetILSToUserCurrencyResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val equivalentAmount = body?.equivalentAmount ?: 0.0
                        callback(equivalentAmount)
                    } else {
                        // Handle unsuccessful response
                        callback(0.0)
                    }
                }

                override fun onFailure(call: Call<GetILSToUserCurrencyResponse>, t: Throwable) {
                    // Handle network or other failures
                    callback(0.0)
                }
            })
        }
    }

    private fun validateRate(): Boolean {
        val rate = rate.toDoubleOrNull()
        if (rate == null || rate !in 0.0..15.0) {
            Toast.makeText(this, "Interest rate must be between 0 and 15", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun validatePeriod(): Boolean {
        val period = period.toIntOrNull()
        if (period == null || period !in 1..60) {
            Toast.makeText(this, "Period must be between 1 and 60 months", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun validateExpiration(): Boolean {
        val expirationDateString = expiration
        val periodMonths = period.toIntOrNull() ?: 0

        val expiration = try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expirationDateString)
        } catch (e: ParseException) {
            null
        }

        val currentDatePlusPeriod = Calendar.getInstance().apply {
            add(Calendar.MONTH, periodMonths)
            // Set to end of the day for comparison
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        if (expiration == null || !expiration.after(currentDatePlusPeriod)) {
            Toast.makeText(this, "Expiration date must be valid and greater than today's date plus the period", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textViews = listOf(
            amountText, rateText, periodText, expirationText,
            lendHeader, conditionHeader, interestHeader, periodHeader,
            syndicationHeader, consortiumHeader, publishOfferHeader
        )
        textViews.forEach { textView ->
            textView.textSize = textView.textSize * textScalar
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

}