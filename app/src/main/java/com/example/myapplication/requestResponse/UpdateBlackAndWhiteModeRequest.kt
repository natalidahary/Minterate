package com.example.myapplication.requestResponse

import com.google.gson.annotations.SerializedName

data class UpdateBlackAndWhiteModeRequest(
    @SerializedName("isBlackAndWhiteMode") val isBlackAndWhiteMode: Boolean
)