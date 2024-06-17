package com.example.myapplication.userInformation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityChangeAddressMenuBinding
import com.example.myapplication.databinding.ActivityTermsPrivacyBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Retrofit
import kotlin.properties.Delegates

class TermsPrivacyActivity: AppCompatActivity() {

    private lateinit var binding: ActivityTermsPrivacyBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var exitButton: MaterialButton
    private lateinit var conditionsBody: AppCompatTextView
    private lateinit var minterateLogo: AppCompatImageView

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager
    private lateinit var userData: UserDataResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val appData = AppData.getInstance()
        userData = appData.userData!!

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()

        findViews()
        applyTextScalar()
        applyBlackAndWhiteMode()

        binding.termPrivacyBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun findViews() {
        exitButton = findViewById(R.id.term_privacy_BTN_exitButton)
        conditionsBody = findViewById(R.id.terms_conditions_body)
        minterateLogo = findViewById(R.id.logo)
    }


    private fun applyTextScalar() {
        val textElements = listOf(
            conditionsBody
            // Include other TextViews and EditTexts as needed
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }


    private fun applyBlackAndWhiteMode() {
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isBWMode = preferences.getBoolean("isBlackAndWhiteMode", false)

        if (isBWMode) {
            val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorWhiteGray))

            conditionsBody.setTextColor(Color.BLACK)
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            minterateLogo.setImageResource(R.drawable.minterate_b_and_w)
            minterateLogo.layoutParams.width = 160.dpToPx(this)
            minterateLogo.layoutParams.height = 80.dpToPx(this)
            minterateLogo.scaleType = ImageView.ScaleType.FIT_CENTER

        } else {
            // Restore default colors
            val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
            rootLayout.setBackgroundResource(R.drawable.general_background)

            conditionsBody.setTextColor(Color.WHITE)
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorLightBlue))
            minterateLogo.setImageResource(R.drawable.icon_minterate)
        }
    }

    // Extension function to convert dp to px
    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

}