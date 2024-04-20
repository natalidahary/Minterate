package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.changesUpdates.ChangeAddressMenuActivity
import com.example.myapplication.changesUpdates.ChangeCreditMenuActivity
import com.example.myapplication.changesUpdates.ChangeMobileMenuActivity
import com.example.myapplication.changesUpdates.ChangePasswordActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.loginActivity.LoginActivity
import com.example.myapplication.requestResponse.Transaction
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userActions.BorrowActivity
import com.example.myapplication.userActions.LendActivity
import com.example.myapplication.userActions.TransactionAdapter
import com.example.myapplication.userPreferences.AccessibilityMenuActivity
import com.example.myapplication.userPreferences.CurrencyMenuActivity
import com.example.myapplication.userPreferences.DeleteUserMenuActivity
import com.example.myapplication.userPreferences.HelpMenuActivity
import com.example.myapplication.userPreferences.LoansMenuActivity
import com.example.myapplication.userPreferences.NotificationsMenuActivity
import com.example.myapplication.userPreferences.SoundManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var retrofit: Retrofit
    private var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval: Long = 7000 // 7 second
    private lateinit var heyText: AppCompatTextView
    private lateinit var loansBalText: AppCompatTextView
    private lateinit var lendButton: MaterialButton
    private lateinit var borrowButton: MaterialButton
    private lateinit var settingButton: MaterialButton
    private lateinit var personalButton: MaterialButton
    private lateinit var personalDialog: Dialog
    private lateinit var settingsDialog: Dialog
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var totalLoansText: AppCompatTextView
    private lateinit var transactionsText: AppCompatTextView
    private lateinit var userToken: String
    private lateinit var userData: UserDataResponse
    private var textScalar by Delegates.notNull<Float>()
    private lateinit var soundManager: SoundManager
    private var dialogsOpened: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        textScalar = retrieveTextScalarFromPreferences()

        loadDataToMain()

        // Initialize and set up the RecyclerView with the list of transactions
        recyclerView = findViewById(R.id.main_transactionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

        transactionAdapter = TransactionAdapter(userData.id, emptyList())
        recyclerView.adapter = transactionAdapter

        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(userData.sounds)

        binding.mainBTNLendButton.setOnClickListener {
            soundManager.setSoundEnabled(userData.sounds)
            soundManager.playClickSound()
            val intent = Intent(this, LendActivity::class.java)
            startActivity(intent)
        }

        binding.mainBTNBorrowButton.setOnClickListener {
            soundManager.setSoundEnabled(userData.sounds)
            soundManager.playClickSound()
            val intent = Intent(this, BorrowActivity::class.java)
            startActivity(intent)
        }

        binding.mainBTNPersonalButton.setOnClickListener {
            soundManager.setSoundEnabled(userData.sounds)
            soundManager.playClickSound()
            showMenuPersonalDialog()
        }

        binding.mainBTNSettingButton.setOnClickListener {
            soundManager.setSoundEnabled(userData.sounds)
            soundManager.playClickSound()
            showMenuSettingsDialog()
        }

    }

    private fun findViews() {
        heyText = findViewById(R.id.main_TVW_heyText)
        loansBalText = findViewById(R.id.main_TVW_loansBalText)
        lendButton = findViewById(R.id.main_BTN_lendButton)
        borrowButton = findViewById(R.id.main_BTN_borrowButton)
        settingButton = findViewById(R.id.main_BTN_settingButton)
        personalButton = findViewById(R.id.main_BTN_personalButton)
        totalLoansText = findViewById(R.id.main_TVW_totalLoansText)
        transactionsText = findViewById(R.id.main_TVW_transactionsText)
    }


    private fun loadDataToMain() {
        val appData = AppData.getInstance()
        userToken = appData.userToken.toString()

        userData = appData.userData!!

        textScalar = userData.textScalar

        val transactions = appData.transactions ?: emptyList<Transaction>()

        recyclerView = findViewById(R.id.main_transactionRecyclerView)

        // Initialize and set up the RecyclerView with the list of transactions
        transactionAdapter = TransactionAdapter(userData.id, transactions)

        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.adapter = transactionAdapter

        updateUI()
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        val firstName = userData.firstName
        val lastName = userData.lastName
        val totalBalance = userData.totalBalance.toString()
        val userCurrency = userData.currency
        var formatBalance = formatWithCommas(totalBalance)
        var formatCurrency = extractCurrencyValue(userCurrency)

        heyText.text = "Hey $firstName $lastName"
        loansBalText.text = "$formatBalance $formatCurrency"

        // Update RecyclerView with the latest transactions
        val transactions = AppData.getInstance().transactions ?: emptyList()
        transactionAdapter.updateTransactions(transactions)

        // Notify the adapter that the data set has changed
        transactionAdapter.notifyDataSetChanged()
    }


    private fun showMenuPersonalDialog() {
        dialogsOpened = true
        soundManager = SoundManager(this@MainActivity)
        soundManager.setSoundEnabled(userData.sounds)
        personalDialog = Dialog(this)
        personalDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set the gravity to left
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        personalDialog.window?.attributes = layoutParams

        personalDialog.setContentView(R.layout.activity_personal_menu)

        // Find the TextViews in the layout
        val navHome = personalDialog.findViewById<TextView>(R.id.nav_home)
        val navSignOut = personalDialog.findViewById<TextView>(R.id.nav_sign_out)
        val navChangeCredit = personalDialog.findViewById<TextView>(R.id.nav_change_credit)
        val navSecurityMenu = personalDialog.findViewById<TextView>(R.id.nav_security)
        val navChangeAddressMenu = personalDialog.findViewById<TextView>(R.id.nav_change_address)
        val navLoansMenu = personalDialog.findViewById<TextView>(R.id.nav_loans)
        val navHelpMenu = personalDialog.findViewById<TextView>(R.id.nav_help)

        navHome.setOnClickListener {
            soundManager.playClickSound()
            dialogsOpened = false
            personalDialog.dismiss() // Dismiss the dialog after navigation
        }

        navSignOut.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        navChangeCredit.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, ChangeCreditMenuActivity::class.java)
            startActivity(intent)
        }

        navSecurityMenu.setOnClickListener {
            soundManager.playClickSound()
            showSecurityOptionsDialog()
        }

        navChangeAddressMenu.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, ChangeAddressMenuActivity::class.java)
            startActivity(intent)
        }

        navLoansMenu.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, LoansMenuActivity::class.java)
            startActivity(intent)
        }

        navHelpMenu.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, HelpMenuActivity::class.java)
            startActivity(intent)
        }

        // Show the dialog
        personalDialog.show()
    }


    private fun showMenuSettingsDialog() {
        dialogsOpened = true
        soundManager = SoundManager(this@MainActivity)
        soundManager.setSoundEnabled(userData.sounds)
        settingsDialog = Dialog(this)
        settingsDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set the gravity to right
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        settingsDialog.window?.attributes = layoutParams

        settingsDialog.setContentView(R.layout.activity_settings_menu)

        val navHome = settingsDialog.findViewById<TextView>(R.id.nav_home)
        val aboutUs = settingsDialog.findViewById<TextView>(R.id.nav_about)
        val termsPrivacy = settingsDialog.findViewById<TextView>(R.id.nav_terms_conditions)
        val accessibility = settingsDialog.findViewById<TextView>(R.id.nav_accessibility)
        val notifications = settingsDialog.findViewById<TextView>(R.id.nav_Notification)
        val currency = settingsDialog.findViewById<TextView>(R.id.nav_currency)
        val deleteUser = settingsDialog.findViewById<TextView>(R.id.nav_delete_account)

        navHome.setOnClickListener {
            soundManager.playClickSound()
            dialogsOpened = false
            settingsDialog.dismiss() // Dismiss the dialog after navigation
        }

        aboutUs.setOnClickListener {
            soundManager.playClickSound()
            showAboutDialog()
        }

        termsPrivacy.setOnClickListener {
            soundManager.playClickSound()
            showTermsPrivacyDialog()
        }

        accessibility.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, AccessibilityMenuActivity::class.java)
            startActivity(intent)
        }

        notifications.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, NotificationsMenuActivity::class.java)
            startActivity(intent)
        }

        currency.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, CurrencyMenuActivity::class.java)
            startActivity(intent)
        }

        deleteUser.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, DeleteUserMenuActivity::class.java)
            startActivity(intent)
        }

        settingsDialog.show()
    }


    private fun showAboutDialog() {
        dialogsOpened = true
        soundManager = SoundManager(this@MainActivity)
        soundManager.setSoundEnabled(userData.sounds)
        val aboutDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        aboutDialog.setContentView(R.layout.activity_about_menu)

        // Find the TextViews in the layout
        val exitButton = aboutDialog.findViewById<TextView>(R.id.update_password_login_BTN_exitButton)

        exitButton.setOnClickListener {
            soundManager.playClickSound()
            dialogsOpened = false
            aboutDialog.dismiss()
        }
        aboutDialog.show()
    }

    private fun showTermsPrivacyDialog() {
        dialogsOpened = true
        soundManager = SoundManager(this@MainActivity)
        soundManager.setSoundEnabled(userData.sounds)
        val termsPrivacyDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        termsPrivacyDialog.setContentView(R.layout.activity_terms_privacy)

        // Find the TextViews in the layout
        val exitButton = termsPrivacyDialog.findViewById<TextView>(R.id.update_password_login_BTN_exitButton)

        exitButton.setOnClickListener {
            soundManager.playClickSound()
            dialogsOpened = false
            termsPrivacyDialog.dismiss()
        }
        termsPrivacyDialog.show()
    }


    private fun showSecurityOptionsDialog() {
        soundManager = SoundManager(this@MainActivity)
        soundManager.setSoundEnabled(userData.sounds)
        val securityOptionsDialog = Dialog(this)
        securityOptionsDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.gravity = Gravity.NO_GRAVITY
        securityOptionsDialog.window?.attributes = layoutParams

        securityOptionsDialog.setContentView(R.layout.activity_security_options)

        // Find the Buttons in the layout
        val navChangePassword = securityOptionsDialog.findViewById<TextView>(R.id.nav_changePassword)
        val navChangeMobile = securityOptionsDialog.findViewById<TextView>(R.id.nav_changeMobile)

        navChangePassword.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        navChangeMobile.setOnClickListener {
            soundManager.playClickSound()
            val intent = Intent(this, ChangeMobileMenuActivity::class.java)
            startActivity(intent)
        }

        // Show the dialog
        securityOptionsDialog.show()
    }

    fun formatWithCommas(input: String): String {
        val number = try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            return input // Return input as is if it's not a valid number
        }

        return String.format("%,.2f", number)
    }

    fun extractCurrencyValue(currencyString: String): String {
        val regex = Regex("\\((.*?)\\)")
        val matchResult = regex.find(currencyString)
        return matchResult?.groupValues?.get(1) ?: ""
    }

    private fun textSize() {
        val textViews = listOf(
            binding.mainTVWHeyText,
            binding.mainBTNBorrowButton,
            binding.mainBTNLendButton,
            binding.mainTVWLoansBalText,
            binding.mainTVWTotalLoansText,
            binding.mainTVWTransactionsText
        )
        textViews.forEach { textView ->
            textView.textSize = textView.textSize * textScalar
            textView.requestLayout()
            textView.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()

        // Retrieve the text scalar from shared preferences
        textScalar = retrieveTextScalarFromPreferences()

        // Update the text size immediately
        textSize()

        startDataRefreshTask()

        loadDataToMain()

    }


    private fun startDataRefreshTask() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Run the functions to refresh data
                getUserDataByToken(userToken)
                fetchUserTransactions(userToken)

                if (!dialogsOpened) {
                    Log.d("REFRESH-TASK", "~~~~~~~~~~~~ Data refreshed ! ~~~~~~~~~~~~")
                    // Run the code block only when both dialogs are not open
                    finish()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }

                // Reset the flag for the next iteration
                // Schedule the next run after the specified interval
                handler.postDelayed(this, refreshInterval)
            }
        }, refreshInterval)
    }

    override fun onPause() {
        super.onPause()
        // Stop the periodic task when the activity is paused
        handler.removeCallbacksAndMessages(null)
    }
    override fun onDestroy() {
        // Perform any cleanup tasks or resource releases here
        // For example:
        soundManager.release()
        handler.removeCallbacksAndMessages(null) // Stop the data refresh task
        super.onDestroy()
    }

    private fun retrieveTextScalarFromPreferences(): Float {
        val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return preferences.getFloat("textScalar", 1.0f)
    }

    private fun getUserDataByToken(userToken: String) {
        val call: Call<UserDataResponse> = retrofitInterface.getUserDataByToken(userToken)

        call.enqueue(object : Callback<UserDataResponse> {
            override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        val appData = AppData.getInstance()
                        appData.userData = result

                    } else {
                        Log.e(ContentValues.TAG, "Null response body")
                    }

                } else {
                    Log.e(ContentValues.TAG, "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                Log.e(ContentValues.TAG, "Network call failed: ${t.message}")
            }
        })
    }

    private fun fetchUserTransactions(userToken: String) {
        val call: Call<List<Transaction>> = retrofitInterface.getUserTransactions(userToken)

        call.enqueue(object : Callback<List<Transaction>> {
            override fun onResponse(call: Call<List<Transaction>>, response: Response<List<Transaction>>) {
                if (response.isSuccessful) {
                    val transactions = response.body()
                    if (!transactions.isNullOrEmpty()) {
                        // Sort transactions by date in descending order
                        var userTransactions = transactions.sortedByDescending { it.date }
                        val appData = AppData.getInstance()
                        appData.transactions = userTransactions
                        //updateUI()
                        Log.d(ContentValues.TAG, "Transactions received and sorted in descending order successfully: $userTransactions")
                    } else {
                        Log.d(ContentValues.TAG, "No transactions found for the user")
                        // Handle the case where there are no transactions
                        // For example, you could show a message to the user
                    }
                } else {
                    Log.e(ContentValues.TAG, "Unsuccessful response: ${response.code()}")
                    // Handle the case where the request was not successful
                }
            }

            override fun onFailure(call: Call<List<Transaction>>, t: Throwable) {
                Log.e(ContentValues.TAG, "Network call failed: ${t.message}")
                // Handle the case where the network call failed
            }
        })
    }

}

