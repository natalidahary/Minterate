package com.example.myapplication.requestResponse

import java.io.Serializable

data class UserDataResponse(
    var mobile: String,
    var firstName: String,
    var lastName: String,
    var totalBalance: Float,
    var lastFourDigits: String,
    var sounds: Boolean,
    var currency: String,
    var textScalar: Float,
    var cvv: String,
    var monthYear: String,
    var id: String,
    var address: String,
    var city: String,
    var state: String
): Serializable

