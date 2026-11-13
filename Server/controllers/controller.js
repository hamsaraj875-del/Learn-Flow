//external modules

const supabase = require("../services/supabase");

//internal modules

const messages = require("../models/messages");
const database = require("../models/database");
const conversation = require("../models/conversation");
const studySession = require("../models/studySession");


//User Analysis Details

exports.userAnalysis = async(req,res,next)=>{
    console.log("=== user analysis ===");
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        if(!user){
            return res.status(500).json({
                success:false,
                analysis : {sessionCount:0,sessionCompletion:0,accuracy:0,consistency:0,streak:0}
            })
        }
        const session = await studySession.find({userId:req.session.userId}).sort({_id:-1});
        const sessionCount = session.length;
        var sessionCompletion = 0;
        for (const item of session) {
            if (item.status === "completed") {
                sessionCompletion++;
            }
        }
        const completedDates = new Set(session.filter(s => s.status === "completed").map(s => new Date(s.startTime).toISOString().split("T")[0]));
        let streak = 0;
        let date = new Date();
        while (completedDates.has(date.toISOString().split("T")[0])) {
            streak++;
            date.setDate(date.getDate() - 1);
        }
        const accuracy = 0;
        const firstSession = await studySession.findOne({userId: req.session.userId}).sort({ startTime: 1 });
        const totalDays = firstSession? Math.floor((Date.now() - firstSession.startTime) / 86400000) + 1: 0;
        const activeDays = new Set(session.filter(s => s.status === "completed").map(s => new Date(s.startTime).toISOString().split("T")[0])).size;
        const consistancy = totalDays > 0 ? Math.round((activeDays / totalDays) * 100): 0;
        return res.status(200).json({
            success:true,
            analysis : {sessionCount:sessionCount,sessionCompletion:sessionCompletion,accuracy:0,consistancy:consistancy,streak:streak}
        })

    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            analysis : {sessionCount:0,sessionCompletion:0,accuracy:0,consistency:0,streak:0}
        })
    }
}


//creating new chats

exports.createNewChat=async(req,res,next)=>{
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        if(user){
            const lastChat = await conversation.findOne({userId:user._id}).sort({_id:-1});
            if (lastChat) {
                const chatDetails = await messages.findOne({conversationId: lastChat._id});
                if (chatDetails) {
                    const newChat = new conversation({chatName:data.chatName, userId: user._id ,topic:data.topic});
                    const newIdCreation = await newChat.save();
                    console.log(newIdCreation);
                    return res.status(200).json({
                        success: true,
                        conversationId: newIdCreation._id
                    });
                }

                await conversation.findOneAndUpdate(lastChat,{chatName:data.chatName,topic:data.topic});

                return res.status(200).json({
                    success: true,
                    conversationId: lastChat._id
                });
            }
            const newChat = new conversation({chatName:data.chatName,userId:user._id,topic:data.topic});
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

//chat list export

exports.chatList = async(req,res,next)=>{
    console.log("=== chat list ====");
    try{
        const list = await conversation.find({userId:req.session.userId}).select("_id chatName userId").sort({_id:-1});
        const chatList = list.map(chat => ({
            chatName: chat.chatName,
            userId: chat.userId,
            conversationId: chat._id.toString()
        }));
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


//last conversation of the user

exports.lastMessageHistory = async(req,res,next)=>{
    console.log("====Last Message===");
    const data = req.body;
    try{
        const chat = await conversation.findOne({userId:data.userId});
        if(chat){
            return res.status(200).json({
                success:true,
                conversationId:chat.conversationId,
            })
        }
        return res.status(500).json({
            success:false,
            conversationId:"",
        })
    }catch(err){
        console.log(err);
        return res.status(200).json({
            success:false,
            conversationId:"",
        })
    }
}

exports.history = async(req,res,next)=>{
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        if(!user){
            return res.status(200).json({
                success:false,
                conversationId:"",
            })
        }
        const conversate = await conversation.findOne({userId:req.session.userId}).sort({_id:-1});
        if(!conversate){
            return res.status(200).json({
                success:false,
                conversationId:"",
            })
        }
        return res.status(200).json({
            success:true,
            conversationId:conversate._id,
        })
    }catch(err){
        console.log(err);
        return res.status(200).json({
            success:false,
            conversationId:"",
        })
    }
}


//message history

exports.messageHistory = async(req,res,next)=>{
    console.log("=== Message List ===");
    const data = req.body;
    try{
        const chat = await conversation.findOne({userId:data.userId,_id:data.conversationId});
        if(chat){
            const mesList = await messages.find({conversationId:chat._id,userId:chat.userId}).select("role content")
            messageList = mesList.map(mes=>{
                return {
                    isUser:mes.role=='user'?true:false,
                    message:mes.content,
                }
            })
            return res.status(200).json({
                success:true,
                messageList:messageList,
            })
        }else{
            return res.status(500).json({
                success:false,
                messageList:[],
            })
        }
    }
    catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            messageList:[],
        })
    }
}


//session creation

exports.createSession = async(req,res,next)=>{
    console.log("=== create session ===");
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        if(!user || data.userId != req.session.userId){
            console.log(user);
            return res.status(500).json({
                success:false,
                mes:"User not found unauthorized access",
            })
        }
        const details = new studySession({userId:req.session.userId,sessionName:data.sessionName,startTime:data.startTime,endTime:0,plannedDuration:data.plannedDuration,actualDuration:0,status:"pending"});
        const dataStored = await details.save();
        return res.status(200).json({
            success:true,
            mes:dataStored._id,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            mes:"Session creation failed"
        })
    }
}

// session list export

exports.sessionList = async(req,res,next)=>{
    console.log("=== session list ===");
    const data = req.body;
    try{
        const user = await database.findById(data.userId);
        if(!user && data.userId!=req.session.userId){
            return res.status(500).json({
                success:false,
                mes:"user not found unauthorized access",
            })
        }
        const sessionList = await studySession.find({userId:req.session.userId});  
        return res.status(200).json({
            success:true,
            sessionList:sessionList,
        })
    }catch(err){
        console.log(err);
        return res.status(500).json({
            success:false,
            sessionList:[],
        })
    }
}

exports.sessionUpdate = async (req, res, next) => {
    console.log("=== session update ====");
    const data = req.body;
    try {
        const user = await database.findById(data.userId);
        if (!user || data.userId != req.session.userId) {
            return res.status(401).json({
                success: false,
                mes: "user not found, unauthorized access"
            });
        }

        const value = await studySession.findOneAndUpdate({ _id: data.id, userId: data.userId },{startTime: data.startTime, endTime: data.endTime, actualDuration: data.actualDuration,status: data.status},{ new: true });
        if (value) {
            return res.status(200).json({
                success: true,
                mes: "Successfully updated data"
            });
        }
        return res.status(404).json({
            success: false,
            mes: "session not found"
        });
    } catch (err) {
        console.log(err);
        return res.status(500).json({
            success: false,
            mes: "Internal server error"
        });
    }
};




