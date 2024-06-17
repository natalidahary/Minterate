package com.example.myapplication.requestResponse

data class StoreActiveSessionRequest(
   val email: String,
   val token: String
)