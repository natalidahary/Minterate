package com.example.myapplication.userActions

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.example.myapplication.databinding.ActivityBorrowBinding
import com.example.myapplication.requestResponse.GetILSToUserCurrencyResponse
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.userPreferences.AvailableLoansActivity
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class BorrowActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBorrowBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var amountTxt: AppCompatEditText
    private lateinit var periodTxt: AppCompatEditText
    private lateinit var periodHeader: AppCompatTextView
    private lateinit var showLoansButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var progressBarBorrow: ProgressBar
    private lateinit var presentedLoansHeader: AppCompatTextView
    private lateinit var borrowHeader: AppCompatTextView
    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String
    private lateinit var amount: String
    private lateinit var period: String

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        progressBarBorrow.visibility = View.INVISIBLE

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!


        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        binding.borrowBTNShowLoansButton.setOnClickListener {

            amount = binding.borrowEDTAmountTxt.text.toString()
            period = binding.borrowEDTPeriodTxt.text.toString()

            validateAmount { isValid ->
                if (isValid && validatePeriod()) {
                    soundManager.playClickSound()
                    val intent = Intent(this, AvailableLoansActivity::class.java)
                    intent.putExtra("desiredAmount", amount)
                    intent.putExtra("desiredPeriod", period)
                    startActivity(intent)
                } else {
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@BorrowActivity,
                        "All fields are mandatory",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.borrowBTNShowLoansButton.isEnabled = true  // Re-enable the button
                }
            }
        }
        binding.borrowBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun findViews() {
        amountTxt = findViewById(R.id.borrow_EDT_amountTxt)
        periodTxt = findViewById(R.id.borrow_EDT_periodTxt)
        showLoansButton = findViewById(R.id.borrow_BTN_showLoansButton)
        progressBarBorrow = findViewById(R.id.borrow_progressBar)
        exitButton = findViewById(R.id.borrow_BTN_exitButton)
        presentedLoansHeader = findViewById(R.id.borrow_TVW_presentedLoansHeader)
        borrowHeader = findViewById(R.id.borrow_TVW_header)
        periodHeader = findViewById(R.id.borrow_TVW_header)
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

    private fun validatePeriod(): Boolean {
        val period = period.toIntOrNull()
        if (period == null || period !in 1..60) {
            Toast.makeText(this, "Period must be between 1 and 60 months", Toast.LENGTH_SHORT).show()
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
            amountTxt, periodTxt, presentedLoansHeader,
            borrowHeader, periodHeader
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