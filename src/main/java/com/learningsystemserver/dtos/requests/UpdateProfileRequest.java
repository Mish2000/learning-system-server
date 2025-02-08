package com.learningsystemserver.dtos.requests;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String password;
    private String interfaceLanguage;
    private String profileImage;
}
