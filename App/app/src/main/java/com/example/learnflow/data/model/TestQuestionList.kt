package com.example.learnflow.data.model
import java.io.Serializable
class TestQuestionList (
    var success:Boolean,
    var questionList:MutableList<TestQuestion>,
):Serializable;