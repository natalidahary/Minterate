package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.AppData
import com.example.myapplication.userActions.BorrowAgreementActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityChooseLoanBinding
import com.example.myapplication.requestResponse.ApproveLoanByLockRequest
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.UnlockLoanRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import kotlin.properties.Delegates

class ChooseLoanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseLoanBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private lateinit var exitButton: MaterialButton
    private lateinit var amountHeader: AppCompatTextView
    private lateinit var periodHeader: AppCompatTextView
    private lateinit var interestHeader: AppCompatTextView
    private lateinit var monthlyHeader: AppCompatTextView
    private lateinit var getLoanButton: MaterialButton
    private lateinit var amountLoan: AppCompatTextView
    private lateinit var periodLoan: AppCompatTextView
    private lateinit var interestLoan: AppCompatTextView
    private lateinit var monthlyLoan: AppCompatTextView
    private lateinit var userToken: String
    private lateinit var amount: String
    private lateinit var lId: String
    private lateinit var repaymentPeriod: String
    private lateinit var availabilityUntil: String
    private lateinit var interestRate: String
    private lateinit var calculatedLoanAmount: String
    private lateinit var calculatedMonthlyPayment: String
    private lateinit var userData: UserDataResponse
    private lateinit var loanData: LoanDataResponse
    private lateinit var desiredAmount: String
    private lateinit var desiredPeriod: String
    private lateinit var webView: WebView

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseLoanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        @Suppress("DEPRECATION")
        loanData = intent.getSerializableExtra("loan") as LoanDataResponse

        desiredAmount = intent.getStringExtra("desiredAmount").toString()
        desiredPeriod = intent.getStringExtra("desiredPeriod").toString()

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        lId = loanData.lId.toString()
        amount = loanData.amount.toString()
        repaymentPeriod = loanData.period.toString()
        availabilityUntil = loanData.expirationDate.toString()
        interestRate = loanData.interestRate.toString()
        calculatedLoanAmount = calculateLoanAmount(amount,interestRate)
        calculatedMonthlyPayment = calculateMonthlyRepayment(amount, interestRate, repaymentPeriod).toString()

        // Set the text for TextViews
        var formatAmount = formatWithCommas(amount)
        var formatMonthlyPayment = formatWithCommas(calculatedMonthlyPayment)
        var formatCurrency = extractCurrencyValue(loanData.currency.toString())
        amountLoan.text = "$formatAmount ${formatCurrency}"
        periodLoan.text = "$repaymentPeriod months"
        interestLoan.text = "$interestRate%"
        monthlyLoan.text = "$formatMonthlyPayment ${formatCurrency}"

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        binding.chooseLoanBTNGetLoan.setOnClickListener {
            soundManager.playClickSound()
            approveLoanByLock()
        }

        binding.chooseLoanBTNExitButton.setOnClickListener {
           exitPage()
        }

    }

    private fun findViews() {
        exitButton = findViewById(R.id.choose_loan_BTN_exitButton)
        getLoanButton = findViewById(R.id.choose_loan_BTN_getLoan)
        amountHeader = findViewById(R.id.choose_loan_TVW_amountHeader)
        periodHeader = findViewById(R.id.choose_loan_TVW_periodHeader)
        interestHeader = findViewById(R.id.choose_loan_TVW_interestHeader)
        monthlyHeader = findViewById(R.id.choose_loan_TVW_monthlyHeader)
        amountLoan = findViewById(R.id.choose_loan_TVW_amountLoan)
        periodLoan = findViewById(R.id.choose_loan_TVW_periodLoan)
        interestLoan = findViewById(R.id.choose_loan_TVW_interestLoan)
        monthlyLoan = findViewById(R.id.choose_loan_TVW_monthlyLoan)
    }


    fun formatWithCommas(input: String): String {
        val number = try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            return input // Return input as is if it's not a valid number
        }

        return String.format("%,.2f", number)
    }

    fun extractCurrencyValue(currencyString: String): String {
        val regex = Regex("\\((.*?)\\)")
        val matchResult = regex.find(currencyString)
        return matchResult?.groupValues?.get(1) ?: ""
    }

    fun calculateMonthlyRepayment(amount: String, rate: String, period: String): Double {
        // Convert strings to double
        val principal = amount.toDouble()
        val annualRate = rate.toDouble()
        val periods = period.toDouble()

        // Convert annual interest rate to monthly rate
        val monthlyRate = annualRate / 12.0 / 100.0

        // Calculate monthly repayment using the formula
        val numerator = monthlyRate * Math.pow(1 + monthlyRate, periods)
        val denominator = Math.pow(1 + monthlyRate, periods) - 1
        val monthlyRepayment = principal * (numerator / denominator)

        // Round to 2 digits after the decimal point
        return monthlyRepayment.roundTo2DecimalPlaces()
    }

    fun Double.roundTo2DecimalPlaces(): Double {
        val df = DecimalFormat("#.##")
        return df.format(this).toDouble()
    }


    fun calculateLoanAmount(amount: String, interestRate: String): String {
        val amountDouble = amount.toDoubleOrNull()
        val interestRateDouble = interestRate.toDoubleOrNull()

        if (amountDouble != null && interestRateDouble != null) {
            val loanAmount = amountDouble * (1 + interestRateDouble / 100.0)
            return loanAmount.roundTo2DecimalPlaces().toString()
        } else {
            return "Invalid input"
        }
    }

    private fun unlockLoan() {
        lifecycleScope.launch {
            try {
                val request = UnlockLoanRequest(userToken = userToken, loanId = lId)
                val response = retrofitInterface.unlockLoan(request)

                if (response.isSuccessful) {
                    val unlockLoanResponse = response.body()
                    unlockLoanResponse?.let {
                        // Handle the success response
                        //Do nothing
                    }
                } else {
                    // Do nothing
                }
            } catch (e: Exception) {
                // Handle the exception
                Toast.makeText(this@ChooseLoanActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun approveLoanByLock() {
        lifecycleScope.launch {
            try {
                val request = ApproveLoanByLockRequest(userToken = userToken, lId = lId)
                val response = retrofitInterface.approveLoanByLock(request)

                if (response.isSuccessful) {
                    val approveLoanResponse = response.body()
                    approveLoanResponse?.let {
                        // Handle the success response
                        // e.g., display a message to the user
                        Toast.makeText(this@ChooseLoanActivity, it.message, Toast.LENGTH_SHORT).show()

                        // Check if the response indicates approval, and navigate accordingly
                        if (it.message?.equals("Processing loan contract..", ignoreCase = true) == true)  {
                            val intent = Intent(this@ChooseLoanActivity, BorrowAgreementActivity::class.java)
                            intent.putExtra("userToken", userToken)
                            intent.putExtra("user", userData)
                            intent.putExtra("loan", loanData)
                            startActivity(intent)
                        } else {
                            unlockLoan()
                            Toast.makeText(
                                this@ChooseLoanActivity,
                                "Internal Error",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@ChooseLoanActivity, AvailableLoansActivity::class.java)
                            intent.putExtra("user", userData)
                            intent.putExtra("userToken", userToken)
                            intent.putExtra("amount", desiredAmount)
                            intent.putExtra("period", desiredPeriod)
                            startActivity(intent)
                        }
                    }
                } else {
                    unlockLoan()
                    // Handle the error response
                    Toast.makeText(
                        this@ChooseLoanActivity,
                        "Loan is already caught by another user",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@ChooseLoanActivity, AvailableLoansActivity::class.java)
                    intent.putExtra("user", userData)
                    intent.putExtra("userToken", userToken)
                    intent.putExtra("amount", desiredAmount)
                    intent.putExtra("period", desiredPeriod)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // Handle the exception
                Toast.makeText(this@ChooseLoanActivity, "Ops!: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textViews = listOf(
            amountLoan, periodLoan, interestLoan, monthlyLoan, amountHeader, periodHeader, interestHeader, monthlyHeader
            // Include other TextViews or EditTexts you want to scale
        )
        textViews.forEach { textView ->
            textView.textSize = textView.textSize * textScalar
        }
    }

    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        exitPage()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            exitPage()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            exitPage()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            exitPage()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun exitPage(){
        soundManager.playClickSound()
        unlockLoan()
        val intent = Intent(this, AvailableLoansActivity::class.java)
        intent.putExtra("desiredAmount", desiredAmount)
        intent.putExtra("desiredPeriod", desiredPeriod)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}