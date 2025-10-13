package com.hs.lab2.groupeventservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionResolver {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessage> userNotFoundException(UserNotFoundException exception) {
        log.warn("UserNotFoundException: ", exception);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(exception.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorMessage> eventNotFoundException(EventNotFoundException exception) {
        log.warn("EventNotFoundException: ", exception);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(exception.getMessage()));
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorMessage> handleUserServiceUnavailable(UserServiceUnavailableException exception) {
        log.warn("UserServiceUnavailableException: ", exception);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorMessage(exception.getMessage()));
    }
    @ExceptionHandler(EventServiceUnavailableException.class)
    public ResponseEntity<ErrorMessage> handleEventServiceUnavailable(EventServiceUnavailableException exception) {
        log.warn("EventServiceUnavailableException: ", exception);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorMessage(exception.getMessage()));
    }
}
