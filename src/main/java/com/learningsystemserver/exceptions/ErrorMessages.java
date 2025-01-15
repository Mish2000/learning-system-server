package com.learningsystemserver.exceptions;

import lombok.Getter;

@Getter
public enum ErrorMessages {
    USERNAME_DOES_NOT_EXIST("There is no user with the given username: %s "),
    USERNAME_ALREADY_EXIST("username: \'%s\' is already in use."),
    EMAIL_ALREADY_EXIST("email: \'%s\' is already in use."),

    LOGIN_FAILED("Invalid Input: email: '%s' , password: '%s' could not login as: '%s'");
    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

}