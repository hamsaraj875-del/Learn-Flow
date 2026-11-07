package com.example.learnflow.data.repository

import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.model.SessionList
import com.example.learnflow.data.remote.LearnFlowApi

class SessionRepository(private val api: LearnFlowApi) {

    suspend fun createSession(request: Session): Response {
        return api.createSession(request);
    }

    suspend fun sessionList(request: Request): SessionList {
        return api.sessionList(request);
    }

    suspend fun updateSession(request:Session):Response{
        return api.updateSession(request);
    }
}