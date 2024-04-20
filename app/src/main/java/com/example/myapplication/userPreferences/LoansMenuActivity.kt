package com.example.myapplication.userPreferences

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityLoansMenuBinding
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.LoanStatus
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class LoansMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoansMenuBinding

    private var retrofit: Retrofit
    private var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var availableLoansHeader: AppCompatTextView
    private lateinit var exitButton: MaterialButton
    private lateinit var loansProgressBar: ProgressBar
    private lateinit var textLookingLoans: AppCompatTextView
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse
    private var userLoans: List<LoanDataResponse> = emptyList()
    private var isDataLoading: Boolean = false

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoansMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        loansProgressBar.visibility = View.VISIBLE
        textLookingLoans.visibility = View.VISIBLE

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        //userData = appData.userData!!
        userData = appData.userData ?: return

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        retrieveLoans()

        binding.loansMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        exitButton = findViewById(R.id.loans_menu_BTN_exitButton)
        availableLoansHeader = findViewById(R.id.loans_menu_TVW_availableLoansHeader)
        loansProgressBar = findViewById(R.id.loans_menu_progressBar)
        textLookingLoans = findViewById(R.id.loans_menu_TVW_textLookingLoans)
    }

    private fun displayLoans() {
        val tableLayout = binding.tableLayout
        tableLayout.removeAllViews()

        // Group loans by status
        val pendingLoans = userLoans.filter { it.status == LoanStatus.PENDING }
        val activeLoans = userLoans.filter { it.status == LoanStatus.ACTIVE }
        val completedLoans = userLoans.filter { it.status == LoanStatus.COMPLETED }
        val expiredLoans = userLoans.filter { it.status == LoanStatus.EXPIRED }

        // Display each group
        displayLoanGroup("ACTIVE", activeLoans, tableLayout)
        displayLoanGroup("PENDING", pendingLoans, tableLayout)
        displayLoanGroup("COMPLETED", completedLoans, tableLayout)
        displayLoanGroup("EXPIRED", expiredLoans, tableLayout)
    }

    private fun displayLoanGroup(title: String, loanList: List<LoanDataResponse>, tableLayout: TableLayout) {
        if (loanList.isNotEmpty()) {
            val titleTypeface = ResourcesCompat.getFont(this, R.font.js)

            val titleTextColor = when (title) {
                "ACTIVE" -> Color.parseColor("#7DCE13") // Green color for active loans
                "PENDING" -> Color.parseColor("#E36414") // Orange color for pending loans
                "COMPLETED" -> Color.parseColor("#B80000") // Red color for completed loans
                "EXPIRED" -> Color.parseColor("#919190") // Grey color for completed loans
                else -> Color.BLACK
            }

            // Check if the title is "EXPIRED" and add additional TextView
            if (title == "EXPIRED") {
                val expiredMessageTextView = TextView(this).apply {
                    text = "Expired loans will be automatically deleted within a month of expiration"
                    textSize = 18f // Adjust text size as needed
                    typeface = Typeface.create(titleTypeface, Typeface.NORMAL)
                    gravity = Gravity.LEFT
                    setTextColor(Color.WHITE)
                    setPadding(5, 5, 5, 5) // Adjust padding as needed
                }
                tableLayout.addView(expiredMessageTextView)
            }

            // Category title
            val titleTextView = TextView(this).apply {
                text = title
                textSize = 24f
                typeface = Typeface.create(titleTypeface, Typeface.BOLD)
                gravity = Gravity.LEFT
                setTextColor(titleTextColor)
                setBackgroundResource(R.drawable.rectangle_input4)
            }
            tableLayout.addView(titleTextView)

            // Column titles
            val headerRow = TableRow(this)
            val headers = arrayOf("Loan ID", "Amount", "Repayment Period", "Availability Until", "Interest Rate", "Calculated Loan Amount", "Calculated Monthly Payment")
            headers.forEach { headerTitle ->
                val headerTextView = TextView(this).apply {
                    text = headerTitle
                    textSize = 20f
                    typeface = titleTypeface
                    setTextColor(Color.parseColor("#000000"))
                    setPadding(5, 5, 80, 5)
                }
                headerRow.addView(headerTextView)
            }
            tableLayout.addView(headerRow)

            // Loan rows
            loanList.forEachIndexed { index, loanData ->
                val row = createLoanRow(index, loanData)
                tableLayout.addView(row)
            }

            // Space after each category
            val spaceView = View(this).apply {
                layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 80) // 80px space
            }
            tableLayout.addView(spaceView)
        }
    }



    private fun createLoanRow(index: Int, loanData: LoanDataResponse): TableRow {
        val row = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setPadding(5, 5, 5, 5)
            setBackgroundResource(R.drawable.rectangle_input3)
            setOnClickListener {
                // Intent to start the new activity
                val intent = Intent(this@LoansMenuActivity, ShowLoanDetailsActivity::class.java)
                intent.putExtra("loan", loanData)
                // Start the new activity
                startActivity(intent)
            }
        }

        // Creating TextView for each property and adding them to the row
        val indexTextView = createTextView("${index + 1}")
        val amountTextView = createTextView(formatWithCommas(loanData.amount.toString()))
        val periodTextView = createTextView("${loanData.period} months")
        val expirationDateTextView = createTextView(loanData.expirationDate.toString())
        val interestRateTextView = createTextView("${loanData.interestRate}%")
        val calculatedLoanAmountTextView = createTextView(formatWithCommas(calculateLoanAmount(loanData.amount, loanData.interestRate).toString()))
        val calculatedMonthlyPaymentTextView = createTextView(formatWithCommas(calculateMonthlyRepayment(loanData.amount, loanData.interestRate, loanData.period).toString()))

        // Add TextViews to the TableRow
        row.addView(indexTextView)
        row.addView(amountTextView)
        row.addView(periodTextView)
        row.addView(expirationDateTextView)
        row.addView(interestRateTextView)
        row.addView(calculatedLoanAmountTextView)
        row.addView(calculatedMonthlyPaymentTextView)

        if (loanData.status == LoanStatus.PENDING) {
            // Create a container for the delete icon
            val iconContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 0)
                }
            }

            // Create an ImageButton for the delete icon
            val deleteIcon = ImageButton(this).apply {
                setImageResource(R.drawable.ic_del) // Set your delete icon here
                background = null // Remove background to make it just the icon
                setOnClickListener {
                    showDeleteConfirmationDialog(loanData.lId)
                }
                layoutParams = LinearLayout.LayoutParams(
                    80, // Width of the icon
                    80  // Height of the icon
                )
            }

            // Add the icon to its container
            iconContainer.addView(deleteIcon)

            // Add the container to the row
            row.addView(iconContainer)
        }

        return row
    }


    private fun createTextView(textContent: String): TextView {
        return TextView(this).apply {
            text = textContent
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            ).apply {
                // Center-align the TextView within its cell
                gravity = Gravity.CENTER
            }
            gravity = Gravity.CENTER // Center-align the text within the TextView
            setTextColor(Color.parseColor("#000000"))
            textSize = 20f
            setPadding(5, 5, 5, 5)
            typeface = ResourcesCompat.getFont(context, R.font.js)
        }
    }


    private fun showDeleteConfirmationDialog(loanId: String?) {
        loanId?.let { id ->
            // Create a Dialog instance
            val dialog = Dialog(this)

            // Set up the Dialog layout programmatically
            dialog.setContentView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 50, 50, 50)
                setBackgroundColor(Color.parseColor("#92a2cc")) // Background color

                // Header
                addView(TextView(this@LoansMenuActivity).apply {
                    text = "Delete Loan"
                    typeface = ResourcesCompat.getFont(context, R.font.js)
                    setTextColor(Color.BLACK)
                    textSize = 24f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, 20)
                    }
                })

                // Message
                addView(TextView(this@LoansMenuActivity).apply {
                    text = "Are you sure you want to delete this loan?"
                    typeface = ResourcesCompat.getFont(context, R.font.js)
                    setTextColor(Color.BLACK)
                    textSize = 18f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, 20)
                    }
                })

                // "Yes" Button
                addView(Button(this@LoansMenuActivity).apply {
                    text = "Yes"
                    textSize = 14f
                    typeface = ResourcesCompat.getFont(context, R.font.js)
                    setBackgroundColor(Color.parseColor("#2b3452"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        deleteLoan(id)
                        dialog.dismiss()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110).apply {
                        setMargins(0, 0, 0, 10)
                    }
                })

                // "No" Button
                addView(Button(this@LoansMenuActivity).apply {
                    text = "No"
                    textSize = 14f
                    typeface = ResourcesCompat.getFont(context, R.font.js)
                    setBackgroundColor(Color.parseColor("#2b3452"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        dialog.dismiss()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110) // Adjust button height here
                })
            })

            // Display the custom Dialog
            dialog.show()
        }
    }


    private fun deleteLoan(loanId: String) {
        // API call to delete the loan
        retrofitInterface.deleteLoan(userToken, loanId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@LoansMenuActivity, "Loan deleted successfully", Toast.LENGTH_SHORT).show()
                    retrieveLoans() // Refresh the loan list
                } else {
                    Toast.makeText(this@LoansMenuActivity, "Failed to delete loan: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                // Print the error message to the log
                Log.e("LoansMenuActivity", "Network error", t)
                Toast.makeText(this@LoansMenuActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
            retrofitInterface.getUserLoans(userToken).enqueue(object : Callback<List<LoanDataResponse>> {
                override fun onResponse(call: Call<List<LoanDataResponse>>, response: Response<List<LoanDataResponse>>) {
                    isDataLoading = false
                    if (response.isSuccessful) {
                        userLoans = response.body() ?: emptyList()
                        val appData = AppData.getInstance()
                        appData.userLoans = userLoans
                        runOnUiThread {
                            displayLoans()
                            hideProgressBar()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LoansMenuActivity, "Failed to retrieve loans", Toast.LENGTH_SHORT).show()
                            hideProgressBar()
                        }
                    }
                }

                override fun onFailure(call: Call<List<LoanDataResponse>>, t: Throwable) {
                    isDataLoading = false
                    runOnUiThread {
                        Toast.makeText(this@LoansMenuActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        hideProgressBar()
                    }
                }
            })
        }
    }



    private fun showProgressBar() {
        loansProgressBar.visibility = View.VISIBLE
        textLookingLoans.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        loansProgressBar.visibility = View.INVISIBLE
        textLookingLoans.visibility = View.INVISIBLE
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            availableLoansHeader, textLookingLoans
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