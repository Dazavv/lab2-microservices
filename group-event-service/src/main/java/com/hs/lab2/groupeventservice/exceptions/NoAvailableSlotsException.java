package com.hs.lab2.groupeventservice.exceptions;

public class NoAvailableSlotsException extends RuntimeException {
    public NoAvailableSlotsException(String message) {
        super(message);
    }
}
