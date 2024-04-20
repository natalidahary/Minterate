package com.example.myapplication.userActions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityLoanAgreementBorrowBinding
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.LoanDataRequest
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.LoanStatus
import com.example.myapplication.requestResponse.UnlockLoanRequest
import com.example.myapplication.requestResponse.UpdateAndAddLoanRequest
import com.example.myapplication.requestResponse.UserDataResponse
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class BorrowAgreementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoanAgreementBorrowBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var userData: UserDataResponse
    private lateinit var loanData: LoanDataResponse
    private lateinit var userToken: String
    private lateinit var webView: WebView

    private lateinit var amount: String
    private lateinit var lId: String
    private lateinit var availabilityUntil: String
    private lateinit var interestRate: String
    private lateinit var calculatedLoanAmount: String
    private lateinit var calculatedMonthlyPayment: String
    private lateinit var rate: String
    private lateinit var period: String
    private lateinit var expiration: String
    private var isLender: Boolean = false
    private var isFinish: Boolean = false
    private lateinit var htmlContract: String
    private lateinit var borrowerSignature: String

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanAgreementBorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)


        @Suppress("DEPRECATION")
        userData = intent.getSerializableExtra("user") as UserDataResponse

        userToken = intent.getStringExtra("userToken").toString()

        @Suppress("DEPRECATION")
        loanData = intent.getSerializableExtra("loan") as LoanDataResponse
        //Log.d("LoanData", "Loan idddddddddddddddd: ${loanData.lId}")


        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(
            WebAppInterface(
                this,
                userData,
                userToken,
                isLender,
                isFinish
            ), "AndroidInterface")

        lId = loanData.lId.toString()
        amount = loanData.amount.toString()
        rate = loanData.interestRate.toString()
        period = loanData.period.toString()
        expiration = loanData.expirationDate.toString()
        availabilityUntil = loanData.expirationDate.toString()
        interestRate = loanData.interestRate.toString()
        calculatedLoanAmount = calculateLoanAmount(amount,interestRate)
        calculatedMonthlyPayment = calculateMonthlyRepayment(amount, interestRate, period).toString()


        htmlContract = getHtmlContent()
            .replace("[Full Name of Borrower]", "${userData.firstName ?: ""} ${userData.lastName ?: ""}")
            .replace("[ID of Borrower]", "${userData.id ?: ""}")
            .replace("[Address of Borrower]", "${userData.address ?: ""}")
            .replace("[City, State of Borrower]", "${userData.city ?: ""} ${userData.state ?: ""}")
            .replace("[Not yet determined.]", "${getCurrentDate()}")
            .replace("[Not yet determined]", "${getFirstRepaymentDate()}")


        webView.loadDataWithBaseURL(null, htmlContract, "text/html", "UTF-8", null)


    }

    class WebAppInterface(private val activity: Activity, private val userData: UserDataResponse, private  var userToken: String, private val isLender: Boolean, private val isFinish: Boolean) {
        @JavascriptInterface
        fun exitToMainActivity(borrowerSignature: String) {
            if (activity is BorrowAgreementActivity) {
                // Run network operation in a background thread
                Thread {
                    activity.saveLoan(borrowerSignature= borrowerSignature)
                    activity.runOnUiThread {
                        // Continue with UI operations on the main thread
                        val newActivityIntent = Intent(activity, MainActivity::class.java)
                        newActivityIntent.putExtra("userToken", userToken)
                        newActivityIntent.putExtra("user", userData)
                        activity.startActivity(newActivityIntent)
                        activity.finish()
                    }
                }.start()
            } else {
                Toast.makeText(activity, "Error saving contract", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun closeActivity() {
            if(activity is BorrowAgreementActivity) {
                activity.unlockLoan()
                val newActivityIntent = Intent(activity, BorrowActivity::class.java)
                newActivityIntent.putExtra("userToken", userToken)
                newActivityIntent.putExtra("user", userData)
                activity.startActivity(newActivityIntent)
                activity.finish()
            } else{
                Toast.makeText(activity, "An error occurred", Toast.LENGTH_SHORT).show()
                val newActivityIntent = Intent(activity, BorrowActivity::class.java)
                newActivityIntent.putExtra("userToken", userToken)
                newActivityIntent.putExtra("user", userData)
                activity.startActivity(newActivityIntent)
                activity.finish()
            }
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


    fun getFirstRepaymentDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1) // Add one month to the current date

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }



    @SuppressLint("NewApi")
    fun getCurrentDate(): String {
        return LocalDate.now().toString()
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

    private fun saveLoan(borrowerSignature: String) {
        this.borrowerSignature = borrowerSignature
        val currentDate = getCurrentDate()

        val signedHtmlContract = htmlContract
            .replace("[Borrower Signature Image]", "<img src='$borrowerSignature' alt=''/>")
            .replace("[Date of Borrower]", currentDate ?: "[Date of Lender]")

        val updatedLoanData = LoanDataRequest(
            lenderId = loanData.lenderId,
            borrowerId = userData.id,
            amount = amount.toDoubleOrNull() ?: 0.0,
            currency = loanData.currency,
            period = period.toIntOrNull() ?: 0,
            interestRate = rate.toDoubleOrNull() ?: 0.0,
            lId = lId,
            startDate = getCurrentDate(),
            endDate = getEndDate(period),
            expirationDate = expiration,
            status = LoanStatus.ACTIVE,
            contractHTML = signedHtmlContract
        )

        val updateAndAddLoanRequest = UpdateAndAddLoanRequest(userToken, lId, updatedLoanData)

        retrofitInterface.updateAndAddLoan(updateAndAddLoanRequest).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    // The HTTP response code indicates success (2xx)
                    Toast.makeText(this@BorrowAgreementActivity, "Loan updated successfully", Toast.LENGTH_SHORT).show()
                    // Optional: navigate to another screen or perform another action
                } else {
                    // The HTTP response code indicates an error
                    Toast.makeText(this@BorrowAgreementActivity, "Error updating loan: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@BorrowAgreementActivity, "Network failure: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    fun getEndDate(monthsToAddStr: String): String {
      val monthsToAdd = monthsToAddStr.toIntOrNull() ?: 0 // Convert to Int, default to 0 if conversion fails

      val calendar = Calendar.getInstance()
      calendar.add(Calendar.MONTH, monthsToAdd)

      val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      return dateFormat.format(calendar.time)
    }

    private fun getHtmlContent(): String {
      // Return the HTML content as a string
      // (You can copy the HTML content here)
      return loanData.contractHTML.toString()

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
                Toast.makeText(this@BorrowAgreementActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
