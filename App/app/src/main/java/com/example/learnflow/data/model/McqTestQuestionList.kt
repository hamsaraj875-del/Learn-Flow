package com.example.learnflow.data.model
import java.io.Serializable
class McqTestQuestionList (
    var success:Boolean,
    var testId:String,
    var questionList:MutableList<McqTestQuestion>,
):Serializable;