package com.example.myapplication.changesUpdates

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
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
import com.example.myapplication.databinding.ActivityChangePasswordBinding
import com.example.myapplication.requestResponse.PasswordUpdateRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import kotlin.properties.Delegates


class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var oldPasswordEditText: AppCompatEditText
    private lateinit var passwordEditText: AppCompatEditText
    private lateinit var confirmPassEditText: AppCompatEditText
    private lateinit var changePasswordBTNConfirm: MaterialButton
    private lateinit var changePasswordBTNExitButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var changePasswordHeader: AppCompatTextView

    private lateinit var userData: UserDataResponse
    private lateinit var userToken: String

    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        progressBar.visibility = View.INVISIBLE
        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()
        userData = appData.userData!!

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        textScalar = retrieveTextScalarFromPreferences()
        applyTextScalar()


        binding.changePasswordBTNConfirm.setOnClickListener {
            val oldPass = binding.changePasswordEDTOldPassword.text.toString()
            val newPass = binding.changePasswordEDTNewPassword.text.toString()
            val confirmPass = binding.changePasswordEDTConfirmPassword.text.toString()

            if (oldPass.isNotEmpty() && newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
                isValidPassword(newPass) { isPasswordValid ->
                    if (isPasswordValid) {
                        soundManager.playClickSound()
                        progressBar.visibility = View.VISIBLE
                        val hashedNewPassword = hashPassword(newPass)
                        val hashedOldPassword = hashPassword(oldPass)
                        updatePassword(userToken, hashedNewPassword, hashedOldPassword)
                        recreate()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        soundManager.playWrongClickSound()
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Invalid password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "All fields are mandatory",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        binding.changePasswordBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun findViews() {
        oldPasswordEditText = findViewById(R.id.change_password_EDT_oldPassword)
        passwordEditText = findViewById(R.id.change_password_EDT_newPassword)
        confirmPassEditText = findViewById(R.id.change_password_EDT_confirmPassword)
        changePasswordBTNConfirm = findViewById(R.id.change_password_BTN_confirm)
        changePasswordHeader = findViewById(R.id.change_password_TVW_header)
        progressBar = findViewById(R.id.change_password_progressBar)
        changePasswordBTNExitButton = findViewById(R.id.change_password_BTN_exitButton)
    }


    private fun updatePassword(userToken: String, newPassword: String, oldPassword: String) {
        val passwordUpdateRequest = PasswordUpdateRequest(newPassword, oldPassword)
        val call: Call<Void> = retrofitInterface.updatePassword(userToken, passwordUpdateRequest)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Password updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Handle unsuccessful response, display an error message or perform appropriate actions
                    progressBar.visibility = View.INVISIBLE
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Failed to update password. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle network or other failures
                progressBar.visibility = View.INVISIBLE
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Network error. Please check your connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun isValidPassword(password: String, callback: (Boolean) -> Unit) {
        // Perform the local password validation
        val localPasswordValidation =
            !password.isNullOrBlank() &&
                    password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                    containsRequiredCharacterTypes(password) &&
                    !isCommonPassword(password) &&
                    !isDictionaryWord(password) &&
                    !hasRepeatedCharacters(password) &&
                    !isPasswordInHistory(password)

        if (localPasswordValidation) {
            // The password is valid based on local criteria
            callback(true)
        } else {
            showToast("Invalid password")
            callback(false)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@ChangePasswordActivity, message, Toast.LENGTH_SHORT).show()
    }


    private fun containsRequiredCharacterTypes(password: String): Boolean {
        val containsUppercase = password.any { it.isUpperCase() }
        val containsLowercase = password.any { it.isLowerCase() }
        val containsDigit = password.any { it.isDigit() }
        val containsSpecialChar = password.any { !it.isLetterOrDigit() }

        return containsUppercase && containsLowercase && containsDigit && containsSpecialChar
    }

    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = listOf("password123", "123456", "qwerty", "admin")
        return commonPasswords.contains(password)
    }

    private fun isDictionaryWord(password: String): Boolean {
        val dictionaryWords = listOf("password", "admin", "letmein", "welcome")
        return dictionaryWords.contains(password.lowercase(Locale.getDefault()))
    }

    private fun hasRepeatedCharacters(password: String): Boolean {
        return password.windowed(2).any { it[0] == it[1] }
    }

    private fun isPasswordInHistory(password: String): Boolean {
        val passwordHistory = listOf("previousPassword1", "previousPassword2", "previousPassword3")
        return passwordHistory.contains(password)
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 128
    }


    private fun hashPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        val hashedBytes = messageDigest.digest(passwordBytes)
        //return Base64.encodeToString(hashedBytes, Base64.NO_WRAP).trim()
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun applyTextScalar() {
        val textElements = listOf(
            oldPasswordEditText, passwordEditText, confirmPassEditText,
            changePasswordHeader
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