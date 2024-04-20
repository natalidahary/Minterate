package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityChangeAddressMenuBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class ChangeAddressMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeAddressMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var address: AppCompatEditText
    private lateinit var city: AppCompatEditText
    private lateinit var state: AppCompatEditText
    private lateinit var changeAddressMenuBTNExitButton: MaterialButton
    private lateinit var changeAddressMenuBTNSave: MaterialButton
    private lateinit var progressBarMyAddress: ProgressBar
    private lateinit var changeAddressHeader: AppCompatTextView
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

        binding.changeAddressMenuBTNSave.setOnClickListener {
            soundManager.playClickSound()
            val address = binding.changeAddressMenuEDTAddress.text.toString()
            val city = binding.changeAddressMenuEDTCity.text.toString()
            val state= binding.changeAddressMenuEDTState.text.toString()

            if (address.isNotEmpty() && city.isNotEmpty() && state.isNotEmpty()) {
                updateAddress(userToken, address, city, state)
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

    private fun findViews() {
        address = findViewById(R.id.change_address_menu_EDT_address)
        city = findViewById(R.id.change_address_menu_EDT_city)
        state = findViewById(R.id.change_address_menu_EDT_state)
        changeAddressMenuBTNExitButton = findViewById(R.id.change_address_menu_BTN_exitButton)
        changeAddressMenuBTNSave = findViewById(R.id.change_address_menu_BTN_save)
        progressBarMyAddress = findViewById(R.id.change_address_menu_progressBar)
        changeAddressHeader = findViewById(R.id.change_address_menu_TVW_Header)
    }

    private fun updateAddress(userToken: String, address: String, city: String, state: String) {
        val updateRequest = com.example.myapplication.requestResponse.AddressUpdateRequest(
            address,
            city,
            state
        )
        val call: Call<Void> = retrofitInterface.updateUserAddress(userToken, updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val appData = AppData.getInstance()
                    appData.userData!!.address = address
                    appData.userData!!.city = city
                    appData.userData!!.state = state
                    // Handle successful response (e.g., show a success message)
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
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangeAddressMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            address, city, state,
            changeAddressHeader
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