package com.placify.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from.email}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name}")
    private String fromName;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    /**
     * Send password reset email with OTP
     * @param toEmail Recipient email
     * @param otp 6-digit OTP code
     * @throws EmailSendException if email fails to send
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        String subject = "Password Reset OTP - Placify";
        
        String htmlContent = buildPasswordResetOtpEmailHtml(otp);
        Content content = new Content("text/html", htmlContent);
        
        Mail mail = new Mail(from, subject, to, content);
        
        sendEmail(mail, "password reset OTP", toEmail);
    }
    
    /**
     * Send email verification OTP for new signups
     * @param toEmail Recipient email
     * @param otp 6-digit OTP code
     * @param userName User's name
     * @throws EmailSendException if email fails to send
     */
    public void sendEmailVerificationOtp(String toEmail, String otp, String userName) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        String subject = "Verify Your Email - Placify";
        
        String htmlContent = buildEmailVerificationOtpHtml(otp, userName);
        Content content = new Content("text/html", htmlContent);
        
        Mail mail = new Mail(from, subject, to, content);
        
        sendEmail(mail, "email verification OTP", toEmail);
    }
    
    /**
     * Common method to send email via SendGrid
     */
    private void sendEmail(Mail mail, String emailType, String toEmail) {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("{} email sent successfully to: {}", emailType, toEmail);
            } else {
                logger.error("Failed to send {} email. Status: {}, Body: {}", 
                    emailType, response.getStatusCode(), response.getBody());
                throw new EmailSendException("Failed to send " + emailType + " email");
            }
        } catch (IOException e) {
            logger.error("Error sending {} email to {}: {}", emailType, toEmail, e.getMessage());
            throw new EmailSendException("Failed to send " + emailType + " email", e);
        }
    }
    
    /**
     * Build HTML email template for email verification OTP
     */
    private String buildEmailVerificationOtpHtml(String otp, String userName) {
        String displayName = (userName != null && !userName.isEmpty()) ? userName : "there";
        
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0; }" +
            "        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }" +
            "        .otp-box { background-color: #fff; border: 3px dashed #667eea; padding: 20px; text-align: center; margin: 30px 0; border-radius: 8px; }" +
            "        .otp-code { font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; margin: 10px 0; }" +
            "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }" +
            "        .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 3px; }" +
            "        .welcome { background-color: #e8f5e9; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0; border-radius: 3px; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class='container'>" +
            "        <div class='header'>" +
            "            <h1 style='margin: 0;'>🎉 Welcome to Placify!</h1>" +
            "        </div>" +
            "        <div class='content'>" +
            "            <div class='welcome'>" +
            "                <h2 style='color: #4CAF50; margin-top: 0;'>Hello " + displayName + "!</h2>" +
            "                <p style='margin-bottom: 0;'>Thank you for signing up! We're excited to have you on board.</p>" +
            "            </div>" +
            "            <h3 style='color: #667eea;'>Verify Your Email Address</h3>" +
            "            <p>To complete your registration and start using Placify, please verify your email address using the OTP code below:</p>" +
            "            <div class='otp-box'>" +
            "                <p style='margin: 0; font-size: 14px; color: #666;'>Your Verification Code</p>" +
            "                <div class='otp-code'>" + otp + "</div>" +
            "                <p style='margin: 0; font-size: 12px; color: #999;'>Enter this code to verify your email</p>" +
            "            </div>" +
            "            <div class='warning'>" +
            "                <strong>⚠️ Important:</strong>" +
            "                <ul style='margin: 10px 0;'>" +
            "                    <li>This OTP will expire in <strong>15 minutes</strong></li>" +
            "                    <li>Do not share this code with anyone</li>" +
            "                    <li>If you didn't create an account, please ignore this email</li>" +
            "                    <li>You have 3 attempts to enter the correct OTP</li>" +
            "                </ul>" +
            "            </div>" +
            "            <p>Once verified, you'll have full access to all Placify features including:</p>" +
            "            <ul>" +
            "                <li>📄 Resume Optimizer</li>" +
            "                <li>🏢 Company Insights</li>" +
            "                <li>💰 Salary Analyzer</li>" +
            "                <li>🎓 Interview Preparation</li>" +
            "                <li>💬 AI Career Chatbot</li>" +
            "            </ul>" +
            "            <p>If you have any questions, please contact our support team.</p>" +
            "            <p>Best regards,<br><strong>The Placify Team</strong></p>" +
            "        </div>" +
            "        <div class='footer'>" +
            "            <p>&copy; 2026 Placify. All rights reserved.</p>" +
            "            <p>This is an automated email. Please do not reply.</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
    }
    
    /**
     * Build HTML email template for OTP
     */
    private String buildPasswordResetOtpEmailHtml(String otp) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            "        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
            "        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }" +
            "        .otp-box { background-color: #fff; border: 3px dashed #4CAF50; padding: 20px; text-align: center; margin: 30px 0; border-radius: 8px; }" +
            "        .otp-code { font-size: 36px; font-weight: bold; color: #4CAF50; letter-spacing: 8px; margin: 10px 0; }" +
            "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }" +
            "        .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 3px; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class='container'>" +
            "        <div class='header'>" +
            "            <h1 style='margin: 0;'>🔐 Placify</h1>" +
            "        </div>" +
            "        <div class='content'>" +
            "            <h2 style='color: #4CAF50;'>Password Reset OTP</h2>" +
            "            <p>Hello,</p>" +
            "            <p>You requested to reset your password. Use the OTP code below to verify your identity:</p>" +
            "            <div class='otp-box'>" +
            "                <p style='margin: 0; font-size: 14px; color: #666;'>Your OTP Code</p>" +
            "                <div class='otp-code'>" + otp + "</div>" +
            "                <p style='margin: 0; font-size: 12px; color: #999;'>Enter this code in the app</p>" +
            "            </div>" +
            "            <div class='warning'>" +
            "                <strong>⚠️ Important:</strong>" +
            "                <ul style='margin: 10px 0;'>" +
            "                    <li>This OTP will expire in <strong>10 minutes</strong></li>" +
            "                    <li>Do not share this code with anyone</li>" +
            "                    <li>If you didn't request this, please ignore this email</li>" +
            "                    <li>You have 3 attempts to enter the correct OTP</li>" +
            "                </ul>" +
            "            </div>" +
            "            <p>If you have any questions, please contact our support team.</p>" +
            "            <p>Best regards,<br><strong>The Placify Team</strong></p>" +
            "        </div>" +
            "        <div class='footer'>" +
            "            <p>&copy; 2026 Placify. All rights reserved.</p>" +
            "            <p>This is an automated email. Please do not reply.</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
    }
    
    /**
     * Custom exception for email sending errors
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message) {
            super(message);
        }
        
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
