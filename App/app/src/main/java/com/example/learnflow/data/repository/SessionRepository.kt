package com.example.learnflow.data.repository

import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.LearnFlowApi

class SessionRepository(private val api: LearnFlowApi) {

    suspend fun createSession(request: Session): Response {
        return api.createSession(request);
    }
}