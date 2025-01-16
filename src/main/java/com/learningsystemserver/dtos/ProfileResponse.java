package com.learningsystemserver.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String username;
    private String email;
    private String interfaceLanguage;
    private String solutionDetailLevel;
}
