package com.example.learnflow

import android.app.Application
import com.example.learnflow.data.remote.RetrofitClient

class LearnFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}