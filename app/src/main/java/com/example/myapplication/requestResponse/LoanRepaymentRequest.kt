package com.example.myapplication.requestResponse

data class LoanRepaymentRequest(
    val loanId: String,
    val userToken: String
)
