package com.example.tiggle.service.account.impl;

import com.example.tiggle.entity.AccountVerificationToken;
import com.example.tiggle.entity.Student;
import com.example.tiggle.repository.account.AccountVerificationTokenRepository;
import com.example.tiggle.service.account.AccountVerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountVerificationTokenServiceImpl implements AccountVerificationTokenService {
    
    private final AccountVerificationTokenRepository tokenRepository;
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    
    @Override
    public String generateVerificationToken(String accountNo, Student student) {
        Optional<AccountVerificationToken> existingToken =
                tokenRepository.findByAccountNoAndStudentAndUsedFalse(accountNo, student);
        
        if (existingToken.isPresent() && existingToken.get().isValid()) {
            return existingToken.get().getVerificationToken();
        }
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        
        AccountVerificationToken verificationToken = new AccountVerificationToken(accountNo, token, student, expiresAt);
        
        tokenRepository.save(verificationToken);
        
        log.info("계좌 인증 토큰 생성 완료 - 계좌번호: {}, 사용자ID: {}", accountNo, student.getId());
        return token;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        Optional<AccountVerificationToken> tokenEntity = tokenRepository.findByVerificationToken(token);
        
        if (tokenEntity.isEmpty()) {
            log.warn("존재하지 않는 토큰: {}", token);
            return false;
        }
        
        AccountVerificationToken verificationToken = tokenEntity.get();
        boolean isValid = verificationToken.isValid();
        
        if (!isValid) {
            log.warn("유효하지 않은 토큰 - 토큰: {}, 사용여부: {}, 만료여부: {}", 
                    token, verificationToken.getUsed(), verificationToken.isExpired());
        }
        
        return isValid;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean validateTokenForAccount(String token, String accountNo) {
        Optional<AccountVerificationToken> tokenEntity = tokenRepository.findByVerificationToken(token);
        
        if (tokenEntity.isEmpty()) {
            log.warn("존재하지 않는 토큰: {}", token);
            return false;
        }
        
        AccountVerificationToken verificationToken = tokenEntity.get();
        
        // 토큰이 유효한지 확인
        if (!verificationToken.isValid()) {
            log.warn("유효하지 않은 토큰 - 토큰: {}, 사용여부: {}, 만료여부: {}", 
                    token, verificationToken.getUsed(), verificationToken.isExpired());
            return false;
        }
        
        // 토큰의 계좌번호와 요청된 계좌번호가 일치하는지 확인
        if (!accountNo.equals(verificationToken.getAccountNo())) {
            log.warn("토큰의 계좌번호와 요청 계좌번호 불일치 - 토큰 계좌: {}, 요청 계좌: {}", 
                    verificationToken.getAccountNo(), accountNo);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void markTokenAsUsed(String token) {
        tokenRepository.markTokenAsUsed(token);
        log.info("토큰 사용 처리 완료: {}", token);
    }
    
    @Override
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("만료된 토큰 정리 완료");
    }
}