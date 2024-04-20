package com.example.myapplication.requestResponse

import com.example.myapplication.requestResponse.LoanDataRequest

data class UpdateAndAddLoanRequest(
    val userToken: String,
    val lId: String,
    val loanData: LoanDataRequest
)
