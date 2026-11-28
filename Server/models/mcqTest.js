const mongoose = require("mongoose");

const schema = mongoose.Schema({
    userId: {type: String,required: true},
    topic: {type: String,required: true},
    totalQuestions: Number,
    correct: Number,
    wrong: Number,
    questions: [{
        question: String,
        options: [String],
        correctAnswer: String,
        status: {
            type: String,
            enum: ["correct", "wrong", "unanswered"]
        }
    }],
})

module.exports = mongoose.model("mcqTests",schema);