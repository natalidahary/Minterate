package com.example.myapplication

import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.Transaction
import com.example.myapplication.requestResponse.UserDataResponse

class AppData private constructor() {
    var userToken: String? = null
    var userData: UserDataResponse? = null
    var transactions: List<Transaction>? = null
    var userLoans: List<LoanDataResponse>? = null

    companion object {
        private var instance: AppData? = null

        fun getInstance(): AppData {
            if (instance == null) {
                instance = AppData()
            }
            return instance!!
        }
    }
}