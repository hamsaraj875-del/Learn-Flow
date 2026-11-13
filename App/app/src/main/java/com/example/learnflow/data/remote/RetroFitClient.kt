
package com.example.learnflow.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL ="https://learn-flow-4nqd.onrender.com/"

    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (!initialized) {
            SessionCookieJar.initialize(context.applicationContext)
            initialized = true
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(SessionCookieJar)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: LearnFlowApi by lazy {
        check(initialized) {
            "Call RetrofitClient.initialize(context) before using api"
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LearnFlowApi::class.java)
    }
}
