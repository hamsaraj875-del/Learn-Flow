
require("dotenv").config();
const Groq = require("groq-sdk");
const { GoogleGenAI } = require("@google/genai");

const gemini = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});

const classificationInstruction = `
You are a question classifier for LearnFlow AI.

Classify the user's latest question into exactly one of these categories:

PDF:
- The user explicitly refers to the uploaded PDF or document.
- The user asks to summarize, explain, extract, or find information from the document.
- The user uses references such as "this PDF", "the document", "from the file", or "according to the PDF".
- The user asks a follow-up question that depends on information from the document.

GENERAL:
- The user asks a general knowledge question.
- The question does not require information from the uploaded document.
- The user asks for general explanations, programming help, definitions, or other unrelated information.

MIXED:
- The user wants information from the document along with additional explanations.
- The user asks to explain document content using external examples or additional general knowledge.
- The user wants to compare document information with general knowledge.

IMPORTANT:
- Consider conversation history to understand references such as "it", "that", and "this".
- Classify based on the user's latest question.
- Do not answer the question.
- Return ONLY one word: PDF, GENERAL, or MIXED.
`;

const classifyQuestion = async (question, conversationHistory = []) => {
    console.log("classifier is working");
    const messages = [
        ...conversationHistory,
        {
            role: "user",
            content: question
        }
    ];
    try {
        const response = await groq.chat.completions.create({
            model: "openai/gpt-oss-120b",
            messages: [
                {
                    role: "system",
                    content: classificationInstruction
                },
                ...messages
            ],
            temperature: 0
        });
        const result = response.choices[0].message.content
            .trim()
            .toUpperCase();
        if (["PDF", "GENERAL", "MIXED"].includes(result)) {
            return result;
        }
        throw new Error("Invalid classification response: " + result);
    } catch (error) {
        console.error("Groq Classifier Error:", error.message);
        try {
            console.log("Switching classifier to Gemini...");
            const response = await gemini.models.generateContent({
                model: "gemini-2.5-flash",
                contents: messages.map(msg => ({
                    role: msg.role === "assistant" ? "model" : "user",
                    parts: [{ text: msg.content }]
                })),
                config: {
                    systemInstruction: classificationInstruction,
                    temperature: 0
                }
            });
            const result = response.text.trim().toUpperCase();
            if (["PDF", "GENERAL", "MIXED"].includes(result)) {
                return result;
            }
            throw new Error("Invalid classification response: " + result)
        } catch (geminiError) {
            console.error("Gemini Classifier Error:", geminiError.message);
            throw new Error("Both Groq and Gemini classification failed");
        }
    }
};

module.exports = classifyQuestion;
