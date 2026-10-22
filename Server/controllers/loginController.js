//external modules

const {check,validationResult} = require("express-validator");
const dotenv = require("dotenv");
const bcrypt = require("bcrypt");
const emailjs = require("@emailjs/nodejs");


//internal modules

const database = require("../models/database");


//Checking user login

exports.loggedIn=(req,res,next)=>{
  if(req.session.isLoggedIn){
    return res.status(200).json({
      success:true,
      userId:req.session.userId,
      name:req.session.userName,
    })
  }else{
    console.log("user is not logged in");
    return res.status(500).json({
      success:false,
      userId:"",
      name:"",
    })
  }
}




//user sign up

exports.signUp = [
  check("name")
    .notEmpty()
    .withMessage("Name field cannot be empty")
    .matches(/^[a-zA-Z\s]+$/)
    .withMessage("Name cannot contain speacial characters"),

  check("email")
    .notEmpty()
    .withMessage("email field cannot be empty")
    .isEmail()
    .withMessage("Enter a valid email id"),

  check("password")
    .isLength({ min: 8 })
    .withMessage("Password should have at least 8 characters")
    .matches(/[a-z]/)
    .withMessage("password should have lower case")
    .matches(/[A-Z]/)
    .withMessage("password should have upper case")
    .matches(/[0-9]/)
    .withMessage("password should have digits")
    .matches(/[^a-zA-Z0-9]/)
    .withMessage("password should have special characters"),
  async (req, res, next) => {
    const errors = validationResult(req);

    formattedError = {
      name: null,
      email: null,
      password: null,
    };

    if (!errors.isEmpty()) {
      console.log("some error in signup")
      errors.array().forEach((err) => {
        if (!formattedError[err.path]) {
          formattedError[err.path] = err.msg;
        }
      });
      console.log(formattedError);
      return res.status(400).json({
        success: false,
        message: formattedError,
        validationError: true,
      });
    }
    if (errors.isEmpty()) {
      console.log("no errors just signup process")
      try {
        const { name, email, password } = req.body;
        const user = await database.findOne({ email: email });
        if (user) {
          return res.status(409).json({
            success: false,
            validationError: false,
            message: "user already exists",
          });
        } else {
          req.session.userDetails = { name, email, password };
          const otp = otpGenerator();
          try {
            req.session.otp = otp;
            req.session.save();
            await generateEmail({otp,email});
            return res.status(201).json({
              success: true,
              message: "Otp is sent to the email please check the jmail",
            });
          } catch(err) {
            console.log(err);
            return res.status(500).json({
              success: false,
              validationError: false,
              message:
                "Error occurred while sending the email please try again ",
            });
          }
        }
      } catch (err) {
        console.log(err);
        return res.status(500).json({
          success: false,
          validationError: false,
          message: "Internal server error",
        });
      }
    }
  },
];



//otp generator

const otpGenerator = () => {
  return Math.floor(100000 + Math.random() * 900000);
};



//otp checker

exports.otp = async (req, res, next) => {
  const { otp } = req.body;

  if (!req.session.otp || !req.session.userDetails) {
    return res.status(400).json({
      success: false,
      mes: "OTP expired. Please request a new OTP"
    });
  }
  if (String(otp) !== String(req.session.otp)) {
    console.log("not success otp");
    return res.status(400).json({
      success: false,
      mes: "Invalid OTP"
    });
  }
  const { name:user, email, password: passCode } = req.session.userDetails;
  try {
    console.log("otp successfull");
    const password = await bcrypt.hash(passCode, 10);
    const details = new database({user,email,password});
    const userId = await details.save();
    req.session.userId = userId._id;
    req.session.userName = user;
    req.session.isLoggedIn = true;
    delete req.session.otp;
    delete req.session.userDetails;
    req.session.save((err) => {
      if (err) {
        console.log(err);
        return res.status(500).json({
          success: false,
          userId:"",
          name:"",
        });
      }
      return res.json({
        success: true,
        userId:userId._id,
        name:user,
      });
    });
  } catch (err) {
    console.log(err);
    return res.status(500).json({
      success: false,
      userId:"",
      name:"",
    });
  }
};


//email sender

const generateEmail = async ({ otp,email }) => {
  try {
    const templateParams = {
      email: email,
      passcode: otp,
      time: new Date(Date.now() + 15 * 60 * 1000).toLocaleTimeString(),
    };

    const response = await emailjs.send(
      process.env.EMAIL_SERVICE_ID,
      process.env.EMAIL_TEMPLATE_ID,
      templateParams,
      {
        publicKey: process.env.EMAIL_PUBLIC_KEY,
        privateKey: process.env.EMAIL_PRIVATE_KEY,
      }
    );

    console.log("Confirmation email sent through EmailJS.");
    return response;
  } catch (err) {
    console.error("EmailJS error:", err);
    throw err;
  }
};


//user login for account

exports.login = async (req, res, next) => {
  const { email, password } = req.body;

  try {
    const user = await database.findOne({ email: email });
    console.log(user);
    if (user && (await bcrypt.compare(password, user.password))) {
      req.session.isLoggedIn = true;
      req.session.userName = user.name;
      req.session.userId = user._id;
      const userId = user._id;
      const userName = user.name;
      req.session.save((err) => {
        if (err) {
          console.log("Session save error:", err);

          return res.status(500).json({
            success: false,
            userId:"",
            name:"",
          });
        }

        console.log("login successfull");

        return res.status(200).json({
          success: true,
          userId:userId,
          name:userName,
        });
      });
    } else {
      return res.status(500).json({
        success: false,
        userId:"",
        name:"",
      });
    }
  } catch (err) {
    return res.status(201).json({
      success: false,
      userId:"",
      name:"",
    });
  }
};


