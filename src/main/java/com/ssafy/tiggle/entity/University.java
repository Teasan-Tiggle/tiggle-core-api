package com.ssafy.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "user_key", unique = true, nullable = false)
    private String userKey;

    @Column(name = "planet_account_no", length = 30, nullable = false)
    private String planetAccountNo;

    @Column(name = "people_account_no", length = 30, nullable = false)
    private String peopleAccountNo;

    @Column(name = "prosperity_account_no", length = 30, nullable = false)
    private String prosperityAccountNo;
}
