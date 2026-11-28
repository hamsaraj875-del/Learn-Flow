package com.example.learnflow.data.repository
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.TestCreate
import com.example.learnflow.data.model.McqTestQuestionList
import com.example.learnflow.data.model.WrittenTestQuestionList
import com.example.learnflow.data.model.WrittenTestResultModel
import com.example.learnflow.data.model.TestResultSender
import com.example.learnflow.data.remote.LearnFlowApi
import okhttp3.MultipartBody
import okhttp3.RequestBody

class TestRepository(
    private val api: LearnFlowApi
) {
    suspend fun createMcqTest(request: TestCreate): McqTestQuestionList {
        return api.createMcqTest(request);
    }

    suspend fun createMcqDocumentTest(pdf: MultipartBody.Part, questionCount:RequestBody,topic:RequestBody,userId:RequestBody):McqTestQuestionList{
        return api.createMcqDocumentTest(pdf,questionCount,topic,userId);
    }

    suspend fun createQuestionTest(request:TestCreate): WrittenTestQuestionList {
        return api.createQuestionTest(request);
    }

    suspend fun createWrittenDocumentTest(pdf:MultipartBody.Part,questionCount:RequestBody,topic:RequestBody,userId:RequestBody):WrittenTestQuestionList{
        return api.createWrittenDocumentTest(pdf,questionCount,topic,userId);
    }

    suspend fun writtenTestResult(request:WrittenTestQuestionList): WrittenTestResultModel {
        return api.questionTestResult(request);
    }

    suspend fun testResult(request: TestResultSender): Response {
        return api.testResult(request);
    }

}