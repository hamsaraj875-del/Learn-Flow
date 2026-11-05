package com.example.learnflow.data.repository

import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.SignUpRequest
import com.example.learnflow.data.model.userData
import com.example.learnflow.data.remote.LearnFlowApi

class LoginRepository(private val api: LearnFlowApi) {


    suspend fun userIsLoggedIn(): userData {
        return api.userIsLoggedIn();
    }

    suspend fun login(request: LoginRequest): userData {
        return api.login(request);
    }

    suspend fun signUp(request: SignUpRequest): Response {
        return api.signUp(request);
    }

    suspend fun otp(request: OtpRequest):userData{
        return api.otp(request);
    }

}