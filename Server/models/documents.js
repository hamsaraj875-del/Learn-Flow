
const mongoose = require("mongoose");

const documentSchema = new mongoose.Schema({
    userId: {
        type: mongoose.Schema.Types.ObjectId,
        required: true,
        ref: "users"
    },
    conversationId: {
        type: mongoose.Schema.Types.ObjectId,
        required: true
    },
    fileName: {
        type: String,
        required: true
    }
}, {
    timestamps: true
});

module.exports = mongoose.model("documents", documentSchema);
