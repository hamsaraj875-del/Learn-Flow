package com.example.learnflow.data.repository
import com.example.learnflow.data.model.TestCreate
import com.example.learnflow.data.model.TestQuestionList
import com.example.learnflow.data.remote.LearnFlowApi
import okhttp3.MultipartBody

class TestRepository(
    private val api: LearnFlowApi
) {
    suspend fun createTest(request: TestCreate): TestQuestionList {
        return api.createTest(request);
    }

    suspend fun createDocumentTest(pdf: MultipartBody.Part, request: TestCreate):TestQuestionList{
        return api.createDocumentTest(pdf,request);
    }
}