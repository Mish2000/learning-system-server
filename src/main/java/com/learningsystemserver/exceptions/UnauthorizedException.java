package com.learningsystemserver.exceptions;

public class UnauthorizedException extends Exception{
    private static final long serialVersionUID = -7349370204572297321L;

    public UnauthorizedException(String message) {
        super(message);
    }
}