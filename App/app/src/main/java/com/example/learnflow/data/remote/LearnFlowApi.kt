package com.example.learnflow.data.remote

import com.example.learnflow.data.model.ChatHistoryList
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.model.MessageHistoryRequest
import com.example.learnflow.data.model.MessageList
import com.example.learnflow.data.model.NewChatRequest
import com.example.learnflow.data.model.NewChatResponse
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.SignUpRequest
import com.example.learnflow.data.model.userData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface LearnFlowApi {

    @POST("user/login")
    suspend fun login(
        @Body request: LoginRequest
    ): userData

    @POST("user/signUp")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response

    @POST("user/otp")
    suspend fun otp(
        @Body request: OtpRequest
    ): userData

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse

    @POST("new_chat")
    suspend fun createNewChat(
        @Body request: NewChatRequest
    ): NewChatResponse

    @POST("chatlist")
    suspend fun chatList(): ChatHistoryList;

    @POST("last/conversation/messages")
    suspend fun getLastConversation(
        @Body userId: String?
    ):NewChatResponse

    @POST("user/loggedIn")
    suspend fun userIsLoggedIn(): userData

    @POST("conversation/messages")
    suspend fun getMessages(
        @Body request: MessageHistoryRequest
    ): MessageList

    @Multipart
    @POST("document")
    suspend fun uploadDocument(
        @Part pdf: MultipartBody.Part,
        @Part("question") question: RequestBody
    ): ChatResponse
}