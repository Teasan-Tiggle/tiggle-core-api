package com.example.tiggle.entity;

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
}

