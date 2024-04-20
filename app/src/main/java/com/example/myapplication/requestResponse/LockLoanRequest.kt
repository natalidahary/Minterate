package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class LockLoanRequest(
    @SerializedName("userToken") val userToken: String,
    @SerializedName("lId") val loanId: String
)
