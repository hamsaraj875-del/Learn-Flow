package com.example.learnflow.data.remote

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import io.example.learnflow.data.model.MessageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LearnFlowApi {

    @POST("api/chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse

    @GET("api/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String
    ): MessageResponse
}