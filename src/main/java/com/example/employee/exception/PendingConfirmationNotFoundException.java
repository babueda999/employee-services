package com.example.employee.exception;

public class PendingConfirmationNotFoundException
        extends RuntimeException {

    public PendingConfirmationNotFoundException(String message) {
        super(message);
    }
}
