//external modules

const express = require("express");
const app = express();
const mongoose = require("mongoose");
const multer = require("multer");
const session = require("express-session");
const loginRouter = require("./login")
const MongoStore = require("connect-mongo").default;



//internal modules

const controller  = require("../controllers/controller");
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

const storage = multer.diskStorage({
    destination:(req,res,cb)=>{
        cb(null,'uploads');
    },
    filename:(req,file,cb)=>{
        cb(null,new Date().toString()+'-'+file.originalname);
    }
})

const fileFilter = (req,file,cb)=>{
    if(['application/pdf'].includes(file.mimetype)){
        cb(null,true);
    }else{
        cb(null,false);
    }
}


//configuations

app.use(express.json());
require("dotenv").config();
app.use(multer({storage,fileFilter}).single('pdf'));


//routers

app.use("/user",loginRouter);


//main routes handling

app.post("/chat",controller.openAiResult);
app.post("/document",controller.documentOpenAiResult);
app.post("/new_chat",controller.createNewChat);
app.post("/chatlist",controller.chatList);
app.post("/conversation/messages",controller.messageHistory);


mongoose.connect(db).then(()=>{
    console.log("Server and mongodb connected successfully");

    app.listen(PORT, "0.0.0.0", () => {
        console.log(`Server is running on http://0.0.0.0:${PORT}`);
    });
})