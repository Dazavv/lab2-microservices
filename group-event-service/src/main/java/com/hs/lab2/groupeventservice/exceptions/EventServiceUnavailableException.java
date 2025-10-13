package com.hs.lab2.groupeventservice.exceptions;

public class EventServiceUnavailableException extends RuntimeException{
    public EventServiceUnavailableException(String s) {
        super(s);
    }
}
