package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class UnlockLoanResponse(
    @SerializedName("message")
    val message: String
)
