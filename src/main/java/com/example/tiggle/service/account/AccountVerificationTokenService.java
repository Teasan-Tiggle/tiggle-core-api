package com.example.tiggle.service.account;

import com.example.tiggle.entity.Student;

public interface AccountVerificationTokenService {
    
    String generateVerificationToken(String accountNo, Student student);
    
    boolean validateToken(String token);
    
    void markTokenAsUsed(String token);
    
    void cleanupExpiredTokens();
}