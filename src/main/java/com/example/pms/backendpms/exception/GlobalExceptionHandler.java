package com.example.pms.backendpms.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
    StringBuilder message = new StringBuilder("Validation failed");

    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      message
          .append(": ")
          .append(fieldError.getField())
          .append(" ")
          .append(fieldError.getDefaultMessage());
      break;
    }

    return buildResponse(HttpStatus.BAD_REQUEST, message.toString());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  ResponseEntity<Map<String, Object>> handleForbidden(Exception exception) {
    return buildResponse(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
  }

  private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("timestamp", LocalDateTime.now());
    response.put("status", status.value());
    response.put("error", status.getReasonPhrase());
    response.put("message", message);
    return ResponseEntity.status(status).body(response);
  }
}
