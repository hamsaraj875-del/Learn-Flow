package com.example.learnflow.data.model
import java.io.Serializable
class TestQuestion(
    var type:String,
    var question:String,
    var options:Array<String>,
    var correctAnswer:String,
):Serializable;