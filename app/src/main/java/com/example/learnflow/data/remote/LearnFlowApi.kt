package com.example.learnflow.data.remote

import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.model.LoginResponse
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.model.SignUpRequest
import io.example.learnflow.data.model.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LearnFlowApi {

    @POST("user/login")
    suspend fun login(
        @Body request: LoginRequest
    ):LoginResponse;

    @POST("user/signUp")
    suspend fun signUp(
        @Body request: SignUpRequest
    ):LoginResponse

    @POST("user/otp")
    suspend fun otp(
        @Body request: OtpRequest
    ):LoginResponse

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse


    @POST("new_chat")
    suspend fun createNewChat(
        @Body userId:String
    ):ChatResponse


    @POST("user/loggedIn")
    suspend fun userIsLoggedIn(): LoginResponse;


    @GET("api/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String
    ): MessageResponse


    @Multipart
    @POST("document")
    suspend fun uploadDocument(
        @Part pdf: MultipartBody.Part,
        @Part("question") question: RequestBody
    ): ChatResponse
}