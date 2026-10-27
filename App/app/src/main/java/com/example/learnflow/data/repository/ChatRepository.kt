package com.example.learnflow.data.repository

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
import com.example.learnflow.data.remote.LearnFlowApi
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

    suspend fun getLastConversation(userId: String?):NewChatResponse{
        return api.getLastConversation(userId);
    }

    suspend fun createNewChat(userId: NewChatRequest): NewChatResponse {
        return api.createNewChat(userId);
    }

    suspend fun chatList(): ChatHistoryList {
        return api.chatList();
    }

    suspend fun getMessages(request: MessageHistoryRequest): MessageList {
        return api.getMessages(request)
    }

    suspend fun uploadDocument(pdf: MultipartBody.Part,question: RequestBody): ChatResponse {
        return api.uploadDocument(pdf, question)
    }
}