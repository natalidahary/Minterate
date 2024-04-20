package com.example.myapplication.userActions

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityLoanAgreementBinding
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.LoanDataRequest
import com.example.myapplication.requestResponse.LoanStatus
import com.example.myapplication.requestResponse.SaveLoanRequest
import com.example.myapplication.requestResponse.UserDataResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate



class LoanAgreementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoanAgreementBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String
    private lateinit var webView: WebView

    private lateinit var amount: String
    private lateinit var rate: String
    private lateinit var period: String
    private lateinit var expiration: String
    private lateinit var htmlContract: String

    private lateinit var lenderSignature: String
    private var isLoanSaved = false
    private var isLender: Boolean = true
    private var isFinish: Boolean = false


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanAgreementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAppInterface(this, userData, userToken, isLender, isFinish), "AndroidInterface")

        amount = intent.getStringExtra("amount").toString()
        rate = intent.getStringExtra("rate").toString()
        period = intent.getStringExtra("period").toString()
        expiration = intent.getStringExtra("expiration").toString()

        val formattedAmount = formatWithCommas(amount)
        val formattedMonthlyRepayment = formatWithCommas((calculateMonthlyRepayment(amount, rate, period) ?: 0.0).toString())

        htmlContract = getHtmlContent()
            .replace("[Full Name of Lender]", "${userData.firstName ?: ""} ${userData.lastName ?: ""}")
            .replace("[ID of Lender]", "${userData.id ?: ""}")
            .replace("[Address of Lender]", "${userData.address ?: ""}")
            .replace("[City, State]", "${userData.city ?: ""} ${userData.state ?: ""}")
            .replace("[Full Name of Borrower]", "[Full Name of Borrower]")
            .replace("[ID of Borrower]", "[ID of Borrower]")
            .replace("[Address of Borrower]", "[Address of Borrower]")
            .replace("[City, State of Borrower]", "[City, State of Borrower]")
            .replace("[Enter Loan Amount in words and numbers]", "${convertNumberToWords(amount)}<br>$formattedAmount ${userData.currency ?: ""}")
            .replace("[Enter Interest Rate]", rate ?: "")
            .replace("[Enter Repayment Period]", period ?: "")
            .replace("[Enter Monthly Payment Amount]", "$formattedMonthlyRepayment ${userData.currency}")
            .replace("[Enter Grace Period]", "14")
            .replace("[Enter Cure Period]", "7")

        webView.loadDataWithBaseURL(null, htmlContract, "text/html", "UTF-8", null)

    }

    fun formatWithCommas(input: String): String {
        val number = try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            return input // Return input as is if it's not a valid number
        }

        return String.format("%,.2f", number)
    }

    class WebAppInterface(private val activity: LoanAgreementActivity, private val userData: UserDataResponse, private  var userToken: String, private val isLender: Boolean, private val isFinish: Boolean) {
        @JavascriptInterface
        fun exitToMainActivity(lenderSignature: String) {
            if (activity is LoanAgreementActivity && !activity.isLoanSaved) {
                // Run network operation in a background thread
                Thread {
                    activity.saveLoan(lenderSignature = lenderSignature)
                    activity.runOnUiThread {
                        // Continue with UI operations on the main thread
                        val newActivityIntent = Intent(activity, MainActivity::class.java)
                        activity.startActivity(newActivityIntent)
                        Toast.makeText(activity, "Contract was saved successfully \nLoan has been published!", Toast.LENGTH_SHORT).show()
                        activity.finish()
                        activity.isLoanSaved = true
                    }
                }.start()
            } else {
                Toast.makeText(activity, "Error saving contract", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun closeActivity() {
            val newActivityIntent = Intent(activity, LendActivity::class.java)
            activity.startActivity(newActivityIntent)
            activity.finish()
        }

        @JavascriptInterface
        fun isLender(): Boolean {
            return isLender
        }

        @JavascriptInterface
        fun isFinish(): Boolean {
            return isFinish
        }

    }

    @SuppressLint("NewApi")
    fun getCurrentDate(): String {
        return LocalDate.now().toString()
    }

    fun saveLoan(lenderSignature: String) {
        this.lenderSignature = lenderSignature
        var currentDate = getCurrentDate()
        //this.borrowerSignature = borrowerSignature

        val signedHtmlContract = htmlContract
            // Replace placeholders with actual signature images
            .replace("[Lender Signature Image]", "<img src='$lenderSignature' alt=''/>")
            .replace("[Date of Lender]", currentDate ?: "[Date of Lender]")

        val loanData = LoanDataRequest(
            lenderId = userData.id,
            borrowerId = "",
            amount = amount.toDoubleOrNull() ?: 0.0,
            currency = userData.currency,
            period = period.toIntOrNull() ?: 0,
            interestRate = rate.toDoubleOrNull() ?: 0.0,
            startDate = " ",
            endDate = " ",
            expirationDate = expiration,
            status = LoanStatus.PENDING,
            contractHTML = signedHtmlContract
        )

        val saveLoanRequest = SaveLoanRequest(userToken, loanData)
        retrofitInterface.saveLoan(saveLoanRequest).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@LoanAgreementActivity, "Loan saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoanAgreementActivity, "Error saving loan: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@LoanAgreementActivity, "Network failure: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun getHtmlContent(): String {
        // Load HTML content from the assets folder
        val assetManager = applicationContext.assets
        val inputStream: InputStream
        try {
            inputStream = assetManager.open("loan_agreement.html")
        } catch (e: IOException) {
            e.printStackTrace()
            return "" // Handle the error as needed
        }

        // Convert the input stream to a string
        val htmlContent = inputStream.bufferedReader().use { it.readText() }

        // Return the HTML content
        return htmlContent
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


    fun convertNumberToWords(number: String): String {
        val ones = arrayOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
        val teens = arrayOf("eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
        val tens = arrayOf("", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

        var num = number.toInt()

        val words = StringBuilder()

        if (num == 0) {
            return "zero"
        }

        if (num < 0) {
            words.append("minus ")
            num = -num
        }

        if (num / 1000 > 0) {
            words.append(convertNumberToWords((num / 1000).toString())).append(" thousand ")
            num %= 1000
        }

        if (num / 100 > 0) {
            words.append(ones[num / 100]).append(" hundred ")
            num %= 100
        }

        if (num > 0) {
            if (words.isNotEmpty()) {
                words.append("and ")
            }

            when {
                num == 10 -> words.append("ten")
                num in 11..19 -> words.append(teens[num - 11])
                else -> {
                    words.append(tens[num / 10])
                    if (num % 10 > 0) {
                        words.append("-").append(ones[num % 10])
                    }
                }
            }
        }

        return words.toString().trim()
    }


}