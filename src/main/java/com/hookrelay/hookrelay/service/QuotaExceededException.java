package com.hookrelay.hookrelay.service;

public class QuotaExceededException extends RuntimeException {

    private final int status;

    public QuotaExceededException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}