package com.example.tiggle.service.auth;

public interface MailService {
    void sendEmail(String to, String subject, String text);
}
