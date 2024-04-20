package com.example.myapplication.requestResponse

data class PasswordUpdateRequest(
    val newPassword: String,
    val oldPassword: String
)