package com.ssafy.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name = "dutchpay_share")
@Getter @Setter
public class DutchpayShare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="dutchpay_id", nullable=false)
    private Dutchpay dutchpay;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private Users user;

    @Column(nullable=false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DutchpayShareStatus status;

    private LocalDateTime notifiedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "pay_more", nullable = false)
    private boolean payMore;

    /** 결제 시 확정된 자투리 금액 (roundUp(100) - amount), 미선택이면 0 */
    @Column(name = "tiggle_amount", nullable = false)
    private long tiggleAmount;

    /** 실제 납부 금액 (amount + tiggleAmount) */
    @Column(name = "paid_amount", nullable = false)
    private long paidAmount;

    public void settle(boolean payMoreSelected, long topUp) {
        this.payMore = payMoreSelected;
        long tiggle = Math.max(0L, topUp);
        this.tiggleAmount = tiggle;
        this.paidAmount   = (this.amount == null ? 0L : this.amount) + tiggle;
        this.status = DutchpayShareStatus.PAID;
        this.notifiedAt = java.time.LocalDateTime.now();
    }

}

