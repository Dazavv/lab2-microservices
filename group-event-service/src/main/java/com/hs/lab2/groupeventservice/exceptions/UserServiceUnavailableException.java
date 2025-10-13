package com.hs.lab2.groupeventservice.exceptions;

public class UserServiceUnavailableException extends RuntimeException{
    public UserServiceUnavailableException(String s) {
        super(s);
    }
}
