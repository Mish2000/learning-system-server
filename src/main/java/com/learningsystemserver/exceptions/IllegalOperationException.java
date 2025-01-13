package com.learningsystemserver.exceptions;

public class IllegalOperationException extends Exception{
    private static final long serialVersionUID = -694836301261877893L;

    public IllegalOperationException(String message) {
        super(message);
    }
}