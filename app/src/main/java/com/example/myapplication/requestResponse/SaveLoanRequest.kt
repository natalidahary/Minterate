package com.example.myapplication.requestResponse

data class SaveLoanRequest(
    val userToken: String,
    val loanData: LoanDataRequest
)
