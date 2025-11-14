package com.example.exception;

import com.example.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxUploadFileSize;

    // 404 – Employee not found
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 409 – Duplicate email
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEmailException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 400 – Invalid or inconsistent data (business-level)
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidDataException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400 – Any other illegal argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ===== FILE-RELATED EXCEPTIONS =====

    // 400 – File is invalid (wrong type, too large according to service-level rules, etc.)
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 404 – File not found in storage
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 500 – Storage error (I/O problems, directory creation failures, etc.)
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // 413 – File exceeds configured multipart limit
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, WebRequest req) {
        String message = "Uploaded file is too large. Maximum allowed size is " + maxUploadFileSize + ".";
        ErrorResponse body = new ErrorResponse(message, HttpStatus.PAYLOAD_TOO_LARGE.value(), req.getDescription(false));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    // 500 – Fallback for unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest req) {
        ErrorResponse body = new ErrorResponse(
                "Internal server error: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                req.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
