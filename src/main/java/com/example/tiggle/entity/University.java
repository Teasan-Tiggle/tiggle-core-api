package com.example.tiggle.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;
}
