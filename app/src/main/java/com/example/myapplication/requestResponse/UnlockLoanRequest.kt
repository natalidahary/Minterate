package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class UnlockLoanRequest(
    @SerializedName("userToken") val userToken: String,
    @SerializedName("lId") val loanId: String
)
