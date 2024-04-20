package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class LoanLockStatusResponse(
    @SerializedName("locked")
    val locked: Boolean
)
