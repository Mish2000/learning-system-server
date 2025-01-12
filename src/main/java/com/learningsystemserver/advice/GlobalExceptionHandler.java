package com.learningsystemserver.advice;

import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.IllegalOperationException;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({AlreadyInUseException.class, InvalidInputException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInputRequests(Exception e){
        logger.error(e.getMessage());
        return handleErrors(ErrorResponse.builder()
                .message(e.getMessage())
                .error(HttpStatus.BAD_REQUEST)
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedRequests(Exception e){
        logger.error(e.getMessage());
        return handleErrors(ErrorResponse.builder()
                .message(e.getMessage())
                .error(HttpStatus.UNAUTHORIZED)
                .status(HttpStatus.UNAUTHORIZED.value())
                .build());
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotAllowedRequests(Exception e){
        logger.error(e.getMessage());
        return handleErrors(ErrorResponse.builder()
                .message(e.getMessage())
                .error(HttpStatus.METHOD_NOT_ALLOWED)
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .build());
    }

    private ResponseEntity<ErrorResponse> handleErrors(ErrorResponse errorResponse){
        return new ResponseEntity<>(errorResponse, errorResponse.getError());
    }
}
