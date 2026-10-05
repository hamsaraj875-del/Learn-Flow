package com.example.learnflow.data.repository

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.remote.LearnFlowApi
import io.example.learnflow.data.model.MessageResponse

class ChatRepository(
    private val api: LearnFlowApi
) {

    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return api.sendMessage(request)
    }

    suspend fun getMessages(conversationId: String): MessageResponse {
        return api.getMessages(conversationId)
    }
}