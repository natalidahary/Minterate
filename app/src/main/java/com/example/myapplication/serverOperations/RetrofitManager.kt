package com.example.myapplication.serverOperations

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitManager {

    private var retrofit: Retrofit? = null
    private var retrofitInterface: RetrofitInterface? = null

    fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        return retrofit!!
    }

    fun getRetrofitInterface(): RetrofitInterface {
        if (retrofitInterface == null) {
            retrofitInterface = getRetrofit().create(RetrofitInterface::class.java)
        }
        return retrofitInterface!!
    }
}
/*
object RetrofitManager {

    private var retrofit: Retrofit? = null
    private var retrofitInterface: RetrofitInterface? = null

    fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getRetrofitInterface(): RetrofitInterface {
        if (retrofitInterface == null) {
            retrofitInterface = getRetrofit().create(RetrofitInterface::class.java)
        }
        return retrofitInterface!!
    }

}
*/
