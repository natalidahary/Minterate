package com.example.myapplication.userPreferences

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.AppData
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.databinding.ActivityDeleteUserMenuBinding
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.UserDataResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class DeleteUserMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteUserMenuBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var deleteUserHeader: TextView
    private lateinit var userNameText: TextView
    private lateinit var userIdText: TextView
    private lateinit var confirmDeleteHeader: TextView
    private lateinit var deleteUserButton: Button
    private lateinit var exitButton: Button
    private lateinit var deleteProgressBar: ProgressBar
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteUserMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        deleteProgressBar.visibility = View.INVISIBLE

        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        updateUI()

        // Initialize and set soundManager
        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(true)


        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()

        binding.deleteUserMenuBTNDeleteUserButton.setOnClickListener {
            soundManager.playClickSound()
            deleteProgressBar.visibility = View.VISIBLE
            checkAndDeleteUser()
        }

        binding.deleteUserMenuBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        deleteUserHeader = findViewById(R.id.delete_user_menu_TVW_header)
        userNameText = findViewById(R.id.delete_user_menu_TVW_userNameText)
        userIdText = findViewById(R.id.delete_user_menu_TVW_userIdText)
        confirmDeleteHeader = findViewById(R.id.delete_user_menu_TVW_confirmDeleteHeader)
        deleteUserButton = findViewById(R.id.delete_user_menu_BTN_deleteUserButton)
        exitButton = findViewById(R.id.delete_user_menu_BTN_exitButton)
        deleteProgressBar = findViewById(R.id.delete_user_menu_progressBar)
    }


    private fun updateUI() {
        userNameText.text = userData.firstName + " " + userData.lastName
        userIdText.text = userData.id
    }


    private fun checkAndDeleteUser() {
        retrofitInterface.checkUserActiveLoans(userToken).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("DeleteUserActivity", "checkUserActiveLoans onResponse: ${response.body()?.message}")
                if (response.isSuccessful && response.body()?.message == "No active loans") {
                    deleteUser()
                } else {
                    Toast.makeText(this@DeleteUserMenuActivity, "User cannot be deleted due to active loans", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@DeleteUserMenuActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("DeleteUserActivity", "checkUserActiveLoans onFailure: ${t.message}")
                Toast.makeText(this@DeleteUserMenuActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun deleteUser() {
        retrofitInterface.deleteUser(userToken).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("DeleteUserActivity", "deleteUser onResponse: ${response.body()?.message}")
                if (response.isSuccessful) {
                    Toast.makeText(this@DeleteUserMenuActivity, "User deleted successfully", Toast.LENGTH_LONG).show()
                    // Navigate to LoginActivity or any other activity as needed
                    val intent = Intent(this@DeleteUserMenuActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@DeleteUserMenuActivity, "Failed to delete user", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("DeleteUserActivity", "deleteUser onFailure: ${t.message}")
                Toast.makeText(this@DeleteUserMenuActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            deleteUserHeader, userNameText, userIdText, confirmDeleteHeader
        )
        textElements.forEach { element ->
            element.textSize = element.textSize * textScalar
        }
    }

    override fun onDestroy() {
        soundManager.release() // Release resources used by soundManager
        super.onDestroy()
    }

}