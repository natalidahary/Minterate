package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityChangeAddressMenuBinding
import com.example.myapplication.requestResponse.AddressUpdateRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class ChangeAddressMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeAddressMenuBinding

    // Retrofit variables
    private var retrofit: Retrofit = RetrofitManager.getRetrofit()
    private var retrofitInterface: RetrofitInterface = RetrofitManager.getRetrofitInterface()

    // UI elements
    private lateinit var address: AppCompatEditText
    private lateinit var city: AppCompatEditText
    private lateinit var state: AppCompatEditText
    private lateinit var changeAddressMenuBTNExitButton: MaterialButton
    private lateinit var changeAddressMenuBTNSave: MaterialButton
    private lateinit var progressBarMyAddress: ProgressBar
    private lateinit var changeAddressHeader: AppCompatTextView
    private lateinit var minterateLogo: AppCompatImageView

    // Other variables
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse
    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeAddressMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        progressBarMyAddress.visibility = View.INVISIBLE
        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()
        applyBlackAndWhiteMode()

        binding.changeAddressMenuBTNSave.setOnClickListener {
            soundManager.playClickSound()
            val addressStr = address.text.toString()
            val cityStr = city.text.toString()
            val stateStr = state.text.toString()

            if (addressStr.isNotEmpty() && cityStr.isNotEmpty() && stateStr.isNotEmpty()) {
                updateAddress(userToken, addressStr, cityStr, stateStr)
                recreate()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeAddressMenuActivity,
                    "All fields are mandatory",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.changeAddressMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Finds all required views and initializes them.
     */
    private fun findViews() {
        address = findViewById(R.id.change_address_menu_EDT_address)
        city = findViewById(R.id.change_address_menu_EDT_city)
        state = findViewById(R.id.change_address_menu_EDT_state)
        changeAddressMenuBTNExitButton = findViewById(R.id.change_address_menu_BTN_exitButton)
        changeAddressMenuBTNSave = findViewById(R.id.change_address_menu_BTN_save)
        progressBarMyAddress = findViewById(R.id.change_address_menu_progressBar)
        changeAddressHeader = findViewById(R.id.change_address_menu_TVW_Header)
        minterateLogo = findViewById(R.id.logo)
    }

    /**
     * Sends a network request to update the user's address.
     *
     * @param userToken The token of the logged-in user.
     * @param address The new address to be updated.
     * @param city The new city to be updated.
     * @param state The new state to be updated.
     */
    private fun updateAddress(userToken: String, address: String, city: String, state: String) {
        val updateRequest = AddressUpdateRequest(address, city, state)
        val call: Call<Void> = retrofitInterface.updateUserAddress(userToken, updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val appData = AppData.getInstance()
                    appData.userData!!.address = address
                    appData.userData!!.city = city
                    appData.userData!!.state = state
                    // Handle successful response
                    Toast.makeText(
                        this@ChangeAddressMenuActivity,
                        "Address updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangeAddressMenuActivity,
                        "Failed to update address",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeAddressMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Retrieves the text scalar value from SharedPreferences.
     *
     * @return The retrieved text scalar value.
     */
    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    /**
     * Applies the retrieved text scalar to relevant UI elements.
     */
    private fun applyTextScalar() {
        val textElements = listOf(address, city, state, changeAddressHeader)
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    /**
     * Applies black and white mode to the UI based on user preferences.
     * If black and white mode is enabled, it adjusts UI elements to grayscale colors.
     * Otherwise, it restores default colors.
     */
    private fun applyBlackAndWhiteMode() {
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isBWMode = preferences.getBoolean("isBlackAndWhiteMode", false)
        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)

        if (isBWMode) {
            // Apply black and white mode changes
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorWhiteGray))

            // Text and input fields
            listOf(address, city, state).forEach {
                it.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                it.setHintTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                it.setBackgroundResource(R.drawable.rectangle_input_black)
            }

            // Buttons and progress bar
            changeAddressMenuBTNSave.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            changeAddressMenuBTNExitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            changeAddressHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeAddressMenuBTNSave.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeAddressMenuBTNExitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            progressBarMyAddress.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorBlack))

            // Logo adjustments
            minterateLogo.setImageResource(R.drawable.minterate_b_and_w)
            minterateLogo.layoutParams.width = 160.dpToPx(this)
            minterateLogo.layoutParams.height = 80.dpToPx(this)
            minterateLogo.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            // Restore default colors
            rootLayout.setBackgroundResource(R.drawable.general_background)

            // Text and input fields
            listOf(address, city, state).forEach {
                it.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
                it.setBackgroundResource(R.drawable.rectangle_input)
                it.setHintTextColor(Color.BLACK)
            }

            // Buttons and progress bar
            changeAddressMenuBTNSave.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeAddressMenuBTNExitButton.setTextColor(ContextCompat.getColor(this, R.color.TextColorBlack))
            changeAddressHeader.setTextColor(ContextCompat.getColor(this, R.color.TextColorWhite))
            changeAddressMenuBTNSave.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorYellow))
            changeAddressMenuBTNExitButton.setBackgroundColor(ContextCompat.getColor(this, R.color.TextColorLightBlue))
            progressBarMyAddress.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.TextColorWhite))

            // Logo adjustments
            minterateLogo.setImageResource(R.drawable.icon_minterate)
        }
    }


    /**
     * Extension function to convert dp to pixels.
     *
     * @param context The context to fetch resources and display metrics.
     * @return The calculated pixel value from dp.
     */
    fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}
