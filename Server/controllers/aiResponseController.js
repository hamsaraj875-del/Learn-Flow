//external modules

const fs = require("fs");
const { PDFParse } = require("pdf-parse");
const { RecursiveCharacterTextSplitter } = require("@langchain/textsplitters");

//internal modules

const generateResponse = require("../services/openAi");
const documents = require("../models/documents");
const textChunk = require("../models/textChunk");
const messages = require("../models/messages");
const classifier = require("../services/classifier");
const generateRagResponse = require("../services/ragAi");


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
        const type = await classifier(question.question,conversation.slice(0,-1));
        console.log(type);
        if (type == "PDF" || type == "MIXED") {
            const pdfs = await pdfFinder(
                req.session.userId,
                question.conversationId
            );
            if (pdfs.length === 0) {
                return res.status(200).json({
                    response: "Please upload a PDF in this conversation so I can answer your question from it."
                });
            }
            const queryEmbedding = await generateQueryEmbedding(question.question);
            const context = await chunkFinder(
                req.session.userId,
                pdfs,
                queryEmbedding
            );
            const prompt = [
                {
                    role: "user",
                    content: `
                        Document Context:
                        ${context}

                        Question:
                        ${question.question}
                    `
                }
            ];
            const answer = await generateRagResponse(prompt);
            const assistantMessage = new messages({
                conversationId: question.conversationId,
                userId: req.session.userId,
                role: "assistant",
                content: answer
            });
            await assistantMessage.save();
            return res.status(200).json({
                response: answer
            });
        }
        else{
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
        }
    } catch (error) {
        console.error("Open Ai API error:");
        console.error(error);
        res.status(500).json({
            error: "Internal Server Error"
        });
    }
};


//pdf finder in the conversation

const pdfFinder = async (userId, conversationId) => {
    try {
        const pdfs = await documents.find({
            userId: userId,
            conversationId: conversationId
        });
        return pdfs;
    } catch (err) {
        console.log(err);
        return [];
    }
};


//chunk finder in the database

const chunkFinder = async (userId, pdfs, queryEmbedding) => {
    try {
        const documentIds = pdfs.map(pdf => pdf._id);
        const chunks = await textChunk.find({
            userId: userId,
            documentId: { $in: documentIds }
        }).select("text embedding chunkIndex");
        const results = chunks.map(chunk => ({
            text: chunk.text,
            score: cosineSimilarity(
                queryEmbedding,
                chunk.embedding
            )
        }));
        results.sort((a, b) => b.score - a.score);
        const relevantChunks = results.slice(0, 5);
        return relevantChunks.map(chunk => chunk.text).join("\n\n");
    } catch (err) {
        console.log(err);
        return "";
    }
};

//user document handled responses

exports.documentOpenAiResult=async(req,res,next)=>{
    const pdf = req.file;
    const question = req.body.question;
    const conversationId = req.body.conversationId;

    if(!pdf){
        return res.status(500).json({
            response:"i didn't got any pdf please try again"
        })
    }

    const document = new documents({_id:req.documentId,userId:req.session.userId,conversationId:conversationId,fileName:pdf.originalname,filePath:req.filePath})
    await document.save();
    const pdfBuffer = pdf.buffer;
    console.log("===question is recieved with the document===");
    try{
        const pdfText = await extractPdfText(pdfBuffer);
        const chunks = await textChunker(pdfText);
        const pdfEmbeddings = await generateEmbeddings(chunks);
        const queryEmbeddings = await generateQueryEmbedding(question);

        const chunkDocuments = chunks.map((chunk, index) => ({
            documentId: req.documentId,
            userId: req.session.userId,
            chunkIndex: index,
            text: chunk,
            embedding: pdfEmbeddings[index]
        }));
        await textChunk.insertMany(chunkDocuments);

        const results = chunks.map((chunk, index) => ({
            text: chunk,
            score: cosineSimilarity(
                queryEmbeddings,
                pdfEmbeddings[index]
            )
        }));
        results.sort((a, b) => b.score - a.score);
        const relevantChunks = results.slice(0, 5);
        const context = relevantChunks.map(chunk => chunk.text).join("\n\n");

        const prompt = [
            {
                role: "user",
                content: `
                Document Context:
                ${context}

                Question:
                ${question}
            `
            }
        ];
        const answer = await generateRagResponse(prompt);

        const userMes = new messages({conversationId:conversationId,userId:req.session.userId,role:"user",content:question});
        await userMes.save();
        const aiMes = new messages({conversationId:conversationId,userId:req.session.userId,role:"assistant",content:answer});
        await aiMes.save();
        return res.status(200).json({
            response:answer
        })
    }catch(err){
        console.log(err);
    }
}

//similarity in the embeddings using consine similaryity algorithm

const cosineSimilarity = (vectorA, vectorB) => {
    let dotProduct = 0;
    let magnitudeA = 0;
    let magnitudeB = 0;

    for (let i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        magnitudeA += vectorA[i] * vectorA[i];
        magnitudeB += vectorB[i] * vectorB[i];
    }

    if (magnitudeA === 0 || magnitudeB === 0) {
        return 0;
    }

    return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
};




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





//Text to chunker convertor

const textChunker = async (text) => {
    const splitter = new RecursiveCharacterTextSplitter({
        chunkSize: 3000,
        chunkOverlap: 300,
        separators: ["\n\n", "\n", " ", ""]
    });

    const chunks = await splitter.splitText(text);
    return chunks;
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
