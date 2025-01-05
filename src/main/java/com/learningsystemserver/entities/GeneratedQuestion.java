package com.learningsystemserver.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generated_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String questionText;

    @Column(length = 2000)
    private String solutionSteps;

    private String correctAnswer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;
}
