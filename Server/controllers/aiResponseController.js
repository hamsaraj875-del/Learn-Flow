//external modules

const fs = require("fs");
const { PDFParse } = require("pdf-parse");
const { RecursiveCharacterTextSplitter } = require("@langchain/textsplitters");

//internal modules

const generateResponse = require("../services/openAi");
const messages = require("../models/messages");


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


const tester = async()=>{
    const pdfBuffer = fs.readFileSync("./uploads/test.pdf")
    const parser = new PDFParse({
        data: new Uint8Array(pdfBuffer)
    });
    try {
        const result = await parser.getText();
        const text = result.text;
        await textChunker(text);
    } catch (error) {
        console.error(error);
    } finally {
        await parser.destroy();
    }
}
tester();


//Text to chunker convertor

const textChunker = async (text) => {
    const splitter = new RecursiveCharacterTextSplitter({
        chunkSize: 3000,
        chunkOverlap: 300,
        separators: ["\n\n", "\n", " ", ""]
    });

    const chunks = await splitter.splitText(text);
    const embeddings = await generateEmbeddings(chunks);
};

//embbeding generation by gemini api

const embedText = async (text, taskType) => {
    const apiKey = process.env.GEMINI_API_KEY;
    const response = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent",
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "x-goog-api-key": apiKey
            },
            body: JSON.stringify({
                content: {
                    parts: [{ text }]
                },
                taskType
            })
        }
    );
    const data = await response.json();
    if (!response.ok) {
        throw new Error(
            data.error?.message || "Embedding generation failed"
        );
    }
    return data.embedding.values;
};



//embedding generator by passing chunks to gemini

const generateEmbeddings = async (chunks) => {
    const embeddings = [];
    for (let i = 0; i < chunks.length; i++) {
        const vector = await embedText(
            chunks[i],
            "RETRIEVAL_DOCUMENT"
        );
        embeddings.push(vector);
        console.log(`Generated embedding ${i + 1}/${chunks.length}`);
    }
    return embeddings;
};


const generateQueryEmbedding = async (question) => {
    return await embedText(question, "RETRIEVAL_QUERY");
};
