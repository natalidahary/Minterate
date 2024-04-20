package com.example.myapplication.serverOperations

import com.example.myapplication.requestResponse.AddressUpdateRequest
import com.example.myapplication.requestResponse.ApiResponse
import com.example.myapplication.requestResponse.ApproveLoanByLockRequest
import com.example.myapplication.requestResponse.ApproveLoanByLockResponse
import com.example.myapplication.requestResponse.CreditCheckResponse
import com.example.myapplication.requestResponse.CreditUpdateRequest
import com.example.myapplication.requestResponse.CurrencyUpdateRequest
import com.example.myapplication.requestResponse.EmailCheckResponse
import com.example.myapplication.requestResponse.GetMobileByEmailResponse
import com.example.myapplication.requestResponse.IdCheckResponse
import com.example.myapplication.requestResponse.LoanDataResponse
import com.example.myapplication.requestResponse.LoanLockStatusResponse
import com.example.myapplication.requestResponse.LoanRepaymentRequest
import com.example.myapplication.requestResponse.LockLoanRequest
import com.example.myapplication.requestResponse.LockLoanResponse
import com.example.myapplication.requestResponse.LoginResponse
import com.example.myapplication.requestResponse.MobileCheckResponse
import com.example.myapplication.requestResponse.MobileUpdateRequest
import com.example.myapplication.requestResponse.PasswordUpdateRequest
import com.example.myapplication.requestResponse.RecordTransactionRequest
import com.example.myapplication.requestResponse.SaveLoanRequest
import com.example.myapplication.requestResponse.SoundSettingsUpdateRequest
import com.example.myapplication.requestResponse.TextScalarUpdateRequest
import com.example.myapplication.requestResponse.Transaction
import com.example.myapplication.requestResponse.UnlockLoanRequest
import com.example.myapplication.requestResponse.UnlockLoanResponse
import com.example.myapplication.requestResponse.UpdateAndAddLoanRequest
import com.example.myapplication.requestResponse.UpdatePasswordLoginRequest
import com.example.myapplication.requestResponse.UserDataResponse
import com.example.myapplication.requestResponse.GetILSToUserCurrencyResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query


interface RetrofitInterface {

    @POST("/login")
    fun executeLogin(@Body map: HashMap<String, String>): Call<LoginResponse>

    @POST("/signup")
    fun executeSignup(@Body map: HashMap<String, Any>): Call<Void>

    @POST("/check-email")
    fun checkEmailExists(@Body map: HashMap<String, String>): Call<EmailCheckResponse>

    @POST("/check-mobile")
    fun checkMobileExists(@Body map: HashMap<String, String>): Call<MobileCheckResponse>

    @POST("/check-id")
    fun checkIdExists(@Body map: HashMap<String, String>): Call<IdCheckResponse>

    @POST("/check-credit")
    fun checkCreditExists(@Body map: HashMap<String, String>): Call<CreditCheckResponse>

    @POST("/updateAndAddLoan")
    fun updateAndAddLoan(@Body request: UpdateAndAddLoanRequest): Call<ApiResponse>

    @POST("/lockLoan")
    suspend fun lockLoan(@Body requestBody: LockLoanRequest): Response<LockLoanResponse>

    @POST("/unlockLoan")
    suspend fun unlockLoan(@Body requestBody: UnlockLoanRequest): Response<UnlockLoanResponse>

    @POST("/approveLoanByLock")
    suspend fun approveLoanByLock(@Body request: ApproveLoanByLockRequest): Response<ApproveLoanByLockResponse>

    @GET("/getUserDataByToken")
    fun getUserDataByToken(@Query("token") userToken: String): Call<UserDataResponse>

    @GET("/getMobileForEmail")
    fun getMobileForEmail(@Query("email") email: String): Call<GetMobileByEmailResponse>

    @GET("/getLoans")
    suspend fun getLoans(@Query("amount") amount: String, @Query("token") userToken: String): Response<List<LoanDataResponse>>

    @GET("/getILSToUserCurrencyExchangeRate")
    fun getILSToUserCurrencyExchangeRate(@Query("token") userToken: String): Call<GetILSToUserCurrencyResponse>

    @GET("/getLoanLockStatus/{loanId}")
    suspend fun getLoanLockStatus(@Path("loanId") loanId: String): Response<LoanLockStatusResponse>

    @GET("/getUserTransactions")
    fun getUserTransactions(@Query("token") userToken: String): Call<List<Transaction>>

    @PUT("/updateUserAddress")
    fun updateUserAddress(@Query("token") userToken: String, @Body updateRequest: AddressUpdateRequest): Call<Void>

    @PUT("/updateUserCredit")
    fun updateUserCredit(@Query("token") userToken: String, @Body updateRequest:CreditUpdateRequest): Call<Void>

    @PUT("/updatePassword")
    fun updatePassword(@Query("token") userToken: String, @Body request: PasswordUpdateRequest): Call<Void>

    @PUT("updateMobile")
    fun updateMobile(@Query("token") userToken: String, @Body mobileUpdateRequest: MobileUpdateRequest): Call<Void>

    @PUT("/updateUserCurrency")
    fun updateUserCurrency(@Query("token") userToken: String, @Body updateRequest: CurrencyUpdateRequest): Call<Void>

    @PUT("/updateUserTextScalar")
    fun updateUserTextScalar(@Query("token") userToken: String, @Body updateRequest: TextScalarUpdateRequest): Call<Void>

    @PUT("/updateSoundSettings")
    fun updateSoundSettings(@Query("token") userToken: String, @Body updateRequest: SoundSettingsUpdateRequest): Call<Void>

    @PUT("/changePasswordLogin")
    fun changePasswordLogin(@Query("email") email: String, @Body updateRequest: UpdatePasswordLoginRequest): Call<Void>

    @POST("/saveLoan")
    fun saveLoan(@Body request: SaveLoanRequest): Call<ApiResponse>

    @GET("/getUserLoans")
    fun getUserLoans(@Query("token") userToken: String): Call<List<LoanDataResponse>>

    @DELETE("/deleteLoan")
    fun deleteLoan(@Query("userToken") userToken: String, @Query("loanId") loanId: String): Call<ApiResponse>

    @GET("/checkUserActiveLoans")
    fun checkUserActiveLoans(@Query("token") userToken: String): Call<ApiResponse>

    @DELETE("/deleteUser")
    fun deleteUser(@Query("token") userToken: String): Call<ApiResponse>

    @POST("/completeLoanRepayment")
    fun completeLoanRepayment(@Body request: LoanRepaymentRequest): Call<ApiResponse>

    @POST("/recordTransaction")
    fun recordTransaction(@Body request: RecordTransactionRequest): Call<ApiResponse>

    @GET("/getUserNameById")
    fun getUserNameById(@Query("userId") userId: String): Call<UserDataResponse>


}