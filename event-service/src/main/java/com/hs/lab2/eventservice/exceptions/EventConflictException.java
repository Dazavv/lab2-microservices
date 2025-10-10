package com.hs.lab2.eventservice.exceptions;

public class EventConflictException extends RuntimeException {
    public EventConflictException(String message) {
        super(message);
    }
}
