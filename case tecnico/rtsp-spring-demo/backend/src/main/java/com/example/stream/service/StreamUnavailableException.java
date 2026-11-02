package com.example.stream.service;

public class StreamUnavailableException extends RuntimeException {
    public StreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}