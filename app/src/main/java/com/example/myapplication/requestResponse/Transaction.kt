package com.example.myapplication.requestResponse

import java.io.Serializable

data class Transaction (
        var amount: Double = 0.0,
        var currency: String? = null,
        var date: String? = null,
        var loanId: String? = null,// link transaction with loan
        var origin: String? = null,
        var destination: String? = null,
        var originFirstName: String? = null,
        var originLastName: String? = null,
        var destinationFirstName: String? = null,
        var destinationLastName: String? = null,
        var paymentCount: String? = null

) : Serializable
