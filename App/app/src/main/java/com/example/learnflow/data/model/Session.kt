package com.example.learnflow.data.model

class Session(
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val plannedDuration: Long,
    val actualDuration: Long,
    val status: String
)