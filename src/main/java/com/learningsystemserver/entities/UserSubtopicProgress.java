package com.learningsystemserver.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_subtopic_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "subtopic_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubtopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtopic_id", nullable = false)
    private Topic subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel currentDifficulty;

    @Column(nullable = false)
    private int correctStreak;

    @Column(nullable = false)
    private int wrongStreak;

    @Column(nullable = false)
    private int attemptsSinceLastChange;

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
}
