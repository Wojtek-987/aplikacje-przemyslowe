package com.example.exception;

/**
 * File NOT found in storage. Intentionally separate from java.io.FileNotFoundException.
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
