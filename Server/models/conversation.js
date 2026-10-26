const mongoose = require("mongoose");

const conversationSchema = mongoose.Schema({
    chatName: {
      type: String,
      required:true,
    },
    userId:{
      type:String,
      required:true,
    },
    topic:{
      type:String,
      required:true,
    }
  },
  { timestamps: true }
)


module.exports = mongoose.model("conversations",conversationSchema);