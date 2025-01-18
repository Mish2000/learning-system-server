package com.learningsystemserver.dtos;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String password;
    private String interfaceLanguage;
}
