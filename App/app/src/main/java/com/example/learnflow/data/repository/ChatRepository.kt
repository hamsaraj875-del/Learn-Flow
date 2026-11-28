package com.example.learnflow.data.repository

import com.example.learnflow.data.model.ChatHistoryList
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.HistoryCheckup
import com.example.learnflow.data.model.MessageHistoryRequest
import com.example.learnflow.data.model.MessageList
import com.example.learnflow.data.model.NewChatRequest
import com.example.learnflow.data.model.NewChatResponse
import com.example.learnflow.data.remote.LearnFlowApi
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChatRepository(
    private val api: LearnFlowApi
) {

    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return api.sendMessage(request)
    }

    suspend fun getLastConversation(userId: HistoryCheckup?):NewChatResponse{
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

    suspend fun uploadDocument(
        pdf: MultipartBody.Part,
        question: RequestBody,
        conversationId: RequestBody
    ): ChatResponse {
        return api.uploadDocument(pdf, question,conversationId)
    }
}