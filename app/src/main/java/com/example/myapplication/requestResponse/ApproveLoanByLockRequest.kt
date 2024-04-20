package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class ApproveLoanByLockRequest(
    @SerializedName("userToken") val userToken: String,
    @SerializedName("lId") val lId: String
)
