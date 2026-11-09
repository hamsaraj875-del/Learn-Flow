//external modules

const fs = require("fs");
const { PDFParse } = require("pdf-parse");

//internal modules

const generateResponse = require("../services/openAi");


//ai response generator

exports.openAiResult = async (req, res, next) => {
    const question = req.body;
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
