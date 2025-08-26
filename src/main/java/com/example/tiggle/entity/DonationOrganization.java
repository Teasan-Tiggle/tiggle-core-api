package com.example.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DonationOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esg_category_id", nullable = false)
    private EsgCategory esgCategory;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "account_no", length = 30)
    private String accountNo;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "url", length = 255)
    private String url;
}
