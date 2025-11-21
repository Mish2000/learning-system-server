package com.learningsystemserver.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String username;
    private String email;
    private String interfaceLanguage;
    private String solutionDetailLevel;
    private String profileImage;
    private Integer subDifficultyLevel;
    private String currentDifficulty;
    private String newToken;
    private String role;
}
