//external modules

const express = require("express");
const app = express();
const mongoose = require("mongoose");
const multer = require("multer");
const session = require("express-session");
const loginRouter = require("./login")
const MongoStore = require("connect-mongo").default;
const { createClient } = require("@supabase/supabase-js");
const crypto = require("crypto");
const supabase = require("../services/supabase");



//internal modules

const controller  = require("../controllers/controller");
const aiResponseController = require("../controllers/aiResponseController");
const testController = require("../controllers/testController");
const PORT = process.env.PORT || 3000;
const db = process.env.DB


//session handling

const store = new MongoStore({
    mongoUrl: db,
    collectionName: "sessions"
});

app.use(session({
    secret: process.env.SECRET_KEY,
    resave: false,
    saveUninitialized: false,
    store: store,
    cookie: {
        httpOnly: true,
        maxAge: 1000 * 60 * 60*24*10,
        rolling: true
    }
}));



//file handling in the uploads folder 
//file handling

const storage = multer.memoryStorage();

const upload = multer({
    storage: storage,
    limits: {
        fileSize: 20 * 1024 * 1024
    },
    fileFilter: (req, file, cb) => {
        if (file.mimetype === "application/pdf") {
            cb(null, true);
        } else {
            cb(new Error("Only PDF files are allowed"));
        }
    }
});

//generate document id

const generateDocumentId = (req, res, next) => {
    req.documentId = new mongoose.Types.ObjectId();
    next();
};

//upload to supabase
const uploadToSupabase = async (req, res, next) => {
    try {
        const filePath = `${req.documentId}.pdf`;

        const { data, error } = await supabase.storage
            .from(process.env.SUPABASE_BUCKET)
            .upload(filePath, req.file.buffer, {
                contentType: "application/pdf",
                upsert: false
            });

        if (error) {
            throw error;
        }

        req.filePath = data.path;

        next();

    } catch (error) {
        console.log("Supabase upload error:", error);

        return res.status(500).json({
            success: false,
            message: "PDF upload to Supabase failed"
        });
    }
};

//configuations

app.use(express.json());
require("dotenv").config();


//routers

app.use("/user",loginRouter);


//main routes handling

app.post("/test",testController.createTest);
app.post("/chat",aiResponseController.openAiResult);
app.post("/userAnalysis",controller.userAnalysis);
app.post("/document",generateDocumentId,upload.single("pdf"),uploadToSupabase,aiResponseController.documentOpenAiResult);
app.post("/new_chat",controller.createNewChat);
app.post("/chatlist",controller.chatList);
app.post("/conversation/messages",controller.messageHistory);
app.post("/updateSession",controller.sessionUpdate);
app.post("/last/conversation/messages",controller.history);
app.post("/createSession",controller.createSession);
app.post("/sessionList",controller.sessionList);



mongoose.connect(db).then(()=>{
    console.log("Server and mongodb connected successfully");

    app.listen(PORT, "0.0.0.0", () => {
        console.log(`Server is running on http://0.0.0.0:${PORT}`);
    });
})