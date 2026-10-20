package com.shivani.iamservice.exception;

import java.util.Map; // builds the {"error": "..."} JSON body
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus; // named HTTP status codes (HttpStatus.BAD_REQUEST -> 400)
import org.springframework.http.ResponseEntity; // status code + body together
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler; // "when this exception is thrown, run this method"
import org.springframework.web.bind.annotation.RestControllerAdvice; // applies these handlers to ALL controllers

// One central place that turns exceptions thrown anywhere (services, controllers)
// into clean HTTP responses, so no controller needs its own try/catch.
@RestControllerAdvice
public class GlobalExceptionHandler {

	// Thrown by AuthService.register for bad input: invalid email, weak password,
	// or an email that is already registered. The client made a mistake -> 400.
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
	}

	// Thrown when credentials or tokens are wrong: login (bad email or password),
	// refresh (unknown, used or expired refresh token) and UserService.getById.
	// The message never says WHICH part failed, so an attacker learns nothing ->
	// 401.
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleUnauthorized(BadCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	}

	// Thrown by UserService.changeRole when the user id doesn't exist.
	// The thing asked for isn't there -> 404.
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

}