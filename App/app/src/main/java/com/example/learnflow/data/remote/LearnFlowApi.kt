package com.example.learnflow.data.remote

import com.example.learnflow.data.model.ChatHistoryList
import com.example.learnflow.data.model.ChatRequest
import com.example.learnflow.data.model.ChatResponse
import com.example.learnflow.data.model.HistoryCheckup
import com.example.learnflow.data.model.LoginRequest
import com.example.learnflow.data.model.MessageHistoryRequest
import com.example.learnflow.data.model.MessageList
import com.example.learnflow.data.model.NewChatRequest
import com.example.learnflow.data.model.NewChatResponse
import com.example.learnflow.data.model.OtpRequest
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Response
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.model.SessionList
import com.example.learnflow.data.model.SignUpRequest
import com.example.learnflow.data.model.TestCreate
import com.example.learnflow.data.model.McqTestQuestionList
import com.example.learnflow.data.model.WrittenTestQuestionList
import com.example.learnflow.data.model.WrittenTestResultModel
import com.example.learnflow.data.model.TestResultSender
import com.example.learnflow.data.model.UserAnalysis
import com.example.learnflow.data.model.userData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface LearnFlowApi {

    @POST("user/login")
    suspend fun login(
        @Body request: LoginRequest
    ): userData

    @POST("user/signUp")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response

    @POST("user/otp")
    suspend fun otp(
        @Body request: OtpRequest
    ): userData

    suspend fun resendOtp():userData;

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse

    @POST("new_chat")
    suspend fun createNewChat(
        @Body request: NewChatRequest
    ): NewChatResponse

    @POST("chatlist")
    suspend fun chatList(): ChatHistoryList;

    @POST("last/conversation/messages")
    suspend fun getLastConversation(
        @Body userId: HistoryCheckup?
    ):NewChatResponse


    @POST("createSession")
    suspend fun createSession(
        @Body request: Session
    ) : Response;

    @POST("sessionList")
    suspend fun sessionList(
        @Body request: Request
    ): SessionList;

    @POST("updateSession")
    suspend fun updateSession(
        @Body request:Session
    ): Response;

    @POST("userAnalysis")
    suspend fun userAnalysis(
        @Body request:Request
    ): UserAnalysis

    @POST("user/loggedIn")
    suspend fun userIsLoggedIn(): userData

    @POST("conversation/messages")
    suspend fun getMessages(
        @Body request: MessageHistoryRequest
    ): MessageList

    @POST("test/mcq")
    suspend fun createMcqTest(
        @Body request: TestCreate
    ): McqTestQuestionList;

    @POST("test/mcq/result")
    suspend fun testResult(
        @Body request: TestResultSender
    ):Response;

    @POST("test/question")
    suspend fun createQuestionTest(
        @Body request:TestCreate
    ): WrittenTestQuestionList;

    @POST("test/question/result")
    suspend fun questionTestResult(
        @Body request:WrittenTestQuestionList
    ): WrittenTestResultModel;

    @Multipart
    @POST("test/mcq/document")
    suspend fun createMcqDocumentTest(
        @Part pdf:MultipartBody.Part,
        @Part("questionCount") questionCount:RequestBody,
        @Part("topic") topic:RequestBody,
        @Part("userId") userId:RequestBody,
    ):McqTestQuestionList;

    @Multipart
    @POST("test/written/document")
    suspend fun createWrittenDocumentTest(
        @Part pdf:MultipartBody.Part,
        @Part("questionCount") questionCount:RequestBody,
        @Part("topic") topic:RequestBody,
        @Part("userId") userId:RequestBody,
    ):WrittenTestQuestionList;

    @Multipart
    @POST("document")
    suspend fun uploadDocument(
        @Part pdf: MultipartBody.Part,
        @Part("question") question: RequestBody,
        @Part("conversationId") conversationId: RequestBody,
    ): ChatResponse
}