package com.bookshelf.books;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps book-list failures to the project's standard error body with generic messages. */
@RestControllerAdvice(basePackageClasses = BookExceptionHandler.class)
public class BookExceptionHandler {

    @ExceptionHandler(DuplicateBookException.class)
    public ResponseEntity<Map<String, Object>> duplicate(HttpServletRequest request) {
        return body(HttpStatus.CONFLICT, "Book is already in your lists", request);
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, "Book not found", request);
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
