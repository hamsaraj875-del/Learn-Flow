//external modules

const { PDFParse } = require("pdf-parse");

//internal modules

const mcqTest = require("../models/mcqTest");
const questionTest = require("../models/questionTest");
const testAi = require("../services/testAi");
const testResultAi = require("../services/testResultAi");

exports.createMcqTest = async(req,res,next)=>{
    console.log("=== create test without document===");
    const data = req.body;
    console.log(data);
    try{
        const response = await testAi(data.topic,data.questionCount,"mcq");
        const testDetails = await mcqTest({userId:req.session.userId,topic:data.topic,totalQuestion:data.questionCount,correct:0,wrong:0,questions:response.questions});
        await testDetails.save();
        console.log(response.questions);
        return res.status(200).json({
            success:true,
            testId:testDetails._id,
            questionList:response.questions,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            testId:"",
            questionList:[],
        })
    }
}
 
exports.createQuestionTest = async(req,res,next)=>{
    console.log("=== create question test without document ===");
    const {userId,topic,questionCount} = req.body;

    try{
        if(userId==req.session.userId){
            const response = await testAi(topic,questionCount,"question_answer");
            const questions = response.questions.map(question=>({question,answer:"",marks:""}));
            const details = new questionTest({userId:req.session.userId,topic:topic,totalQuestions:questionCount,questions:questions});
            await details.save();
            console.log(questions);

            return res.status(200).json({
                success:true,
                testId:details._id,
                questionList:questions,
            })
        }
        else{
            return res.status(500).json({
                success:false,
                testId:"",
                questionList:[],
            })
        }
    }catch(err){
        console.log(err);
        return res.status.json({
            success:false,
            testId:"",
            questionList:[],
        })
    }
}


exports.createWrittenDocumentTest = async(req,res,next)=>{
    console.log("=== written document test ====");
    const {questionCount,topic,userId} = req.body;
    const pdf = req.file;
    try{
        const parser = new PDFParse({ data: pdf.buffer });
        const result = await parser.getText();
        const documentText = result.text;
        await parser.destroy();
        const response = await testAi(topic,questionCount,"question_answer",documentText);
        const questions = response.questions.map(question=>({question,answer:"",marks:0}));
        console.log(response);
        const testDetails = await questionTest({userId:req.session.userId,topic:topic,totalQuestions:questionCount,questions:questions});
        await testDetails.save();
        return res.status(200).json({
            success:true,
            testId:testDetails._id,
            questionList:questions,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            testId:"",
            questionList:[]
        })
    }
}

exports.questionTestResult = async(req,res,next)=>{
    console.log("=== written test result without document ===");
    const data = req.body;
    let correct = 0;
    let wrong = 0;
    try{
        const response = await testResultAi(data.questionList);
        console.log(response);
        let questionsList = [];
        let total = 0;
        for(let i=0;i<data.questionList.length;i++){
            questionsList.push({
                question:data.questionList[i].question,
                answer:data.questionList[i].answer,
                marks:response[i]
            });
            total+=response[i];
            if(response[i]==1) correct++;
            if(response[i]==0) wrong++;
        }
        console.log(correct,wrong);
        const updated = await questionTest.findOneAndUpdate(
            {_id:data.testId,userId:req.session.userId},
            {$set:{questions:questionsList,correct:correct,wrong:wrong}},
            {new:true}
        );
        if(!updated){
            console.log("not updated the test result");
            return res.status(404).json({
                success:false,
                total:0,
                correct:0,
                wrong:0
            });
        }
        return res.status(200).json({
            success:true,
            total:total,
            correct:correct,
            wrong:wrong,
        });
    }
    catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            total:0,
            correct:0,
            wrong:0,
        });
    }
}

exports.createMcqDocumentTest = async(req,res,next)=>{
    console.log("=== create test with document");
    const pdf = req.file;
    const { questionCount, topic, userId } = req.body;
    try{
        const parser = new PDFParse({ data: pdf.buffer });
        const result = await parser.getText();
        const documentText = result.text;
        await parser.destroy();
        const response = await testAi(topic,questionCount,"mcq",documentText);
        const testDetails = await mcqTest({userId:req.session.userId,topic:topic,totalQuestion:questionCount,correct:0,wrong:0,questions:response.questions});
        await testDetails.save();
        return res.status(200).json({
            success:true,
            testId:testDetails._id,
            questionList:response.questions,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            testId:"",
            questionList:[]
        })
    }
}


// test score saver

exports.testResultUpdate = async(req,res,next)=>{
    console.log("=== result update ===");
    const data = req.body;
    console.log(data);
    try{
        const updatedTest = await mcqTest.findOneAndUpdate(
            {
                _id: data.testId,
                userId: req.session.userId
            },
            {
                $set: {
                    correct: data.correct,
                    wrong: data.wrong
                }
            },
            { new: true }
        );
        if(updatedTest){
            console.log("updated successfully");
        }
        console.log("test data not found");
        return res.status(200).json({
            success:true,
            mes:"Test is not found !",
        })
    }
    catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            mes:"Test result not updated !",
        })
    }
}