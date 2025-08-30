package com.ssafy.tiggle.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esg_category_id", nullable = false, columnDefinition = "BIGINT DEFAULT 1")
    private EsgCategory esgCategory;

    @Column(name = "level", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer level;

    @Column(name = "experience_points", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long experiencePoints;

    @Column(name = "heart", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer heart;

    public void addHeart(Long amount) {
        long heart = amount / 200;
        this.heart += (int) heart;
    }
}
