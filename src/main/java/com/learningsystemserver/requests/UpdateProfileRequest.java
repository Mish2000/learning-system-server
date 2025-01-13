package com.learningsystemserver.requests;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String interfaceLanguage;
    private String solutionDetailLevel;
}
