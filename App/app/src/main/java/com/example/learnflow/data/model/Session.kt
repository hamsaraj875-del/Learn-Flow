package com.example.learnflow.data.model

class Session(
    val id:String,
    val userId: String,
    val sessionName: String,
    val startTime: Double,
    val endTime: Long?,
    val plannedDuration: Long,
    val actualDuration: Long,
    val status: String
)