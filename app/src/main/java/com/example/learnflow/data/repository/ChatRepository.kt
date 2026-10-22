package com.example.learnflow.data.repository

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.model.NewChatRequest
import com.example.learnflow.data.model.NewChatResponse
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.SignUpRequest
import com.example.learnflow.data.model.userData
import com.example.learnflow.data.remote.LearnFlowApi
import io.example.learnflow.data.model.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChatRepository(
    private val api: LearnFlowApi
) {

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


    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return api.sendMessage(request)
    }

    suspend fun createNewChat(request: NewChatRequest): NewChatResponse {
        return api.createNewChat(request);
    }

    suspend fun getMessages(conversationId: String): MessageResponse {
        return api.getMessages(conversationId)
    }

    suspend fun uploadDocument(pdf: MultipartBody.Part,question: RequestBody): ChatResponse {
        return api.uploadDocument(pdf, question)
    }
}