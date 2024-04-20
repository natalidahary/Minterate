package com.example.myapplication.changesUpdates

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivityUpdatePasswordFinalLoginBinding
import com.example.myapplication.requestResponse.UpdatePasswordLoginRequest
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale


class UpdatePasswordFinalLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdatePasswordFinalLoginBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var newPassword: AppCompatEditText
    private lateinit var confirmPassword: AppCompatEditText
    private lateinit var savePassButton: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var passwordProgressBar: ProgressBar
    private lateinit var updatePasswordHeader: AppCompatTextView
    private lateinit var soundManager: SoundManager
    private lateinit var email: String
    private lateinit var password: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePasswordFinalLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        findViews()
        passwordProgressBar.visibility = View.INVISIBLE

        soundManager = SoundManager(this)

        email = intent.getStringExtra("email").toString()

        binding.updatePasswordLoginFinalBTNSave.setOnClickListener {
            val newPassword = binding.updatePasswordLoginFinalEDTNewPassword.text.toString()
            val confirmPassword = binding.updatePasswordLoginFinalEDTConfirmPassword.text.toString()

            if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (isValidPassword(newPassword, confirmPassword)) {
                        soundManager.playClickSound()
                        passwordProgressBar.visibility = View.VISIBLE
                        password = hashPassword(newPassword)
                        updatePassword(email, password)
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("email", email)
                        intent.putExtra("password", password)
                        startActivity(intent)
                    } else {
                        soundManager.playWrongClickSound()
                        Toast.makeText(
                            this@UpdatePasswordFinalLoginActivity,
                            "Invalid password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@UpdatePasswordFinalLoginActivity,
                    "All fields are mandatory",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.updatePasswordLoginFinalBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@UpdatePasswordFinalLoginActivity, LoginActivity::class.java))
            finish()
        }
    }



    private fun findViews() {
        newPassword = findViewById(R.id.update_password_login_final_EDT_newPassword)
        confirmPassword = findViewById(R.id.update_password_login_final_EDT_confirmPassword)
        savePassButton = findViewById(R.id.update_password_login_final_BTN_save)
        passwordProgressBar = findViewById(R.id.update_password_login_final_progressBar)
        updatePasswordHeader = findViewById(R.id.update_password_login_final_TVW_header)
    }


    private fun updatePassword(email: String, newPassword: String) {
        val passwordUpdateRequest = UpdatePasswordLoginRequest(newPassword)
        val call: Call<Void> = retrofitInterface.changePasswordLogin(email, passwordUpdateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Password update successful
                    Toast.makeText(
                        this@UpdatePasswordFinalLoginActivity,
                        "Password updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Perform any additional actions on success if needed
                } else {
                    // Handle unsuccessful response, display an error message or perform appropriate actions
                    passwordProgressBar.visibility = View.INVISIBLE
                    soundManager.playWrongClickSound()
                    Toast.makeText(
                        this@UpdatePasswordFinalLoginActivity,
                        "Failed to update password. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle network or other failures
                passwordProgressBar.visibility = View.INVISIBLE
                soundManager.playWrongClickSound()
                Toast.makeText(
                    this@UpdatePasswordFinalLoginActivity,
                    "Network error. Please check your connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }




    private fun hashPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        val hashedBytes = messageDigest.digest(passwordBytes)
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }


    private fun isValidPassword(password: String?, confirmPassword: String?): Boolean {
        var isPasswordValid = !password.isNullOrBlank() &&
                password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                containsRequiredCharacterTypes(password) &&
                !isCommonPassword(password) &&
                !isDictionaryWord(password) &&
                !hasRepeatedCharacters(password) &&
                !isPasswordInHistory(password)

        if (!isPasswordValid) {
            Toast.makeText(this@UpdatePasswordFinalLoginActivity, "Invalid password", Toast.LENGTH_SHORT).show()
        }
        if (password != confirmPassword) {
            isPasswordValid = false
            Toast.makeText(this@UpdatePasswordFinalLoginActivity, "Passwords must match", Toast.LENGTH_SHORT).show()
        }

        return isPasswordValid
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

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}