package com.learningsystemserver.advice;

import com.learningsystemserver.exceptions.AlreadyInUseException;
import com.learningsystemserver.exceptions.IllegalOperationException;
import com.learningsystemserver.exceptions.InvalidInputException;
import com.learningsystemserver.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred.";

    @ExceptionHandler({AlreadyInUseException.class, InvalidInputException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInputRequests(Exception e){
        logger.warn(e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedRequests(Exception e){
        logger.warn(e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotAllowedRequests(Exception e){
        logger.warn(e.getMessage());
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedRequests(AccessDeniedException e) {
        logger.warn(e.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationRequests(MethodArgumentNotValidException e) {
        String message = buildValidationMessage(e);
        logger.warn(message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusRequests(ResponseStatusException e) {
        HttpStatus status = resolveHttpStatus(e.getStatusCode());
        String message = resolveResponseStatusMessage(e, status);

        if (status.is4xxClientError()) {
            logger.warn(message);
        } else {
            logger.error(message, e);
        }

        return buildErrorResponse(status, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedRequests(Exception e) {
        logger.error("Unhandled exception", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE);
    }

    private String buildValidationMessage(MethodArgumentNotValidException e) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (!fieldErrors.isBlank()) {
            return "Validation failed: " + fieldErrors;
        }

        String objectErrors = e.getBindingResult().getGlobalErrors().stream()
                .map(error -> safeMessage(error.getDefaultMessage(), "Invalid request."))
                .collect(Collectors.joining("; "));

        if (!objectErrors.isBlank()) {
            return "Validation failed: " + objectErrors;
        }

        return "Validation failed.";
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + safeMessage(fieldError.getDefaultMessage(), "is invalid");
    }

    private String resolveResponseStatusMessage(ResponseStatusException e, HttpStatus status) {
        if (status.is5xxServerError()) {
            return INTERNAL_SERVER_ERROR_MESSAGE;
        }

        String reason = e.getReason();

        if (reason != null && !reason.isBlank()) {
            return reason;
        }

        return status.getReasonPhrase();
    }

    private HttpStatus resolveHttpStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        return handleErrors(ErrorResponse.builder()
                .message(safeMessage(message, status.getReasonPhrase()))
                .error(status)
                .status(status.value())
                .build());
    }

    private ResponseEntity<ErrorResponse> handleErrors(ErrorResponse errorResponse){
        return new ResponseEntity<>(errorResponse, errorResponse.getError());
    }
}
