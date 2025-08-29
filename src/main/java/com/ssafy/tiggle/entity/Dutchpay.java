package com.ssafy.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "dutchpay")
@Getter @Setter
public class Dutchpay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable=false)
    private Long totalAmount;

    @Column(nullable=false)
    private Boolean payMore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="creator_id", nullable=false)
    private Users creator;

    @Column(nullable = false, length = 20)
    private String status = "REQUESTED"; // REQUESTED / CANCELED / COMPLETED

    private Long roundedPerPerson;

    private Long piggyRemainder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "dutchpay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DutchpayShare> shares = new ArrayList<>();
}
