package com.example.myapplication.userActions

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
    private lateinit var syndicationHeader2:AppCompatTextView
    private lateinit var consortiumHeader: AppCompatTextView
    private lateinit var consortiumHeader2: AppCompatTextView
    private lateinit var publishOfferHeader: AppCompatTextView
    private lateinit var publishOfferHeader2: AppCompatTextView
    private lateinit var minterateLogo: AppCompatImageView

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
        applyBlackAndWhiteMode()

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
        syndicationHeader2 = findViewById(R.id.lend_TVW_syndicationHeader2)
        consortiumHeader = findViewById(R.id.lend_TVW_consortiumHeader)
        consortiumHeader2 =  findViewById(R.id.lend_TVW_consortiumHeader2)
        publishOfferHeader = findViewById(R.id.lend_TVW_publishOfferHeader)
        publishOfferHeader2 = findViewById(R.id.lend_TVW_publishOfferHeader2)
        minterateLogo = findViewById(R.id.logo)
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
            syndicationHeader, syndicationHeader2, consortiumHeader, consortiumHeader2,  publishOfferHeader, publishOfferHeader2
        )
        textViews.forEach { textView ->
            textView.textSize = textView.textSize * textScalar
        }
    }

    private fun applyBlackAndWhiteMode() {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isBWMode = preferences.getBoolean("isBlackAndWhiteMode", false)

        val elements = listOf(
            amountText, rateText, periodText, expirationText
        )

        if (isBWMode) {
            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorWhiteGray))

            elements.forEach { element ->
                element.setTextColor(Color.WHITE)
                element.setHintTextColor(Color.WHITE)
                element.setBackgroundResource(R.drawable.rectangle_input_black)
            }
            allowLoanSyndication.setTextColor(Color.BLACK)
            allowLoanSyndication.buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))
            allowLoanConsortium.setTextColor(Color.BLACK)
            allowLoanConsortium.buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))
            lendHeader.setTextColor(Color.BLACK)
            conditionHeader.setTextColor(Color.BLACK)
            interestHeader.setTextColor(Color.BLACK)
            periodHeader.setTextColor(Color.BLACK)
            syndicationHeader.setTextColor(Color.BLACK)
            syndicationHeader2.setTextColor(Color.BLACK)
            consortiumHeader.setTextColor(Color.BLACK)
            consortiumHeader2.setTextColor(Color.BLACK)
            publishOfferHeader.setTextColor(Color.BLACK)
            publishOfferHeader2.setTextColor(Color.BLACK)
            approveButton.setTextColor(Color.WHITE)
            approveButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setTextColor(Color.WHITE)
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            progressBarLend.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))
            minterateLogo.setImageResource(R.drawable.minterate_b_and_w)
            minterateLogo.layoutParams.width = 160.dpToPx(this)
            minterateLogo.layoutParams.height = 80.dpToPx(this)
            minterateLogo.scaleType = ImageView.ScaleType.FIT_CENTER

        } else {
            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
            rootLayout.setBackgroundResource(R.drawable.lender_background)

            elements.forEach { element ->
                element.setTextColor(Color.WHITE)
                element.setHintTextColor(Color.BLACK)
                element.setBackgroundResource(R.drawable.rectangle_input)
            }
            allowLoanSyndication.setTextColor(Color.WHITE)
            allowLoanSyndication.buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))
            allowLoanConsortium.setTextColor(Color.WHITE)
            allowLoanConsortium.buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))
            lendHeader.setTextColor(Color.BLACK)
            conditionHeader.setTextColor(Color.BLACK)
            interestHeader.setTextColor(Color.BLACK)
            periodHeader.setTextColor(Color.BLACK)
            syndicationHeader.setTextColor(Color.WHITE)
            syndicationHeader2.setTextColor(Color.WHITE)
            consortiumHeader.setTextColor(Color.WHITE)
            consortiumHeader2.setTextColor(Color.WHITE)
            publishOfferHeader.setTextColor(Color.WHITE)
            publishOfferHeader2.setTextColor(Color.WHITE)
            approveButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            approveButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorYellow))
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorLightBlue))
            progressBarLend.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))
            minterateLogo.setImageResource(R.drawable.icon_minterate)
        }
    }

    // Extension function to convert dp to px
    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

}