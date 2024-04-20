package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityCurrencyMenuBinding
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class CurrencyMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencyMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var currencyHeader: AppCompatTextView
    private lateinit var defaultCurrency: AppCompatTextView
    private lateinit var defaultCurrencyHeader: AppCompatTextView
    private lateinit var currencySpinner: Spinner
    private lateinit var progressBarCurrency: ProgressBar
    private lateinit var exitButton: MaterialButton
    private lateinit var saveCurrencyButton: MaterialButton
    private lateinit var soundManager: SoundManager
    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String

    private var textScalar by Delegates.notNull<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        progressBarCurrency.visibility = View.INVISIBLE

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        //  val getCurrency = userData.currency
        defaultCurrency.text = userData!!.currency

        // Load currencies from JSON
        val currencies = loadCurrenciesFromJson()

        // Check if currencies are loaded successfully
        if (currencies != null) {
            // Create an ArrayAdapter using the loaded currencies
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)

            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Apply the adapter to the spinner
            currencySpinner.adapter = adapter
        } else {
            // Handle the case where currencies couldn't be loaded
            Toast.makeText(
                this@CurrencyMenuActivity,
                "Failed to load currencies",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        currencySpinner.adapter = adapter

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        binding.currencyMenuBTNSaveButton.setOnClickListener {
            soundManager.playClickSound()
            val selectedCurrency = currencySpinner.selectedItem.toString()
            if (selectedCurrency.isNotEmpty()) {
                updateCurrency(userToken, selectedCurrency)
                soundManager.playClickSound()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@CurrencyMenuActivity,
                    "Please select a currency",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.currencyMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        currencyHeader = findViewById(R.id.currency_menu_TVW_changeCurrencyHeader)
        defaultCurrency = findViewById(R.id.currency_menu_TVW_defaultCurrency)
        defaultCurrencyHeader = findViewById(R.id.currency_menu_TVW_defaultCurrencyHeader)
        currencySpinner = findViewById(R.id.currency_menu_currencySpinner)
        exitButton = findViewById(R.id.currency_menu_BTN_exitButton)
        saveCurrencyButton = findViewById(R.id.currency_menu_BTN_saveButton)
        progressBarCurrency = findViewById(R.id.currency_menu_progressBar)
    }

    private fun updateCurrency(userToken: String, selectedCurrency: String) {
        val updateRequest =
            com.example.myapplication.requestResponse.CurrencyUpdateRequest(selectedCurrency)
        val call: Call<Void> = retrofitInterface.updateUserCurrency(userToken, updateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val appData = AppData.getInstance()
                    appData.userData!!.currency = selectedCurrency
                    // Handle successful response (e.g., show a success message)
                    Toast.makeText(
                        this@CurrencyMenuActivity,
                        "Currency updated successfully: $selectedCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@CurrencyMenuActivity,
                        "Failed to update currency",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@CurrencyMenuActivity,
                    "Network call failed: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadCurrenciesFromJson(): List<String> {
        val jsonString: String
        try {
            // Read the JSON file from the assets folder
            val inputStream = assets.open("currencies.json")
            jsonString = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // Handle the exception (e.g., file not found)
            return emptyList()  // Return an empty list or handle the error accordingly
        }

        // Parse the JSON array
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: JSONException) {
            // Handle JSON parsing error
            emptyList()  // Return an empty list or handle the error accordingly
        }
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            currencyHeader, defaultCurrency, defaultCurrencyHeader
            // Include any other TextViews or Buttons here
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