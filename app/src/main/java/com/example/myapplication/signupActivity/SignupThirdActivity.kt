package com.example.myapplication.signupActivity

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.example.myapplication.R
import com.example.myapplication.serverOperations.RetrofitInterface
import com.example.myapplication.serverOperations.RetrofitManager
import com.example.myapplication.userPreferences.SoundManager
import com.example.myapplication.databinding.ActivitySignupThirdBinding
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class SignupThirdActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupThirdBinding

    private var retrofit: Retrofit
    private  var retrofitInterface: RetrofitInterface

    init {
        retrofit = RetrofitManager.getRetrofit()
        retrofitInterface = RetrofitManager.getRetrofitInterface()
    }

    private lateinit var firstNameEditText: AppCompatEditText
    private lateinit var lastNameEditText: AppCompatEditText
    private lateinit var dobEditText: AppCompatEditText
    private lateinit var idEditText: AppCompatEditText
    private lateinit var addressEditText: AppCompatEditText
    private lateinit var cityEditText: AppCompatEditText
    private lateinit var stateEditText: AppCompatEditText
    private lateinit var nextButton: MaterialButton
    private lateinit var mProgressBar: ProgressBar
    private lateinit var personalDataHeader: AppCompatTextView
    private lateinit var exitButton: MaterialButton

    private lateinit var email: String
    private lateinit var password: String
    private lateinit var mobile: String
    //personal information
    private lateinit var firstName: String
    private lateinit var lastName: String
    private lateinit var dob: String
    private lateinit var id: String
    private lateinit var address: String
    private lateinit var city: String
    private lateinit var state: String

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupThirdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViews()
        mProgressBar.visibility = View.INVISIBLE

        dobEditText.setOnClickListener {
            showDatePickerDialog()

        }


        email = intent.getStringExtra("email").toString()
        password = intent.getStringExtra("password").toString()
        mobile = intent.getStringExtra("mobile").toString()

        soundManager = SoundManager(this)

        binding.signupThirdBTNNextButton.setOnClickListener {
            firstName = binding.signupThirdEDTFirstName.text.toString()
            lastName = binding.signupThirdEDTLastName.text.toString()
            dob = dobEditText.text.toString()
            id = binding.signupThirdEDTId.text.toString() // Get the ID
            address = binding.signupThirdEDTAddress.text.toString() // Get the address
            city = binding.signupThirdEDTCity.text.toString() // Get the city
            state = binding.signupThirdEDTState.text.toString() // Get the state

            // Check if all fields are filled and the ID, address, city, and state are valid
            if (isFirstNameValid(firstName) &&
                isLastNameValid(lastName) &&
                isUserOver18(dob) &&
                isAddressValid(address) &&
                isCityValid(city) &&
                isStateValid(state)
            ) {
                // Validate ID separately
                isIdValid(id) { isIdValid ->
                    if (isIdValid) {
                        // Now, validate the email separately
                        isIdValid(binding.signupThirdEDTId.text.toString()) { isIdValid ->
                            if (isIdValid) {
                                navigateToNextPage()
                            } else {
                                soundManager.playWrongClickSound()
                                showToast("ID is not valid")
                            }
                        }
                    } else {
                        soundManager.playWrongClickSound()
                        showToast("ID must be 9 digits long and must consist only of numbers")
                    }
                }
            } else {
                soundManager.playWrongClickSound()
                // Determine which field is invalid and show the corresponding error message
                when {
                    !isFirstNameValid(firstName) -> showToast("First name is not valid")
                    !isLastNameValid(lastName) -> showToast("Last name is not valid")
                    !isUserOver18(dob) -> showToast("You must be 18 or older to register.")
                    !isAddressValid(address) -> showToast("Address is not valid")
                    !isCityValid(city) -> showToast("City is not valid")
                    !isStateValid(state) -> showToast("State is not valid")
                    else -> showToast("All fields are mandatory")
                }
            }
        }


        binding.signupThirdBTNExitButton.setOnClickListener {
            soundManager.playClickSound()
            startActivity(Intent(this@SignupThirdActivity, SignupSecondOtpActivity::class.java))
            finish()
        }
    }

    private fun findViews() {
        firstNameEditText = findViewById(R.id.signup_third_EDT_firstName)
        lastNameEditText = findViewById(R.id.signup_third_EDT_lastName)
        dobEditText = findViewById(R.id.signup_third_EDT_DOB)
        idEditText = findViewById(R.id.signup_third_EDT_id)
        addressEditText = findViewById(R.id.signup_third_EDT_address)
        cityEditText = findViewById(R.id.signup_third_EDT_city)
        stateEditText = findViewById(R.id.signup_third_EDT_state)
        nextButton = findViewById(R.id.signup_third_BTN_nextButton)
        mProgressBar = findViewById(R.id.signup_third_progressBar)
        personalDataHeader = findViewById(R.id.signup_third_TVW_personalDataHeader)
        exitButton = findViewById(R.id.signup_third_BTN_exitButton)
    }

    private fun showToast(message: String) {
        Toast.makeText(this@SignupThirdActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun collectDataFromPageThird() {
        firstName = binding.signupThirdEDTFirstName.text.toString()
        lastName = binding.signupThirdEDTLastName.text.toString()
        dob = dobEditText.text.toString()
        id = binding.signupThirdEDTId.text.toString() // Get the ID
        address = binding.signupThirdEDTAddress.text.toString() // Get the address
        city = binding.signupThirdEDTCity.text.toString() // Get the city
        state = binding.signupThirdEDTState.text.toString() // Get the state
    }


    private fun navigateToNextPage(){
        soundManager = SoundManager(this@SignupThirdActivity)
        soundManager.playClickSound()
        val intent = Intent(this, SignupFourthActivity::class.java)
        intent.putExtra("email" , email)
        intent.putExtra("password" , password)
        intent.putExtra("mobile" , mobile)
        intent.putExtra("firstName" , firstName)
        intent.putExtra("lastName" , lastName)
        intent.putExtra("dob" , dob)
        intent.putExtra("id" , id)
        intent.putExtra("address" , address)
        intent.putExtra("city" , city)
        intent.putExtra("state" , state)
        startActivity(intent)
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _: DatePicker?, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
            dobEditText.setText(formattedDate)
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun isUserOver18(dob: String): Boolean {
        val dobDate = dob.split("/")
        val selectedYear = dobDate[2].toInt()
        val selectedMonth = dobDate[0].toInt() - 1
        val selectedDay = dobDate[1].toInt()

        val currentDate = Calendar.getInstance()
        val selectedDate = Calendar.getInstance()
        selectedDate.set(selectedYear, selectedMonth, selectedDay)

        var age = currentDate.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR)

        if (currentDate.get(Calendar.MONTH) < selectedDate.get(Calendar.MONTH) ||
            (currentDate.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    currentDate.get(Calendar.DAY_OF_MONTH) < selectedDate.get(Calendar.DAY_OF_MONTH))) {
            age--
        }

        return age >= 18
    }

    private fun isIdValid(id: String, callback: (Boolean) -> Unit) {
        // Perform the local ID validation
        val localIdValidation = id.length == 9 && id.all { it.isDigit() }

        if (localIdValidation) {
            // ID format is valid, check if it already exists
            val map = hashMapOf("id" to id)
            val call = retrofitInterface.checkIdExists(map)

            call.enqueue(object : Callback<com.example.myapplication.requestResponse.IdCheckResponse> {
                override fun onResponse(call: Call<com.example.myapplication.requestResponse.IdCheckResponse>, response: Response<com.example.myapplication.requestResponse.IdCheckResponse>) {
                    if (response.isSuccessful) {
                        val idCheckResponse = response.body()
                        if (idCheckResponse != null) {
                            if (idCheckResponse.exists) {
                                // Server response indicates ID already exists
                                Toast.makeText(this@SignupThirdActivity, "ID already exists", Toast.LENGTH_SHORT).show()
                                callback(false)
                            } else {
                                // ID is valid and does not exist
                                callback(true)
                            }
                        }
                    } else {
                        // Server response error
                        Toast.makeText(this@SignupThirdActivity, "Server response error", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }

                override fun onFailure(call: Call<com.example.myapplication.requestResponse.IdCheckResponse>, t: Throwable) {
                    // Handle network or other failures
                    Toast.makeText(this@SignupThirdActivity, "Network error", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            })
        } else {
            // Invalid ID
            Toast.makeText(this@SignupThirdActivity, "Invalid ID", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    private fun isAddressValid(address: String): Boolean {
        return address.length >= 5 && address.all { it.isLetterOrDigit() || it.isWhitespace() }
    }

    private fun isCityValid(city: String): Boolean {
        return city.length >= 4 && city.all { it.isLetter() || it.isWhitespace() } && city.first().isUpperCase()
    }

    private fun isStateValid(state: String): Boolean {
        return state.length >= 3 && state.all { it.isLetter() } &&  state.first().isUpperCase()
    }

    private fun isFirstNameValid(firstName: String): Boolean {
        return firstName.isNotEmpty() &&
                firstName.all { it.isLetter() } &&
                firstName.first().isUpperCase() &&
                firstName.substring(1).all { it.isLowerCase() }
    }

    private fun isLastNameValid(lastName: String): Boolean {
        return lastName.isNotEmpty() &&
                lastName.all { it.isLetter() } &&
                lastName.first().isUpperCase() &&
                lastName.substring(1).all { it.isLowerCase() }
    }
}