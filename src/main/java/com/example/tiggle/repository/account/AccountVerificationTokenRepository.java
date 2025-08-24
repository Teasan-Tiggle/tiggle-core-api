package com.example.tiggle.repository.account;

import com.example.tiggle.entity.AccountVerificationToken;
import com.example.tiggle.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccountVerificationTokenRepository extends JpaRepository<AccountVerificationToken, Long> {
    
    Optional<AccountVerificationToken> findByVerificationToken(String verificationToken);
    
    Optional<AccountVerificationToken> findByAccountNoAndUserAndUsedFalse(String accountNo, Users user);
    
    @Modifying
@Query("DELETE FROM AccountVerificationToken a WHERE a.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
@Query("UPDATE AccountVerificationToken a SET a.used = true WHERE a.verificationToken = :token")
    void markTokenAsUsed(@Param("token") String token);
}