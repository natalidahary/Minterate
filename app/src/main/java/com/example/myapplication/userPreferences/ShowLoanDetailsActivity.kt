package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.AppData
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.requestResponse.Transaction
import com.example.myapplication.databinding.ActivityShowLoanDetailsBinding
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.LoanRepaymentRequest
import com.example.myapplication.requestResponse.LoanStatus
import com.example.myapplication.requestResponse.RecordTransactionRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.UnitValue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates
import kotlin.random.Random


class ShowLoanDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowLoanDetailsBinding

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
    private lateinit var loanDetailsHeader: AppCompatTextView
    private lateinit var loanRepaymentButton: MaterialButton
    private lateinit var amountLoan: AppCompatTextView
    private lateinit var periodLoan: AppCompatTextView
    private lateinit var interestLoan: AppCompatTextView
    private lateinit var monthlyLoan: AppCompatTextView
    private lateinit var csvButton: MaterialButton
    private lateinit var excelText: AppCompatTextView
    private lateinit var repaymentText: AppCompatTextView
    private lateinit var repaymentText2: AppCompatTextView

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
    private var transactions: List<Transaction> = emptyList()
    private lateinit var webView: WebView

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowLoanDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        loanRepaymentButton.visibility = View.INVISIBLE
        repaymentText.visibility = View.INVISIBLE
        repaymentText2.visibility = View.INVISIBLE


        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        @Suppress("DEPRECATION")
        loanData = intent.getSerializableExtra("loan") as LoanDataResponse

        transactions = appData.transactions ?: emptyList()

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        //loanData
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


        if (loanData.status == LoanStatus.ACTIVE && userData.id == loanData.borrowerId) {
            loanRepaymentButton.visibility = View.VISIBLE
            repaymentText.visibility = View.VISIBLE
            repaymentText2.visibility = View.VISIBLE

            val currentLoan = AppData.getInstance().userLoans?.find { it.lId == lId }
            val originalAmount = currentLoan?.amount ?: 0.0
            val filteredTransactions = transactions.filter { it.loanId == lId }

            val totalPaid = filteredTransactions.sumOf { it.amount }
            val remainingAmount = if (filteredTransactions.isEmpty()) originalAmount else originalAmount - totalPaid
            val formattedRemainingAmount = formatWithCommas(remainingAmount.toString()) + " "
            val baseText = "amount of "
            repaymentText2.text = "$baseText$formattedRemainingAmount${loanData.currency}"
            repaymentText2.setTextColor(Color.WHITE) // Set the text color to white

            Log.d(
                "RepaymentButton",
                "Original Amount: $originalAmount, Total Paid: $totalPaid, Remaining: $remainingAmount"
            )

            binding.showLoanBTNRepaymentButton.setOnClickListener {
                soundManager.playClickSound()
                Log.d("RepaymentButton", "Loan is active, proceeding with repayment process")

                val processTransaction = { lenderFirstName: String, lenderLastName: String ->
                    val newPaymentCount = if (filteredTransactions.isEmpty()) "1/12" else {
                        val lastPaymentCount = filteredTransactions.lastOrNull()?.paymentCount ?: "0/12"
                        val (currentCount, totalCount) = lastPaymentCount.split("/").let {
                            val current = it[0].toIntOrNull() ?: 0
                            val total = it[1].toIntOrNull() ?: 12
                            Pair(current, total)
                        }
                        "${currentCount + 1}/$totalCount"
                    }

                    val repaymentTransaction = Transaction(
                        loanId = loanData.lId,
                        currency = loanData.currency,
                        amount = remainingAmount,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        origin = userData?.id,
                        destination = currentLoan?.lenderId,
                        originFirstName = userData?.firstName,
                        originLastName = userData?.lastName,
                        destinationFirstName = lenderFirstName,
                        destinationLastName = lenderLastName,
                        paymentCount = newPaymentCount
                    )

                    Log.d("RepaymentButton", "Creating repayment transaction: $repaymentTransaction")

                    recordTransaction(repaymentTransaction)
                    loanData.lId?.let { it1 -> completeLoanRepayment(it1) }

                    // Delay before returning to LoansMenuActivity
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, LoansMenuActivity::class.java)
                        startActivity(intent)
                        finish() // Close current activity
                    }, 3000) // Delay for 3 seconds

                    Log.d("RepaymentButton", "Completed loan repayment process")
                }

                if (filteredTransactions.isEmpty()) {
                    // Fetch lender's data for the first transaction
                    val lenderId = currentLoan?.lenderId ?: return@setOnClickListener
                    getUserDataById(lenderId) { lenderData ->
                        val lenderFirstName = lenderData?.firstName ?: "Unknown"
                        val lenderLastName = lenderData?.lastName ?: "Unknown"
                        processTransaction(lenderFirstName, lenderLastName)
                    }
                } else {
                    // Use lender's data from the last transaction
                    val lastTransaction = filteredTransactions.maxByOrNull { it.date ?: "" }
                    val lenderFirstName = lastTransaction?.destinationFirstName ?: "Unknown"
                    val lenderLastName = lastTransaction?.destinationLastName ?: "Unknown"
                    processTransaction(lenderFirstName, lenderLastName)
                }
            }

        } else {
            loanRepaymentButton.visibility = View.GONE
            repaymentText.visibility = View.GONE
            repaymentText2.visibility = View.GONE
            //Toast.makeText(this, "This loan is not active and cannot be repaid.", Toast.LENGTH_SHORT).show()
        }



        binding.showLoanBTNPdfButton.setOnClickListener {
            soundManager.playClickSound()
            createAndShareCsvFile()
        }

        binding.showLoanBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, LoansMenuActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun findViews() {
        exitButton = findViewById(R.id.show_loan_BTN_exitButton)
        amountHeader = findViewById(R.id.show_loan_TVW_amountHeader)
        periodHeader = findViewById(R.id.show_loan_TVW_periodHeader)
        interestHeader = findViewById(R.id.show_loan_TVW_interestHeader)
        monthlyHeader = findViewById(R.id.show_loan_TVW_monthlyHeader)
        loanDetailsHeader = findViewById(R.id.show_loan_TVW_loanDetailsHeader)
        loanRepaymentButton = findViewById(R.id.show_loan_BTN_repaymentButton)
        amountLoan = findViewById(R.id.show_loan_TVW_amountLoan)
        periodLoan = findViewById(R.id.show_loan_TVW_periodLoan)
        interestLoan = findViewById(R.id.show_loan_TVW_interestLoan)
        monthlyLoan = findViewById(R.id.show_loan_TVW_monthlyLoan)
        csvButton = findViewById(R.id.show_loan_BTN_pdfButton)
        excelText = findViewById(R.id.show_loan_TVW_pdfText)
        repaymentText = findViewById(R.id.show_loan_TVW_repaymentText)
        repaymentText2 = findViewById(R.id.show_loan_TVW_repaymentText2)
    }

    fun getUserDataById(userId: String, onResponse: (UserDataResponse?) -> Unit) {
        val userService = retrofit.create(RetrofitInterface::class.java)
        userService.getUserNameById(userId).enqueue(object : Callback<UserDataResponse> {
            override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                if (response.isSuccessful) {
                    onResponse(response.body())
                } else {
                    Log.e("UserDataError", "Error fetching user data by ID")
                    onResponse(null)
                }
            }

            override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                Log.e("UserDataError", "Network error: ${t.message}")
                onResponse(null)
            }
        })
    }




    private fun completeLoanRepayment(loanId: String) {
        val request = LoanRepaymentRequest(loanId, userToken)

        retrofitInterface.completeLoanRepayment(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    // Update local loan status
                    loanData.status = LoanStatus.COMPLETED
                    // Show confirmation message
                    Toast.makeText(this@ShowLoanDetailsActivity, "Loan repaid successfully!", Toast.LENGTH_SHORT).show()
                    // Update UI or navigate back to the loans menu
                    finish() // or call a method to update UI
                } else {
                    Toast.makeText(this@ShowLoanDetailsActivity, "Failed to complete repayment: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@ShowLoanDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun recordTransaction(transaction: Transaction) {
        val request = RecordTransactionRequest(userToken, transaction)

        retrofitInterface.recordTransaction(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ShowLoanDetailsActivity, "Transaction recorded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ShowLoanDetailsActivity, "Failed to record transaction: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@ShowLoanDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createAndShareCsvFile() {
        if (createPdfFile()) {
            sharePdfFile()
        }
    }

    private fun createPdfFile(): Boolean {

        val currentLoan = AppData.getInstance().userLoans?.find { it.lId == lId }
        val originalAmount = currentLoan?.amount
        val interestRate = currentLoan?.interestRate ?: ""
        var repaymentPeriod = currentLoan?.period ?: ""

        val allTransactions = AppData.getInstance().transactions ?: emptyList()
        val filteredTransactions = allTransactions.filter { it.loanId == lId }
        val totalPaid = filteredTransactions.sumOf { it.amount }
        var remainingAmount = originalAmount?.minus(totalPaid).toString()

        var paidPercent = ((totalPaid / originalAmount!!) *100).toString()
        if (remainingAmount < 0.toString()) {
            remainingAmount = 0.toString()
            paidPercent = 100.toString()
        }

        val pdfFilePath = File(getExternalFilesDir(null), "LoanDetails.pdf")
        val pdfWriter = PdfWriter(pdfFilePath)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // Loan Details Title
        val loanDetailsTitle = Paragraph("Loan Details").setBold().setFontSize(14f)
        document.add(loanDetailsTitle)
        // Loan Details Table
        val loanDetailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f))).useAllAvailableWidth()

        // Add rows with label and value
        val loanDetailData = arrayOf(
            Pair("Loan ID", "$lId"),
            Pair("Amount", formatWithCommas(originalAmount.toString())),
            Pair("Interest Rate", "$interestRate%"),
            Pair("Currency", extractCurrencyValue(currentLoan?.currency.toString())),
            Pair("Period", "$repaymentPeriod months"),
            Pair("Remaining Amount", formatWithCommas(remainingAmount)),
            Pair("Paid Percent",  "${formatWithCommas(paidPercent)}%")
        )
        loanDetailData.forEach { (label, value) ->
            loanDetailsTable.addCell(Cell().add(Paragraph(label).setBold().setFontSize(12f)))
            loanDetailsTable.addCell(Cell().add(Paragraph(value)))
        }
        // Adding the table to the document
        document.add(loanDetailsTable)


        // Transaction Details Title
        val transactionDetailsTitle = Paragraph("\nTransaction Details").setBold().setFontSize(14f)
        document.add(transactionDetailsTitle)

        // Transaction Details Title
        val transactionDetailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f, 1f))).useAllAvailableWidth()
        // Adding header cells with bold labels
        val headerLabelsTransaction = arrayOf("Payment Count", "Date", "Amount", "Lender Name", "Borrower Name")
        headerLabelsTransaction.forEach { label ->
            transactionDetailsTable.addHeaderCell(Cell().add(Paragraph(label).setBold().setFontSize(12f)))
        }

        // Adding transaction data
        for (transaction in filteredTransactions) {
            transactionDetailsTable.addCell(Cell().add(Paragraph(transaction.paymentCount ?: "")))
            transactionDetailsTable.addCell(Cell().add(Paragraph(transaction.date ?: "")))
            transactionDetailsTable.addCell(Cell().add(Paragraph(transaction.amount.toString())))
            transactionDetailsTable.addCell(Cell().add(Paragraph("${transaction.destinationFirstName} ${transaction.destinationLastName}")))
            transactionDetailsTable.addCell(Cell().add(Paragraph("${transaction.originFirstName} ${transaction.originLastName}")))
        }
        document.add(transactionDetailsTable)


        // Transaction Details Title
        val transactionPieTitle = Paragraph("\nTransaction Pie Chart\nֿ\n").setBold().setFontSize(14f)
        document.add(transactionPieTitle)

        val totalPeriods = repaymentPeriod?.toString()?.toIntOrNull() ?: 12
        val isLoanFullyPaid = remainingAmount.toDouble() <= 0
        // Assuming you have a list of transactions and a total number of periods
        val pieChartBitmap = createPieChartBitmap(filteredTransactions, 150, 150, totalPeriods, isLoanFullyPaid)

        // Convert the Bitmap to a byte array
        val stream = ByteArrayOutputStream()
        pieChartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapData = stream.toByteArray()

        // Add the image to your PDF
        val imageData = ImageDataFactory.create(bitmapData)
        val pdfImage = Image(imageData)
        document.add(pdfImage)

        document.close()

        return try {
            Toast.makeText(this, "PDF file created successfully", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating PDF file: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun createPieChartBitmap(transactions: List<Transaction>, width: Int, height: Int, totalPeriods: Int, isLoanFullyPaid: Boolean): Bitmap {

        val fontPath = ResourcesCompat.getFont(this, R.font.js)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.BLACK // Text color
            textSize = 12f // Text size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(fontPath, Typeface.BOLD)
        }

        // Define the rectangle for the pie chart
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Calculate total paid amount
        val totalPaid = transactions.sumOf { it.amount }
        var startAngle = 0f

        // Draw each segment for paid amounts
        transactions.forEach { transaction ->
            val sweepAngle = ((transaction.amount / totalPaid) * 360).toFloat()
            paint.color = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)) // Random color for each segment
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)

            // Calculate the angle to position the text
            val textAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble()).toFloat()
            val radius = width / 4f // Adjust the radius as needed
            val textX = width / 2f + radius * cos(textAngle)
            val textY = height / 2f + radius * sin(textAngle)
            canvas.drawText(transaction.paymentCount ?: "", textX, textY, textPaint)

            startAngle += sweepAngle
        }

        // Draw a segment for the unpaid portion if the loan is not fully paid off
        if (!isLoanFullyPaid) {
            val remainingPeriods = totalPeriods - transactions.size
            val sweepAngle = (remainingPeriods.toFloat() / totalPeriods.toFloat()) * 360
            paint.color = Color.LTGRAY // Color for unpaid segment
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
        }

        return bitmap
    }


    private fun sharePdfFile() {
        val fileUri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            File(getExternalFilesDir(null), "LoanDetails.pdf")
        )

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/pdf"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        startActivity(Intent.createChooser(shareIntent, "Share PDF file"))
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

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            amountLoan, periodLoan,
            interestLoan, monthlyLoan, excelText, loanDetailsHeader,
            repaymentText, repaymentText2, amountHeader, periodHeader, interestHeader, monthlyHeader
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