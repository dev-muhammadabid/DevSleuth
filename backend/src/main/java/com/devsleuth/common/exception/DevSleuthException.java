package com.devsleuth.common.exception;

import org.springframework.http.HttpStatus;

public class DevSleuthException extends RuntimeException {

    private final HttpStatus status;

    public DevSleuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public DevSleuthException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HttpStatus getStatus() { return status; }
}
