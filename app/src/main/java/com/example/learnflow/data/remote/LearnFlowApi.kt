package com.example.learnflow.data.remote

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import io.example.learnflow.data.model.MessageResponse
import okhttp3.MultipartBody
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LearnFlowApi {

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse

    @GET("api/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String
    ): MessageResponse

    @Multipart
    @POST("api/documents")
    suspend fun uploadDocument(
        @Part document: MultipartBody.Part
    ): ChatResponse
}