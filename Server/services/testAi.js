
require("dotenv").config();

const Groq = require("groq-sdk");
const { GoogleGenAI } = require("@google/genai");

const gemini = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});

const generateTest = async (
    topic,
    questionCount,
    questionType,
    documentText = ""
) => {
    const count = Number(questionCount);

    if (!topic || !topic.trim()) {
        throw new Error("Topic is required");
    }

    if (!Number.isInteger(count) || count < 1 || count > 50) {
        throw new Error("Question count must be between 1 and 50");
    }

    if (!["mcq", "question_answer"].includes(questionType)) {
        throw new Error("Invalid question type");
    }

    
    const testInstruction = `
    You are LearnFlow AI, an expert academic question paper generator.

    YOUR TASK:
    Generate exactly ${count} high-quality, meaningful, topic-specific questions.

    INPUT:
    TOPIC: ${topic}
    QUESTION COUNT: ${count}
    QUESTION TYPE: ${questionType}

    ${documentText ? `
    SOURCE RESTRICTION:
    Use the following document as the ONLY source of information:
    ${documentText}

    - Every question must be directly based on the document.
    - Do not introduce concepts that are not covered in the document.
    - Preserve the technical terminology used in the document.
    ` : `
    TOPIC KNOWLEDGE:
    - Use your knowledge of the exact topic provided.
    - Identify its actual concepts, principles, terminology, processes, applications and important subtopics.
    - Generate questions that test understanding of those specific concepts.
    - Do not invent vague concepts or assume that the topic contains a particular process.
    `}

    QUESTION QUALITY RULES:
    1. Every question MUST be directly related to the given topic.
    2. Use actual names of concepts, methods, formulas, components, algorithms or processes wherever relevant.
    3. Ask specific questions instead of generic questions.
    4. Test a mixture of knowledge, understanding, application, comparison and analysis where appropriate.
    5. Questions must be clear, meaningful and suitable for a student studying the topic.
    6. Avoid asking the same concept repeatedly.
    7. Each question must be independently understandable.
    8. Do not generate filler questions just to reach the requested count.
    9. Never use vague phrases such as:
    - "this topic"
    - "this subject"
    - "the main concept"
    - "the primary process"
    - "related phenomena"
    - "major components"
    unless those exact phrases are genuinely necessary in a specific question.
    10. Never use placeholders or refer to unspecified concepts.
    11. Do not make assumptions about the topic. Questions must be factually relevant.

    ${questionType === "mcq" ? `
    OUTPUT FORMAT:
    Return ONLY valid JSON in this exact structure:

    {
    "questions": [
        {
        "type": "mcq",
        "question": "A specific question about an actual topic concept?",
        "options": [
            "Option A",
            "Option B",
            "Option C",
            "Option D"
        ],
        "correctAnswer": "Exact correct option"
        }
    ]
    }

    MCQ RULES:
    - Every question must have exactly four options.
    - Only one option must be correct.
    - All options must be relevant and plausible.
    - Avoid obviously incorrect or unrelated options.
    - correctAnswer must exactly match one of the four options.
    ` : `
    OUTPUT FORMAT:
    Return ONLY valid JSON in this exact structure:

    {
    "questions": [
        "A specific question about an actual topic concept?",
        "Another specific question about a different concept?"
    ]
    }

    QUESTION AND ANSWER RULES:
    - Return only question strings.
    - Do not generate answers.
    - Do not include question numbers.
    - Do not include objects inside the questions array.
    `}

    FINAL VALIDATION:
    Before returning the JSON, verify that:
    - Exactly ${count} questions are generated.
    - Every question is specific to "${topic}".
    - No generic filler or placeholder questions are present.
    - Questions are not duplicated or repeated in meaning.
    - All questions are grammatically clear.
    - The output follows the exact requested JSON structure.
    - The JSON is valid and contains no additional text.

    Return ONLY the JSON object.
    `;


    const prompt = testInstruction;

    // Validate the generated response
    const validateResponse = (content, provider) => {
        let result;

        try {
            result = JSON.parse(content);
        } catch (error) {
            throw new Error(`${provider} returned invalid JSON`);
        }

        if (!result || !Array.isArray(result.questions)) {
            throw new Error(
                `${provider} response does not contain a questions array`
            );
        }

        if (result.questions.length !== count) {
            throw new Error(
                `${provider} returned ${result.questions.length} questions instead of ${count}`
            );
        }

        for (const question of result.questions) {
            if (questionType === "mcq") {
                if (
                    !question ||
                    !question.question ||
                    !question.correctAnswer ||
                    question.type !== "mcq" ||
                    !Array.isArray(question.options) ||
                    question.options.length !== 4 ||
                    !question.options.every(option =>
                        typeof option === "string" && option.trim()
                    ) ||
                    !question.options.includes(question.correctAnswer)
                ) {
                    throw new Error(
                        `${provider} returned an invalid MCQ structure`
                    );
                }
            } else {
                if (
                    typeof question !== "string" ||
                    !question.trim()
                ) {
                    throw new Error(
                        `${provider} returned an invalid question structure`
                    );
                }
            }
        }

        return result;
    };

    // Try Groq first
    try {
        if (!process.env.GROQ_API_KEY) {
            throw new Error("GROQ_API_KEY is missing");
        }

        const response = await groq.chat.completions.create({
            model: "openai/gpt-oss-120b",
            messages: [
                {
                    role: "system",
                    content: "You are LearnFlow AI. Generate complete tests and return only valid JSON."
                },
                {
                    role: "user",
                    content: prompt
                }
            ],
            response_format: {
                type: "json_object"
            },
            temperature: 0.7
        });

        const rawResponse = response.choices?.[0]?.message?.content;

        if (!rawResponse) {
            throw new Error("Groq returned an empty response");
        }

        return validateResponse(rawResponse, "Groq");

    } catch (groqError) {
        console.error("Groq API Error:", groqError.message);
    }

    // If Groq fails, try Gemini
    try {
        if (!process.env.GEMINI_API_KEY) {
            throw new Error("GEMINI_API_KEY is missing");
        }

        const response = await gemini.models.generateContent({
            model: "gemini-2.5-flash",
            contents: prompt,
            config: {
                responseMimeType: "application/json",
                temperature: 0.7
            }
        });

        if (!response.text) {
            throw new Error("Gemini returned an empty response");
        }

        return validateResponse(response.text, "Gemini");

    } catch (geminiError) {
        console.error("Gemini API Error:", geminiError.message);

        throw new Error(
            `Test generation failed. Groq and Gemini could not generate a valid test. Gemini error: ${geminiError.message}`
        );
    }
};

module.exports = generateTest;
