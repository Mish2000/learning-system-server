package com.learningsystemserver.exceptions;

public class AlreadyInUseException  extends Exception{
    private static final long serialVersionUID = 6002226348503365414L;

    public AlreadyInUseException(String message) {
        super(message);
    }
}