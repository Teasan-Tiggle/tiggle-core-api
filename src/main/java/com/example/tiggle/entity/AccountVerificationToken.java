package com.example.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AccountVerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_no", nullable = false)
    private String accountNo;
    
    @Column(name = "verification_token", nullable = false, unique = true, length = 255)
    private String verificationToken;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean used = false;
    
    public AccountVerificationToken(String accountNo, String verificationToken, Users user, LocalDateTime expiresAt) {
        this.accountNo = accountNo;
        this.verificationToken = verificationToken;
        this.user = user;
        this.expiresAt = expiresAt;
        this.used = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}