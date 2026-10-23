//external modules

const { PDFParse } = require("pdf-parse");
const generateResponse = require("../services/openAi");
const fs = require("fs");


//internal modules

const messages = require("../models/messages");
const database = require("../models/database");
const conversation = require("../models/conversation");


//asynchronous support functions

exports.openAiResult = async (req, res, next) => {
    const question = req.body;
    console.log(question);
    console.log("hit guys");
    try {
        const userMessage = new messages({
            conversationId:question.conversationId,
            userId:req.session.userId,
            role: "user",
            content: question.question
        });
        await userMessage.save();
        console.log("====user question saved=====")

        const ai_question = await messages
            .find({conversationId:question.conversationId,userId:req.session.userId})
            .sort({ _id: -1 })
            .limit(5)
            .select("role content -_id")
            .lean();

        ai_question.reverse();

        const conversation = ai_question.map(message => ({
            role: message.role,
            content: String(message.content)
        }));
        console.log("===conversation===");
        console.log(conversation);
        const response = await generateResponse(conversation);

        const assistantMessage = new messages({
            conversationId:question.conversationId,
            userId:req.session.userId,
            role: "assistant",
            content: response
        });
        await assistantMessage.save();
        console.log("====ai response saved====");
        res.status(200).json({
            response: response
        });
    } catch (error) {
        console.error("Open Ai API error:");
        console.error(error);
        res.status(500).json({
            error: "Internal Server Error"
        });
    }
};


//creating new chats

exports.createNewChat=async(req,res,next)=>{
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        console.log(user);
        if(user){
            const lastChat = await conversation.findOne({userId:user._id}).sort({_id:-1});
            if (lastChat) {
                const chatDetails = await messages.findOne({conversationId: lastChat._id});
                if (chatDetails) {
                    const newChat = new conversation({ userId: user._id });
                    const newIdCreation = await newChat.save();
                    return res.status(200).json({
                        success: true,
                        conversationId: newIdCreation._id
                    });
                }

                return res.status(200).json({
                    success: true,
                    conversationId: lastChat._id
                });
            }
            const newChat = new conversation({userId:user._id});
            const newIdCreation = await newChat.save();
            return res.status(200).json({
                success:true,
                conversationId:newIdCreation._id
            })
        }
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            conversationId:"",
        })
    }

}


exports.chatList = async(req,res,next)=>{
    try{
        const list = await conversation.find({userId:req.session.userId}).select("_id title userId");
        const chatList = list.map(chat => ({
            title: chat.title,
            userId: chat.userId,
            conversationId: chat._id.toString()
        }));
        console.log(chatList);
        console.log("hit guys")
        return res.status(200).json({
            success:true,
            chatList:chatList,
        })
    }catch(err){
        console.log(err);
        return res.status(200).json({
            success:false,
            chatList:[]
        })
    }
}


//user document handled responses

exports.documentOpenAiResult=async(req,res,next)=>{
    const pdf = req.file;
    const question = req.body.question
    console.log("===question is recieved with the document===");
    console.log(question);
    const pdfBuffer = pdf.buffer;
    try{
        const text = await extractPdfText(pdfBuffer);
        console.log(text)
        return res.status(200).json({
            response:"I have got the pdf what's next?,should i scan it?"
        })
    }catch(err){
        console.log(err);
    }
}



//extraction of plain text in the document

const extractPdfText = async (pdfBuffer) => {
    const parser = new PDFParse({
        data: pdfBuffer
    });
    try {
        const result = await parser.getText();

        if (!result.text || !result.text.trim()) {
            throw new Error(
                "No text found. This PDF may be scanned or image-based."
            );
        }
        return result.text;
    } catch (err) {
        console.error("PDF extraction failed:", err);
        throw err;
    } finally {
        await parser.destroy();
    }
};




async function test() {
    const pdfBuffer = fs.readFileSync("./uploads/test.pdf");

    console.log("Is buffer:", Buffer.isBuffer(pdfBuffer));
    console.log("Size:", pdfBuffer.length);
    console.log("Header:", pdfBuffer.subarray(0, 5).toString());

    const parser = new PDFParse({
        data: new Uint8Array(pdfBuffer)
    });

    try {
        const result = await parser.getText();
        console.log(result.text);
    } catch (err) {
        console.error("Parsing error:", err);
    } finally {
        await parser.destroy();
    }
}



