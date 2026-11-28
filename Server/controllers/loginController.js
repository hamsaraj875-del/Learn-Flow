// external modules

const { check, validationResult } = require("express-validator");
const dotenv = require("dotenv");
const bcrypt = require("bcrypt");
const emailjs = require("@emailjs/nodejs");

// internal modules

const database = require("../models/database");
const OTP_DURATION = 5 * 60 * 1000;

// Checking user login

exports.loggedIn = (req, res, next) => {
    console.log("===login check===");
    if (req.session.isLoggedIn) {
        return res.status(200).json({
            success: true,
            userId: req.session.userId,
            name: req.session.userName,
            email:req.session.userEmail,
        });
    } else {
        return res.status(500).json({
            success: false,
            userId: "",
            name: "",
            email:req.session.userEmail,
        });
    }
};

// user sign up

exports.signUp = [
    check("name")
        .notEmpty()
        .withMessage("Name field cannot be empty")
        .matches(/^[a-zA-Z\s]+$/)
        .withMessage("Name cannot contain special characters"),

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

        const formattedError = {
            name: null,
            email: null,
            password: null,
        };

        if (!errors.isEmpty()) {
            errors.array().forEach((err) => {
                if (!formattedError[err.path]) {
                    formattedError[err.path] = err.msg;
                }
            });

            return res.status(400).json({
                success: false,
                message: formattedError,
                validationError: true,
            });
        }

        try {
            const { name, email, password } = req.body;
            const user = await database.findOne({ email: email });

            if (user) {
                return res.status(409).json({
                    success: false,
                    validationError: false,
                    message: "user already exists",
                });
            }

            req.session.userDetails = { name, email, password };

            const otp = otpGenerator();

            req.session.otp = otp;
            req.session.otpCreatedAt = Date.now();

            req.session.save();

            await generateEmail({ otp, email });

            return res.status(201).json({
                success: true,
                message: "Otp is sent to the email please check the mail",
            });

        } catch (err) {
            console.log(err);

            return res.status(500).json({
                success: false,
                validationError: false,
                message: "Error occurred while sending the email please try again",
            });
        }
    },
];

// OTP generator

const otpGenerator = () => {
    return Math.floor(100000 + Math.random() * 900000);
};

// OTP checker

exports.otp = async (req, res, next) => {
    const { otp } = req.body;

    if (
        !req.session.otp ||
        !req.session.userDetails ||
        !req.session.otpCreatedAt
    ) {
        return res.status(400).json({
            success: false,
            userId: "",
            name: "",
            email:"",
            mes: "OTP session expired. Please request a new OTP."
        });
    }

    const elapsedTime = Date.now() - req.session.otpCreatedAt;

    if (elapsedTime >= OTP_DURATION) {
        delete req.session.otp;
        delete req.session.otpCreatedAt;

        req.session.save();

        return res.status(400).json({
            success: false,
            userId: "",
            name: "",
            email:"",
            mes: "OTP expired. Please request a new OTP."
        });
    }

    if (!otp || String(otp) !== String(req.session.otp)) {
        return res.status(400).json({
            success: false,
            userId: "",
            name: "",
            email:"",
            mes: "Invalid OTP."
        });
    }

    const { name, email, password: passCode } = req.session.userDetails;

    try {
        const password = await bcrypt.hash(passCode, 10);

        const details = new database({
            name,
            email,
            password
        });

        const user = await details.save();

        req.session.userId = user._id;
        req.session.userName = name;
        req.session.userEmail = user.email;
        req.session.isLoggedIn = true;

        delete req.session.otp;
        delete req.session.otpCreatedAt;
        delete req.session.userDetails;

        req.session.save();

        return res.status(200).json({
            success: true,
            userId: user._id,
            name: name,
            email:email,
            mes: "Account created successfully."
        });

    } catch (err) {
        console.log(err);

        return res.status(500).json({
            success: false,
            userId: "",
            name: "",
            email:"",
            mes: "User creation failed. Please try again."
        });
    }
};

// Resend OTP

exports.resendOtp = async (req, res, next) => {
    try {
        if (!req.session.userDetails || !req.session.otpCreatedAt) {
            return res.status(400).json({
                success: false,
                mes: "Signup session expired. Please sign up again."
            });
        }

        const elapsedTime = Date.now() - req.session.otpCreatedAt;

        if (elapsedTime < OTP_DURATION) {
            const remainingTime = OTP_DURATION - elapsedTime;

            return res.status(429).json({
                success: false,
                mes: "Please wait before requesting another OTP.",
                remainingTime
            });
        }

        const { email } = req.session.userDetails;
        const otp = otpGenerator();

        req.session.otp = otp;
        req.session.otpCreatedAt = Date.now();

        req.session.save();

        await generateEmail({ otp, email });

        return res.status(200).json({
            success: true,
            mes: "New OTP sent successfully."
        });

    } catch (err) {
        console.log(err);

        return res.status(500).json({
            success: false,
            mes: "Error occurred while resending OTP."
        });
    }
};

// Email sender

const generateEmail = async ({ otp, email }) => {
    try {
        const templateParams = {
            email: email,
            passcode: otp,
            time: new Date(Date.now() + OTP_DURATION).toLocaleTimeString(),
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

        return response;

    } catch (err) {
        console.error("EmailJS error:", err);
        throw err;
    }
};

// User login

exports.login = async (req, res, next) => {
    console.log("====LOGIN====");

    const { email, password } = req.body;

    try {
        const user = await database.findOne({ email: email });

        if (user && (await bcrypt.compare(password, user.password))) {
            req.session.isLoggedIn = true;
            req.session.userName = user.name;
            req.session.userEmail = user.email;
            req.session.userId = user._id;

            const userId = user._id;
            const userName = user.name;

            req.session.save((err) => {
                if (err) {
                    console.log("Session save error:", err);

                    return res.status(500).json({
                        success: false,
                        userId: "",
                        name: "",
                    });
                }

                return res.status(200).json({
                    success: true,
                    userId: userId,
                    name: userName,
                });
            });

        } else {
            return res.status(500).json({
                success: false,
                userId: "",
                name: "",
            });
        }

    } catch (err) {
        console.log(err);

        return res.status(201).json({
            success: false,
            userId: "",
            name: "",
        });
    }
};