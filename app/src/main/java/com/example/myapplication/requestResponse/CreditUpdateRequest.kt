package com.example.myapplication.requestResponse

data class CreditUpdateRequest(
    var lastFourDigits: String,
    var cardNumber: String,
    var monthYear: String,
    var cvv: String
)
