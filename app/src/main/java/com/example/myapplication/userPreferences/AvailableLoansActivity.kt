package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.AppData
import com.example.myapplication.userActions.BorrowActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityAvailableLoansBinding
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.LockLoanRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import kotlin.properties.Delegates

class AvailableLoansActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAvailableLoansBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private var sortByInterestRateAsc = true
    private var sortByRepaymentPeriodAsc = true

    private lateinit var exitButton: MaterialButton
    private lateinit var availableHeader: AppCompatTextView
    private lateinit var loanIdTextView: AppCompatTextView
    private lateinit var amountTextView: AppCompatTextView
    private lateinit var repaymentPeriodTextView: AppCompatTextView
    private lateinit var availabilityUntilTextView: AppCompatTextView
    private lateinit var interestRateTextView: AppCompatTextView
    private lateinit var calculatedLoanAmountTextView: AppCompatTextView
    private lateinit var calculatedMonthlyPaymentTextView: AppCompatTextView
    private lateinit var textAvailableLoans: AppCompatTextView
    private lateinit var sortRedirect: AppCompatTextView
    private lateinit var loansProgressBar: ProgressBar
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse
    private lateinit var desiredAmount: String
    private lateinit var desiredPeriod: String
    private lateinit var loans: List<LoanDataResponse>
    private lateinit var lId: String
    private var isDataLoading: Boolean = false

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvailableLoansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        loansProgressBar.visibility = View.VISIBLE
        textAvailableLoans.visibility = View.VISIBLE
        loans = emptyList()


        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        desiredAmount = intent.getStringExtra("desiredAmount").toString()
        desiredPeriod = intent.getStringExtra("desiredPeriod").toString()

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        retrieveLoans()

        // Set click listener for sort button
        binding.availableLoansTVWSortRedirect.setOnClickListener {
            soundManager.playClickSound()
            showSortOptions()
        }

        binding.availableLoansBTNExitButton.setOnClickListener{
            soundManager.playClickSound()
            val intent = Intent(this, BorrowActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        exitButton = findViewById(R.id.available_loans_BTN_exitButton)
        availableHeader = findViewById(R.id.available_loans_TVW_header)
        loanIdTextView = findViewById(R.id.available_loans_TVW_loanId)
        amountTextView = findViewById(R.id.available_loans_TVW_amount)
        repaymentPeriodTextView = findViewById(R.id.available_loans_TVW_repaymentPeriod)
        availabilityUntilTextView = findViewById(R.id.available_loans_TVW_availabilityUntil)
        interestRateTextView = findViewById(R.id.available_loans_TVW_interestRate)
        calculatedLoanAmountTextView = findViewById(R.id.available_loans_TVW_calculatedLoanAmount)
        calculatedMonthlyPaymentTextView = findViewById(R.id.available_loans_TVW_calculatedMonthlyPayment)
        textAvailableLoans = findViewById(R.id.available_loans_TVW_textAvailableLoans)
        sortRedirect = findViewById(R.id.available_loans_TVW_sortRedirect)
        loansProgressBar = findViewById(R.id.available_loans_progressBar)
    }


    private fun showSortOptions() {
        val items = arrayOf("Sort by Interest Rate", "Sort by Repayment Period")

        // Load custom font from resources
        val customFont = ResourcesCompat.getFont(this, R.font.js)

        // Create a custom ArrayAdapter with custom appearance
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.textSize = 20f // Set the text size
                textView.setTextColor(Color.BLACK) // Set the text color
                customFont?.let { textView.typeface = it } // Set the custom font

                return textView
            }
        }

        val builder = MaterialAlertDialogBuilder(this)

        // Create a TextView for the custom title
        val customTitleTextView = TextView(this)
        customTitleTextView.text = "Sort Options"
        customTitleTextView.textSize = 30f // Set the text size
        customTitleTextView.setTextColor(Color.BLACK) // Set the text color
        customTitleTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlue)) // Set the background color
        customTitleTextView.gravity = Gravity.CENTER
        customTitleTextView.setPadding(10, 20, 10, 10) // Set padding

        customFont?.let { customTitleTextView.typeface = it }
        // Set the custom title
        builder.setCustomTitle(customTitleTextView)

        // Set the custom adapter
        builder.setAdapter(adapter) { _, which ->
            when (which) {
                0 -> sortLoansByInterestRate()
                1 -> sortLoansByRepaymentPeriod()
            }
        }

        val dialog = builder.create()

        // Set background color for the dialog
        dialog.setOnShowListener {
            val listView = (dialog as? AlertDialog)?.listView
            listView?.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlue))
        }

        dialog.show()
    }


    private fun displayLoans() {
        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)

        // Keep the header row (first row) if it exists
        val headerRow = tableLayout.getChildAt(0) as? TableRow
        tableLayout.removeAllViews()

        // If the header row exists, add it back to the table
        if (headerRow != null) {
            tableLayout.addView(headerRow)
        }
        var index = 0
        for (loanData in loans) {
            val row = TableRow(this)
            row.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            row.setPadding(5, 5, 5, 5)

            row.setBackgroundResource(R.drawable.rectangle_input)

            row.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    if (!isLoanLocked(loanData.lId.orEmpty())) {
                        loansProgressBar.visibility = View.VISIBLE
                        lId = loanData.lId.toString()
                        lockLoan(lId)
                        val intent = Intent(this@AvailableLoansActivity, ChooseLoanActivity::class.java).apply {
                            putExtra("desiredAmount", desiredAmount)
                            putExtra("desiredPeriod", desiredPeriod)
                            putExtra("loan", loanData)
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            startActivity(intent)
                        }

                    } else {
                        // Handle the case where the loan is locked
                        Toast.makeText(
                            this@AvailableLoansActivity,
                            "This loan is currently locked by another user.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Display only specific properties

            val propertiesToDisplay = listOf(
                "${(index + 1)}",
                formatWithCommas(loanData.amount.toString()),
                "${loanData.period} months",
                "${loanData.expirationDate}",
                "${loanData.interestRate}%",
                formatWithCommas(calculateLoanAmount(loanData.amount,
                    loanData.interestRate).toString()),
                formatWithCommas(calculateMonthlyRepayment(
                    loanData.amount,
                    loanData.interestRate,
                    loanData.period
                ).toString())
            )

            for (propertyValue in propertiesToDisplay) {
                val textView = TextView(this)
                val params = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(100, 0, 0, 0)
                textView.layoutParams = params
                textView.text = propertyValue
                textView.setTextColor(Color.parseColor("#FFFFFF"))
                textView.setPadding(5, 5, 5, 5)
                textView.textSize = 20F
                val customFont = ResourcesCompat.getFont(this, R.font.js)
                textView.typeface = customFont

                row.addView(textView)

            }

            tableLayout.addView(row)
            index++
        }

    }

    private fun sortLoansByInterestRate() {
        binding.availableLoansTVWSortRedirect.text = if (sortByInterestRateAsc) "Sort ⤒" else "Sort ⤓"
        loans = if (sortByInterestRateAsc) {
            loans.sortedBy { it.interestRate.toFloat() }
        } else {
            loans.sortedByDescending { it.interestRate.toFloat() }
        }
        sortByInterestRateAsc = !sortByInterestRateAsc
        displayLoans()
    }

    private fun sortLoansByRepaymentPeriod() {
        binding.availableLoansTVWSortRedirect.text = if (sortByRepaymentPeriodAsc) "Sort ⤒" else "Sort ⤓"
        loans = if (sortByRepaymentPeriodAsc) {
            loans.sortedBy { it.period.toFloat() }
        } else {
            loans.sortedByDescending { it.period.toFloat() }
        }
        sortByRepaymentPeriodAsc = !sortByRepaymentPeriodAsc
        displayLoans()
    }

    fun calculateMonthlyRepayment(amount: Double, rate: Double, period: Int): Double {
        // Convert annual interest rate to monthly rate
        val monthlyRate = rate / 12.0 / 100.0

        // Calculate monthly repayment using the formula
        val numerator = monthlyRate * Math.pow(1 + monthlyRate, period.toDouble())
        val denominator = Math.pow(1 + monthlyRate, period.toDouble()) - 1
        val monthlyRepayment = amount * (numerator / denominator)

        // Round to 2 digits after the decimal point
        return monthlyRepayment.roundTo2DecimalPlaces()
    }


    fun calculateLoanAmount(amount: Double, interestRate: Double): Double {
        val loanAmount = amount * (1 + interestRate / 100.0)
        return loanAmount.roundTo2DecimalPlaces()
    }

    fun Double.roundTo2DecimalPlaces(): Double {
        val df = DecimalFormat("#.##")
        return df.format(this).toDouble()
    }

    fun formatWithCommas(input: String): String {
        val number = try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            return input // Return input as is if it's not a valid number
        }

        return String.format("%,.2f", number)
    }


    private fun retrieveLoans() {
        isDataLoading = true
        showProgressBar()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = retrofitInterface.getLoans(desiredAmount, userToken)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@AvailableLoansActivity,
                                "No available loans found",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Filter loans based on amount, period, and lock status
                            loans = responseBody.filter {
                                it.amount <= desiredAmount.toDouble() &&
                                        it.period >= desiredPeriod.toInt() &&
                                        !isLoanLocked(it.lId.orEmpty()) && // Check if the loan is not locked
                                        (userData.id != it.lenderId)
                            }
                            displayLoans()
                        }
                    } else {
                        Toast.makeText(
                            this@AvailableLoansActivity,
                            "Failed to retrieve loans: ${response.errorBody()?.string()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    hideProgressBar()
                    isDataLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AvailableLoansActivity,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    hideProgressBar()
                    isDataLoading = false
                }
            }
        }
    }

    private fun lockLoan(loanId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = LockLoanRequest(userToken = userToken, loanId = loanId)
                val response = retrofitInterface.lockLoan(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Refresh the list of loans after locking
                        retrieveLoans()
                    } else {
                        Toast.makeText(
                            this@AvailableLoansActivity,
                            "Loan is currently on watch by\nanother user",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AvailableLoansActivity,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun isLoanLocked(loanId: String): Boolean {
        return try {
            val response = retrofitInterface.getLoanLockStatus(loanId)
            response.isSuccessful && response.body()?.locked == true
        } catch (e: Exception) {
            false
        }
    }

    private fun showProgressBar() {
        loansProgressBar.visibility = View.VISIBLE
        textAvailableLoans.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        loansProgressBar.visibility = View.INVISIBLE
        textAvailableLoans.visibility = View.INVISIBLE
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textViews = listOf(
            loanIdTextView, amountTextView, repaymentPeriodTextView,
            availabilityUntilTextView, interestRateTextView,
            calculatedLoanAmountTextView, calculatedMonthlyPaymentTextView,
            textAvailableLoans, sortRedirect, availableHeader // Add other text elements as needed
        )
        textViews.forEach { textView ->
            textView.textSize = textView.textSize * textScalar
        }
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}

