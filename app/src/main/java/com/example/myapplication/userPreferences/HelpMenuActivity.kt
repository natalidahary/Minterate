package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityHelpMenuBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Retrofit
import kotlin.properties.Delegates

class HelpMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var helpHeader: AppCompatTextView
    private lateinit var exitButton: MaterialButton
    private lateinit var submitQuestionButton: MaterialButton
    private lateinit var questionInput: AppCompatEditText
    private lateinit var faqLinearLayout: LinearLayout
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()

        setupFAQs()

        applyTextScalar()

        submitQuestionButton.setOnClickListener {
            soundManager.playClickSound()
            val userQuestion = binding.helpMenuEDTQuestionInput.text.toString().trim()
            if (userQuestion.isNotEmpty()) {
                addQuestionToFAQ(userQuestion)
                binding.helpMenuEDTQuestionInput.text?.clear()
            } else {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            }
        }

        binding.helpMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun findViews() {
        helpHeader = findViewById(R.id.help_menu_header)
        exitButton = findViewById(R.id.help_menu_BTN_exitButton)
        faqLinearLayout = findViewById(R.id.faqLinearLayout)
        submitQuestionButton = findViewById(R.id.help_menu_BTN_submitQuestionButton)
        questionInput = findViewById(R.id.help_menu_EDT_questionInput)
    }

    private fun setupFAQs() {
        val questionsAnswers = listOf(
            "How can I check if a loan I’ve extended has been accepted by someone else?" to "Navigate to the main page, access the personal menu, and go to the “Loans” section. Loans are grouped into categories: PENDING, ACTIVE, COMPLETED, and EXPIRED. Once a user accepts the loan you offered, it will appear under the “ACTIVE” category, accompanied by a monthly transaction record from the acceptance date.",
            "What does the total balance represent?" to "The total balance indicates the amount of money the user will either receive or spend. A negative balance implies the user owes money, while a positive balance signifies credit. Ultimately, the total balance will reach zero, indicating that the user has cleared all debts or credits.",
            "How can I view the details of a specific loan transaction?" to "You can view all transactions on the main page. Additionally, on the loans page, clicking on a specific loan allows you to export its details to CSV, providing comprehensive information, including all transactions.",
            "What happens if a loan I offered expires without being accepted?" to "Expired loans will be displayed on the loans page under the “EXPIRED” category for one month. After this period, they will be automatically removed from the system.",
            "What should I do if I forget my password?" to "You can reset your password anytime on the login page by clicking “Forgot Password.” You will need to provide your user email and the registered mobile number for verification.",
            "How can I update my mobile number?" to "On the main page, access the personal menu and navigate to the “Security” section. There, you will find an option to change your password and update your mobile number.",
            "Is it possible to register without a credit card?" to "Our application platform necessitates a credit card for registration, and it’s important to note that your credit card information is securely stored to ensure the safety of your account. Without a credit card, the registration process cannot be completed.",
            "How do I delete my account?" to "To delete your account, ensure there are no active loans or outstanding financial obligations. On the main page, go to the settings menu and navigate to “Delete My Account.”",
            "Can I repay the remaining amount of loan I borrowed?" to "Yes, you can repay the entire remaining amount of the loan you borrowed at any time. On the main page, go to the personal menu, navigate to “Loans,” and under Active loans, click on a loan for further information and options, including the ability to initiate full loan repayment.",
            "How are formalities and loan contracts handled?" to "Each loan on our platform includes a detailed contract outlining terms and conditions for both the lender and borrower. This comprehensive agreement specifies repayment amount, interest rates, and any other relevant terms. This contractual documentation ensures clarity and protection for both parties involved."
            // Add more question-answer pairs
        )

        questionsAnswers.forEach { (question, answer) ->
            val questionView = createTextView(question, 50f, true)
            val answerView = createTextView(answer, 40f, false)

            answerView.visibility = View.GONE
            questionView.setOnClickListener {
                answerView.visibility = if (answerView.visibility == View.GONE) View.VISIBLE else View.GONE
            }

            binding.faqLinearLayout.addView(questionView)
            binding.faqLinearLayout.addView(answerView)

            // Add a spacer View after each question-answer pair
            val spacerView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.faq_spacer_height) // Define this dimension in your dimens.xml
                )
            }
            binding.faqLinearLayout.addView(spacerView)
        }
    }

    private fun createTextView(text: String, size: Float, isBold: Boolean): TextView {
        val titleTypeface = ResourcesCompat.getFont(this, R.font.js)
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size * textScalar)
            setTextColor(Color.WHITE)
            typeface = if (isBold) Typeface.create(titleTypeface, Typeface.BOLD) else titleTypeface
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
           helpHeader, questionInput
            // Add other static TextViews and EditTexts here
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    private fun addQuestionToFAQ(question: String) {
        val questionView = createTextView(question, 50f, true)
        val answerView = createTextView("Thank you for your question. We will respond soon.", 40f, false)

        binding.faqLinearLayout.addView(questionView)
        binding.faqLinearLayout.addView(answerView)
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}