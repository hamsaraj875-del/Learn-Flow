package com.example.learnflow.data.model
import java.io.Serializable
class WrittenTestQuestionList(
    var success:Boolean,
    var testId:String,
    var questionList:MutableList<WrittenTestQuestion>,
):Serializable;