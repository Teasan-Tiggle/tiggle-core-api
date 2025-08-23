package com.example.tiggle.service.account;

import com.example.tiggle.entity.Student;

public interface AccountVerificationTokenService {
    
    String generateVerificationToken(String accountNo, Student student);
    
    boolean validateToken(String token);
    
    boolean validateTokenForAccount(String token, String accountNo);
    
    void markTokenAsUsed(String token);
    
    void cleanupExpiredTokens();
}