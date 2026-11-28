const mongoose = require("mongoose");
const schema = mongoose.Schema;

const messageSchema = mongoose.Schema({
    conversationId:{
        type:mongoose.Schema.Types.ObjectId,
        reference:"conversations",
        required:true,
    },
    userId:{
        type:String,
        required:true,
    },
    role:{
        type:String,
        required:true,
    },
    content:{
        type:String,
        required:true,
    }
});

module.exports = mongoose.model("messages",messageSchema);