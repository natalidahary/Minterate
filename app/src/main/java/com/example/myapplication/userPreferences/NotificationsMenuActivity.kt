package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.AppData
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityNotificationsMenuBinding
import com.example.myapplication.requestResponse.SoundSettingsUpdateRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class NotificationsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var soundManager: SoundManager
    private lateinit var soundCheckBox: CheckBox
    private lateinit var pushOneCheckbox: CheckBox
    private lateinit var pushTwoCheckbox: CheckBox
    private lateinit var saveButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var NotificationsHeader: AppCompatTextView
    private lateinit var SoundHeader: AppCompatTextView
    private lateinit var soundText: AppCompatTextView
    private lateinit var PushupsHeader: AppCompatTextView
    private lateinit var pushOneText: AppCompatTextView
    private lateinit var pushTwoText: AppCompatTextView
    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String
    private lateinit var progressBarNotification: ProgressBar
    private var sounds: Boolean = true
    private var textScalar by Delegates.notNull<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        progressBarNotification.visibility = View.INVISIBLE

        sounds = if(sounds != null){
            userData.sounds
        } else{
            true
        }

        //set soundbox checkbox for default user's choice
        soundCheckBox.isChecked = sounds

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        saveButton.setOnClickListener {
            progressBarNotification.visibility = View.VISIBLE
            updateSoundSettings(userToken, soundCheckBox.isChecked)
            recreate()
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.notificationsMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        exitButton = findViewById(R.id.notifications_menu_BTN_exitButton)
        saveButton = findViewById(R.id.notifications_menu_BTN_saveButton)
        NotificationsHeader = findViewById(R.id.notifications_menu_TVW_header)
        SoundHeader = findViewById(R.id.notifications_menu_TVW_soundHeader)
        soundText = findViewById(R.id.notifications_menu_TVW_soundText)
        PushupsHeader = findViewById(R.id.notifications_menu_TVW_pushUpsHeader)
        pushOneText = findViewById(R.id.notifications_menu_TVW_pushOneText)
        pushTwoText = findViewById(R.id.notifications_menu_TVW_pushTwoText)
        progressBarNotification = findViewById(R.id.notifications_menu_progressBar)
        soundCheckBox = findViewById(R.id.notifications_menu_soundCheckBox)
        pushOneCheckbox = findViewById(R.id.notifications_menu_pushOneCheckBox)
        pushTwoCheckbox = findViewById(R.id.notifications_menu_pushTwoCheckBox)
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

    private fun updateSoundSettings(userToken: String, soundsEnabled: Boolean) {
        val updateRequest = SoundSettingsUpdateRequest(soundsEnabled)
        val call: Call<Void> = retrofitInterface.updateSoundSettings(userToken, updateRequest)
        soundManager.setSoundEnabled(sounds)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val appData = AppData.getInstance()
                    appData.userData!!.sounds = soundsEnabled
                    // Handle successful response (e.g., show a success message)
                    Toast.makeText(
                        this@NotificationsMenuActivity, // Replace with your actual activity reference
                        "Sound settings updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@NotificationsMenuActivity, // Replace with your actual activity reference
                        "Failed to update sound settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure (e.g., show an error message)
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@NotificationsMenuActivity, // Replace with your actual activity reference
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
            NotificationsHeader, SoundHeader,
            soundText, PushupsHeader, pushOneText, pushTwoText
            // Include other TextViews and EditTexts as needed
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

}