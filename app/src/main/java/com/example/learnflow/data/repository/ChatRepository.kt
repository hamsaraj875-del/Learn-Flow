package com.example.learnflow.data.repository

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.DocumentModel
import com.example.learnflow.data.model.LoginResponse
import com.example.learnflow.data.remote.LearnFlowApi
import io.example.learnflow.data.model.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChatRepository(
    private val api: LearnFlowApi
) {

    suspend fun userIsLoggedIn(): LoginResponse {
        return api.userIsLoggedIn();
    }

    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return api.sendMessage(request)
    }

    suspend fun getMessages(conversationId: String): MessageResponse {
        return api.getMessages(conversationId)
    }

    suspend fun uploadDocument(pdf: MultipartBody.Part,question: RequestBody): ChatResponse {
        return api.uploadDocument(pdf, question)
    }
}