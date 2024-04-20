package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityAccessibilityMenuBinding
import com.example.myapplication.requestResponse.TextScalarUpdateRequest
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
    private lateinit var smallText: AppCompatTextView
    private lateinit var mediumText: AppCompatTextView
    private lateinit var largeText: AppCompatTextView
    private lateinit var userData: UserDataResponse
    private lateinit var progressBarAccessibility: ProgressBar
    private lateinit var userToken: String
    private var scaleFactor by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

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
    }

    private fun findViews() {
        exitButton = findViewById(R.id.accessibility_menu_BTN_exitButton)
        confirmButton = findViewById(R.id.accessibility_menu_BTN_confirm)
        smallText = findViewById(R.id.accessibility_menu_TVW_smallText)
        mediumText = findViewById(R.id.accessibility_menu_TVW_mediumText)
        largeText = findViewById(R.id.accessibility_menu_TVW_largeText)
        progressBarAccessibility = findViewById(R.id.accessibility_menu_progressBar)
    }


    private fun updateScalerLabel(progress: Int) {
        // Update the scaler label based on the SeekBar progress
        when (progress) {
            0 -> {
                binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorYellow))
                binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            }
            1 -> {
                binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this,
                    R.color.TextColorYellow
                ))
                binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            }
            2 -> {
                binding.accessibilityMenuTVWSmallText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                binding.accessibilityMenuTVWMediumText.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                binding.accessibilityMenuTVWLargeText.setTextColor(ContextCompat.getColor(this, R.color.TextColorYellow))
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

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

}