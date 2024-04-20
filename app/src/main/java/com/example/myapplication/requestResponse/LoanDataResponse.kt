package com.example.myapplication.requestResponse

import java.io.Serializable

data class LoanDataResponse(
    var lId: String? = null,
    var lenderId: String? = null,
    var borrowerId: String? = null,
    var amount: Double = 0.0,
    var currency: String? = null,
    var period: Int = 0,
    var interestRate: Double = 0.0,
    var startDate: String? = null,
    var endDate: String? = null,
    var expirationDate: String? = null,
    var status: LoanStatus = LoanStatus.PENDING,
    var contractHTML: String? = null,
    var locked: Boolean = false
) : Serializable {
    // Function to convert LoanDataResponse to Map
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "lId" to lId,
            "lenderId" to lenderId,
            "borrowerId" to borrowerId,
            "amount" to amount,
            "period" to period,
            "interestRate" to interestRate,
            "startDate" to startDate,
            "endDate" to endDate,
            "expirationDate" to expirationDate,
            "status" to status,
            "contractHTML" to contractHTML,
            "locked" to locked
        )
    }
}
