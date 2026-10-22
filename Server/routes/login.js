//external modules
const express = require("express");
const database =require("../models/database");

//internal modules

const loginController = require("../controllers/loginController");
const loginRouter = express.Router();

loginRouter.post("/loggedIn",loginController.loggedIn);
loginRouter.post("/signUp",loginController.signUp);
loginRouter.post("/otp",loginController.otp);
loginRouter.post("/login",loginController.login);


module.exports = loginRouter;

