package com.learningsystemserver.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel currentDifficulty;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String interfaceLanguage;
    private String solutionDetailLevel;
}

