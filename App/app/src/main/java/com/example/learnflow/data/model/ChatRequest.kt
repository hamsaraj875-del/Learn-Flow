package com.example.learnflow.data.model

data class ChatRequest (
    val userId: String?,
    val conversationId: String,
    val question: String,
)