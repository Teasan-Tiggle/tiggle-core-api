package com.ssafy.tiggle.service.account;

import com.ssafy.tiggle.entity.Users;

public interface AccountVerificationTokenService {
    
    String generateVerificationToken(String accountNo, Users user);
    
    boolean validateToken(String token);
    
    boolean validateTokenForAccount(String token, String accountNo);
    
    void markTokenAsUsed(String token);
    
    void cleanupExpiredTokens();
}