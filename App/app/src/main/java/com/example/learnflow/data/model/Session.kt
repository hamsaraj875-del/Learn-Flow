package com.example.learnflow.data.model

class Session(
    val userId: String,
    val sessionName: String,
    val startTime: Long,
    val endTime: Long?,
    val plannedDuration: Long,
    val actualDuration: Long,
    val status: String
)