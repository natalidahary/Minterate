package com.example.myapplication.requestResponse

data class RecordTransactionRequest(
    val userToken: String,
    val transaction: Transaction
)
