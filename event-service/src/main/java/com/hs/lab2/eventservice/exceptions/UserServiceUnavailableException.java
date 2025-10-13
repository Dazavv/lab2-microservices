package com.hs.lab2.eventservice.exceptions;

public class UserServiceUnavailableException extends RuntimeException{
    public UserServiceUnavailableException(String s) {
        super(s);
    }
}
