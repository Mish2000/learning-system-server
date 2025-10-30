package com.learningsystemserver.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 30)
    private String username;

    @Column(nullable=false, unique = true)
    private String email;

    @Column(nullable=false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role = Role.USER;

    @Column(name = "interface_language", nullable = false, length = 16)
    private String interfaceLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_difficulty", nullable = false)
    private DifficultyLevel currentDifficulty;

    @Column(name = "sub_difficulty_level")
    private Integer subDifficultyLevel;

    /**
     * Personal overall progress (averaged across all subtopics).
     * Stored both as a score (1.0..5.0) and an enum level (BASIC..EXPERT).
     */
    @Column(name = "overall_progress_score", nullable = false)
    private Double overallProgressScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_progress_level", nullable = false)
    private DifficultyLevel overallProgressLevel;

    @Lob
    private byte[] profileImage;

    @PrePersist
    public void prePersistDefaults() {
        if (interfaceLanguage == null || interfaceLanguage.isBlank()) {
            interfaceLanguage = "en";
        }
        if (currentDifficulty == null) {
            currentDifficulty = DifficultyLevel.BASIC;
        }
        if (overallProgressLevel == null) {
            overallProgressLevel = DifficultyLevel.BASIC;
        }
        if (overallProgressScore == null) {
            overallProgressScore = 1.0;
        }
        if (subDifficultyLevel == null) {
            subDifficultyLevel = 0;
        }
        if (role == null) {
            role = Role.USER;
        }
    }
}
