//external modules

const pdfParse = require("pdf-parse");


//internal modules

const testAi = require("../services/testAi");

exports.createTest = async(req,res,next)=>{
    console.log("=== create test without document===");
    const data = req.body;
    try{
        const response = await testAi(data.topic,data.questionCount,"mcq");
        console.log(response.questions);
        return res.status(200).json({
            success:true,
            questionList:response.questions,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            questionList:[],
        })
    }
}

exports.createDocumentTest = async(req,res,next)=>{
    console.log("=== create test with document");
    const pdf = req.file;
    const data = req.body;

    try{
        const documentText = (await pdfParse(pdf.buffer)).text;
        const response = await testAi(data.topic,data.questionCount,"mcq",documentText);
        return res.status(200).json({
            success:true,
            questionList:response,
        })
    }catch(err){
        return res.status(500).json({
            success:false,
            questionList:[]
        })
    }
}