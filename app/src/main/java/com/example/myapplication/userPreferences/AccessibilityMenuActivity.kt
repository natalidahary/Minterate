package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityAccessibilityMenuBinding
import com.example.myapplication.requestResponse.TextScalarUpdateRequest
import com.example.myapplication.requestResponse.UpdateBlackAndWhiteModeRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates


class AccessibilityMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccessibilityMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var exitButton: MaterialButton
    private lateinit var confirmButton: MaterialButton
    private lateinit var textSizeHeader : AppCompatTextView
    private lateinit var menuHeader: AppCompatTextView
    private lateinit var smallText: AppCompatTextView
    private lateinit var mediumText: AppCompatTextView
    private lateinit var largeText: AppCompatTextView
    private lateinit var userData: UserDataResponse
    private lateinit var progressBarAccessibility: ProgressBar
    private lateinit var seekBar: SeekBar
    private lateinit var userToken: String
    private lateinit var minterateLogo: AppCompatImageView
    private var scaleFactor by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager
    private lateinit var toggleBWButton: MaterialButton
    // Variables for black and white mode
    private var isBlackAndWhiteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessibilityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appData = AppData.getInstance()

        findViews()

        userToken = appData.userToken.toString()
        userData = appData.userData!!

        progressBarAccessibility.visibility = View.INVISIBLE

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        // Retrieve black and white mode state from preferences
        isBlackAndWhiteMode = retrieveBlackAndWhiteModeFromPreferences()

        // Update screen colors and text colors based on the mode state
        updateScreenColors(isBlackAndWhiteMode)
        updateTextColors(isBlackAndWhiteMode)


        // Set up a listener for the SeekBar
        binding.accessibilityMenuScalarSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Handle progress change, you can update UI or perform actions here
                soundManager.playClickSound()
                updateScalerLabel(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Called when tracking starts
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Called when tracking stops, you can perform final actions here
            }
        })

        // Set up a click listener for the Save button
        binding.accessibilityMenuBTNConfirm.setOnClickListener {
            soundManager.playClickSound()
            // Perform actions when the Save button is clicked
            progressBarAccessibility.visibility = View.VISIBLE
            val progress = binding.accessibilityMenuScalarSeekBar.progress
            adjustTextSize(progress)
            Toast.makeText(this@AccessibilityMenuActivity, "Text size was updated successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@AccessibilityMenuActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.accessibilityMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.accessibilityMenuBTNToggleBW.setOnClickListener {
            // Toggle black and white mode
            isBlackAndWhiteMode = !isBlackAndWhiteMode
            // Update screen colors
            updateScreenColors(isBlackAndWhiteMode)
            // Update text colors
            updateTextColors(isBlackAndWhiteMode)
            // Update black and white mode setting on the server
            val request = UpdateBlackAndWhiteModeRequest(isBlackAndWhiteMode)
            updateBlackAndWhiteModeOnServer(userToken, request)
            // Save black and white mode state to preferences
            saveBlackAndWhiteModeToPreferences(isBlackAndWhiteMode)
            Toast.makeText(this@AccessibilityMenuActivity, "Text colors was updated successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@AccessibilityMenuActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        exitButton = findViewById(R.id.accessibility_menu_BTN_exitButton)
        confirmButton = findViewById(R.id.accessibility_menu_BTN_confirm)
        smallText = findViewById(R.id.accessibility_menu_TVW_smallText)
        mediumText = findViewById(R.id.accessibility_menu_TVW_mediumText)
        largeText = findViewById(R.id.accessibility_menu_TVW_largeText)
        menuHeader =  findViewById(R.id.accessibility_menu_TVW_header)
        textSizeHeader = findViewById(R.id.accessibility_menu_TVW_TextSizeHeader)
        progressBarAccessibility = findViewById(R.id.accessibility_menu_progressBar)
        toggleBWButton = findViewById(R.id.accessibility_menu_BTN_toggleBW)
        seekBar = findViewById(R.id.accessibility_menu_scalar_SeekBar)
        minterateLogo = findViewById(R.id.logo)
    }


    private fun updateScalerLabel(progress: Int) {
        // Update the scaler label based on the SeekBar progress
        when (progress) {
            0 -> {
                if(isBlackAndWhiteMode){
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.lightBlack))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                }else{
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorYellow))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                }
            }
            1 -> {
                if(isBlackAndWhiteMode){
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.lightBlack))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                }else{
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorYellow))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                }
            }
            2 -> {
                if(isBlackAndWhiteMode){
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.lightBlack))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
                }else{
                    binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                    binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                    binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorYellow))
                }
            }
        }
    }

    private fun adjustTextSize(progress: Int) {
        scaleFactor = when (progress) {
            0 -> 0.3f  // Small
            1 -> 0.45f  // Medium
            2 -> 0.55f  // Large
            else -> 0.3f // Default to 1.0
        }

        saveTextScalarToPreferences(scaleFactor)

        // Adjust text size for all TextViews in application
        val textViews = listOf(
            binding.accessibilityMenuTVWHeader,
            binding.accessibilityMenuTVWTextSizeHeader,
            binding.accessibilityMenuTVWScalarValueLabel,
            binding.accessibilityMenuTVWSmallText,
            binding.accessibilityMenuTVWMediumText,
            binding.accessibilityMenuTVWLargeText,
        )

        textViews.forEach { textView ->
            textView.textSize = textView.textSize * scaleFactor
            textView.requestLayout()
            textView.invalidate()
        }
        // Update textScalar on the server
        val updateRequest = TextScalarUpdateRequest(scaleFactor)
        updateTextScalar(userToken, updateRequest)
        recreate()
    }

    private fun saveTextScalarToPreferences(scaleFactor: Float) {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putFloat("textScalar", scaleFactor)
        editor.apply()
    }

    private fun updateTextScalar(userToken: String, updateRequest: TextScalarUpdateRequest) {
        val call: Call<Void> = retrofitInterface.updateUserTextScalar(userToken, updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val appData = AppData.getInstance()
                    appData.userData!!.textScalar = scaleFactor

                    // Handle successful response (e.g., show a success message)
                    Toast.makeText(
                        this@AccessibilityMenuActivity,
                        "TextScalar updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@AccessibilityMenuActivity,
                        "Failed to update TextScalar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@AccessibilityMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateBlackAndWhiteModeOnServer(userToken: String, updateRequest: UpdateBlackAndWhiteModeRequest) {
        val call: Call<Void> = retrofitInterface.updateBlackAndWhiteMode(userToken, updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Handle successful response (e.g., show a success message)
                    Toast.makeText(
                        this@AccessibilityMenuActivity,
                        "Black and white mode updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@AccessibilityMenuActivity,
                        "Failed to update black and white mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@AccessibilityMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun updateScreenColors(isBlackAndWhite: Boolean) {
        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        if (isBlackAndWhite) {
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorWhiteGray))
        } else {
            val drawable = ContextCompat.getDrawable(this, R.drawable.general_background)
            if (drawable != null) {
                rootLayout.background = drawable
            } else {
                Log.e("AccessibilityMenuActivity", "Drawable resource is null")
            }
        }
    }

    private fun updateTextColors(isBlackAndWhite: Boolean) {
        if (isBlackAndWhite) {
            smallText.setTextColor(Color.BLACK)
            mediumText.setTextColor(Color.BLACK)
            largeText.setTextColor(Color.BLACK)
            exitButton.setTextColor(Color.WHITE)
            exitButton.setBackgroundColor(Color.BLACK)
            confirmButton.setTextColor(Color.WHITE)
            confirmButton.setBackgroundColor(Color.BLACK)
            textSizeHeader.setTextColor(Color.BLACK)
            menuHeader.setTextColor(Color.BLACK)
            toggleBWButton.setTextColor(Color.WHITE)
            toggleBWButton.setBackgroundColor(Color.BLACK)
            seekBar.setBackgroundResource(R.drawable.rectangle_input_black)
            // Set progress tint and thumb tint
            val grayColor = ContextCompat.getColor(this, R.color.lightBlack)
            seekBar.progressTintList = ColorStateList.valueOf(grayColor)
            seekBar.thumbTintList = ColorStateList.valueOf(grayColor)
            progressBarAccessibility.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))
            minterateLogo.setImageResource(R.drawable.minterate_b_and_w)
            minterateLogo.layoutParams.width = 160.dpToPx(this)
            minterateLogo.layoutParams.height = 80.dpToPx(this)
            minterateLogo.scaleType = ImageView.ScaleType.FIT_CENTER

        } else {
            smallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            mediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            largeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            textSizeHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            menuHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            exitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            exitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorLightBlue))
            confirmButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            confirmButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorYellow))
            toggleBWButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            toggleBWButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorYellow))
            seekBar.setBackgroundResource(R.drawable.rectangle_input)
            progressBarAccessibility.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))
            minterateLogo.setImageResource(R.drawable.icon_minterate)
        }
    }

    // Function to retrieve black and white mode state from preferences
    private fun retrieveBlackAndWhiteModeFromPreferences(): Boolean {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getBoolean("isBlackAndWhiteMode", false) // Default value is false
    }

    // Function to save black and white mode state to preferences
    private fun saveBlackAndWhiteModeToPreferences(isBlackAndWhiteMode: Boolean) {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean("isBlackAndWhiteMode", isBlackAndWhiteMode)
        editor.apply()
    }


    // Extension function to convert dp to px
    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

}