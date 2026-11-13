
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
You are LearnFlow AI, an AI test generator.

Generate exactly ${count} questions.

TOPIC: ${topic}
QUESTION COUNT: ${count}
QUESTION TYPE: ${questionType}

${documentText ? `
DOCUMENT CONTENT:
${documentText}

DOCUMENT RULES:
- Generate questions using ONLY the provided document.
- Do not use outside knowledge.
- Questions must be relevant to the topic and document.
` : `
TOPIC RULES:
- Generate questions using your knowledge of the topic.
- Cover important concepts.
`}

GENERAL RULES:
- Generate exactly ${count} questions.
- Never return an empty questions array.
- Avoid duplicate questions.
- Use clear student-level language.
- Do not include introductory text or explanations outside the JSON.
- Every question must have a correct answer.
- Return ONLY valid JSON.

${questionType === "mcq" ? `
MCQ FORMAT:
{
    "questions": [
        {
            "type": "mcq",
            "question": "Question text",
            "options": [
                "Option A",
                "Option B",
                "Option C",
                "Option D"
            ],
            "correctAnswer": "Exact correct option",
        }
    ]
}

MCQ RULES:
- Every question must have exactly four options.
- Only one option must be correct.
- correctAnswer must exactly match one of the options.
` : `
QUESTION AND ANSWER FORMAT:
{
    "questions": [
        {
            "type": "question_answer",
            "question": "Question text",
            "correctAnswer": "Expected answer",
        }
    ]
}
`}

Before returning, verify:
- The questions array contains exactly ${count} objects.
- Every object follows the required structure.
- No question is duplicated.
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
            throw new Error(`${provider} response does not contain a questions array`);
        }
        if (result.questions.length !== count) {
            throw new Error(
                `${provider} returned ${result.questions.length} questions instead of ${count}`
            );
        }
        for (const question of result.questions) {
            if (
                !question.question ||
                !question.correctAnswer ||
                question.type !== questionType 
            ) {
                throw new Error(`${provider} returned an invalid question structure`);
            }
            if (
                questionType === "mcq" &&
                (
                    !Array.isArray(question.options) ||
                    question.options.length !== 4 ||
                    !question.options.includes(question.correctAnswer)
                )
            ) {
                throw new Error(`${provider} returned an invalid MCQ structure`);
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
