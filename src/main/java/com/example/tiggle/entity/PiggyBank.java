package com.example.tiggle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "piggy_bank",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_piggy_bank_student", columnNames = "student_id"),
                @UniqueConstraint(name = "uk_piggy_bank_account_no", columnNames = "account_no")
        },
        indexes = {
                @Index(name = "idx_piggy_bank_created_at", columnList = "created_at"),
                @Index(name = "idx_piggy_bank_account_no", columnList = "account_no")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PiggyBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50)
    @Builder.Default
    private String name = "저금통";

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_piggy_bank_student"))
    private Users owner;

    @Column(name = "account_no", length = 30)
    private String accountNo;

    @Column(name = "current_amount", precision = 10, scale = 2,
            nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "target_amount", precision = 10, scale = 2,
            nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 5000.00")
    @Builder.Default
    private BigDecimal targetAmount = new BigDecimal("5000.00");

    @Column(name = "saving_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer savingCount = 0;

    @Column(name = "donation_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer donationCount = 0;

    @Column(name = "donation_total_amount", precision = 10, scale = 2,
            nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal donationTotalAmount = BigDecimal.ZERO;

    @Column(name = "auto_donation", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean autoDonation = false;

    @Column(name = "auto_saving", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean autoSaving = false;

    /** 저금통당 1개 선택 (여러 저금통이 같은 카테고리를 공유 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esg_category_id",
            foreignKey = @ForeignKey(name = "fk_piggy_bank_esg_category"))
    private EsgCategory esgCategory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
