package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class ApproveLoanByLockResponse(
    @SerializedName("message")
    val message: String
)
