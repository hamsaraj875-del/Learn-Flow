
require("dotenv").config();

const Groq = require("groq-sdk");
const { GoogleGenAI } = require("@google/genai");

const gemini = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});

const evaluateTest = async (questionAnswers) => {
    if (!Array.isArray(questionAnswers) || questionAnswers.length === 0) {
        throw new Error("Question answers must be a non-empty array");
    }

    if (questionAnswers.length > 50) {
        throw new Error("Maximum 50 questions allowed");
    }

    for (const item of questionAnswers) {
        if (
            !item ||
            typeof item.question !== "string" ||
            typeof item.answer !== "string" ||
            !item.question.trim()
        ) {
            throw new Error("Invalid question or answer format");
        }
    }

    const prompt = `
    You are LearnFlow AI, an accurate academic answer evaluator.

    YOUR TASK:
    Evaluate the student's answer to each question and assign marks.

    QUESTIONS AND STUDENT ANSWERS:
    ${JSON.stringify(questionAnswers)}

    EVALUATION RULES:
    1. Evaluate every answer based on its corresponding question.
    2. Each question carries a maximum of 1 mark.
    3. Fully correct answer: 1.
    4. Completely incorrect or irrelevant answer: 0.
    5. Partially correct answer: assign a decimal mark between 0 and 1.
    6. Award partial marks when the answer demonstrates some correct understanding.
    7. Accept answers with different wording if their meaning is correct.
    8. Ignore minor spelling or grammatical mistakes if the meaning is clear.
    9. Do not award marks for answers that are merely related to the question but do not answer it.
    10. An empty answer must receive 0.
    11. Evaluate each answer independently.
    12. Do not penalize a student for using a concise answer if it correctly answers the question.
    13. Do not give marks based on answer length.
    14. Never award more than 1 mark for any question.
    15. Maintain the exact order of the provided questions.

    OUTPUT REQUIREMENTS:
    - Return ONLY a JSON object in this exact structure:
      {"marks":[1,0.5,0,0.75,1]}
    - The marks array must contain exactly ${questionAnswers.length} elements.
    - Every element must be between 0 and 1.
    - Use decimal values where partial marks are appropriate.
    - Do not return explanations or additional text.

    Return ONLY the JSON object.
    `;

    const validateResponse = (content, provider) => {
        let result;

        try {
            result = JSON.parse(content);
        } catch (error) {
            throw new Error(`${provider} returned invalid JSON`);
        }

        const marks = result?.marks;

        if (
            !Array.isArray(marks) ||
            marks.length !== questionAnswers.length ||
            !marks.every(mark =>
                typeof mark === "number" &&
                Number.isFinite(mark) &&
                mark >= 0 &&
                mark <= 1
            )
        ) {
            throw new Error(`${provider} returned invalid marks`);
        }

        return marks;
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
                    content: "You are an academic answer evaluator. Return only a JSON object containing a marks array."
                },
                {
                    role: "user",
                    content: prompt
                }
            ],
            response_format: {
                type: "json_object"
            },
            temperature: 0.2
        });

        const rawResponse = response.choices?.[0]?.message?.content;

        if (!rawResponse) {
            throw new Error("Groq returned an empty response");
        }

        return validateResponse(rawResponse, "Groq");

    } catch (groqError) {
        console.error("Groq Evaluation Error:", groqError.message);
    }

    // If Groq fails, try Gemini
    try {
        if (!process.env.GEMINI_API_KEY) {
            throw new Error("GEMINI_API_KEY is missing");
        }

        const response = await gemini.models.generateContent({
            model: "gemini-3.8-flash",
            contents: prompt,
            config: {
                responseMimeType: "application/json",
                temperature: 0.2
            }
        });

        if (!response.text) {
            throw new Error("Gemini returned an empty response");
        }

        return validateResponse(response.text, "Gemini");

    } catch (geminiError) {
        console.error("Gemini Evaluation Error:", geminiError.message);

        throw new Error(
            `Answer evaluation failed. Groq and Gemini could not evaluate the test. Gemini error: ${geminiError.message}`
        );
    }
};

module.exports = evaluateTest;
