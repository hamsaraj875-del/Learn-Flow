const mongoose = require("mongoose");

const conversationSchema = mongoose.Schema({
    title: {
      type: String,
      default: "New Chat"
    },
    userId:{
      type:String,
      required:true
    }
  },
  { timestamps: true }
)


module.exports = mongoose.model("conversations",conversationSchema);