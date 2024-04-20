package com.example.myapplication.requestResponse

import java.io.Serializable

data class LoanDataRequest(
    var lenderId: String? = null,
    var borrowerId: String? = null,
    var amount: Double = 0.0,
    var currency: String? = null,
    var period: Int = 0,
    var interestRate: Double = 0.0,
    var lId: String? = null,
    var startDate: String? = null,
    var endDate: String? = null,
    var expirationDate: String? = null,
    var status: LoanStatus = LoanStatus.PENDING,
    var contractHTML: String? = null
) : Serializable

enum class LoanStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    EXPIRED
}
