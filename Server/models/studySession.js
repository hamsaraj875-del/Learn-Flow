const mongoose = require("mongoose");

const schema = mongoose.Schema({
    userId:{type:Number,required:true},
    startTime:{type:Number,required:true},
    endTime:{type:Number,required:true},
    plannedDuration:{type:Number,required:true},
    actualDuration:{type:Number,required:true},
});

module.exports = mongoose.model("studySessions",schema);