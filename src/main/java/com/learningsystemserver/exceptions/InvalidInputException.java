package com.learningsystemserver.exceptions;

public class InvalidInputException  extends Exception{
    private static final long serialVersionUID = -2700628185028924446L;

    public InvalidInputException(String message) {
        super(message);
    }
}