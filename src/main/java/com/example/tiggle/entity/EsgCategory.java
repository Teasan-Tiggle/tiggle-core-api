package com.example.tiggle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "esg_category",
        uniqueConstraints = @UniqueConstraint(name = "uk_esg_category_name", columnNames = "name"),
        indexes = @Index(name = "idx_esg_category_name", columnList = "name")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EsgCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리명: Planet, People, Prosperity
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    // 캐릭터명: 몰리, 쏠, 리노
    @Column(name = "character_name", length = 50)
    private String characterName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
