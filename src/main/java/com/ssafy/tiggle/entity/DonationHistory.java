package com.ssafy.tiggle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "account_type", length = 20, nullable = false)
    private String accountType;

    @Column(name = "account_no", length = 30, nullable = false)
    private String accountNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esg_category_id", nullable = false)
    private EsgCategory esgCategory;

    @Column(name = "amount", nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal amount;

    @Column(name = "donated_at", nullable = false)
    private LocalDateTime donatedAt;

    @Column(name = "title", length = 50, nullable = false)
    private String title;
}
