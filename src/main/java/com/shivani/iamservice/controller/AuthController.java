package com.shivani.iamservice.controller;

import org.springframework.http.HttpStatus; // named HTTP status codes (HttpStatus.CREATED -> 201)
import org.springframework.http.ResponseEntity; // a full HTTP response: status code, headers and body together
import org.springframework.web.bind.annotation.PostMapping; // routes POST requests to a method
import org.springframework.web.bind.annotation.RequestBody; // binds the incoming JSON to a Java object
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivani.iamservice.dto.LoginRequest;
import com.shivani.iamservice.dto.RefreshRequest;
import com.shivani.iamservice.dto.RegisterRequest;
import com.shivani.iamservice.dto.TokenResponse;
import com.shivani.iamservice.service.AuthService;

@RestController // this class receives HTTP requests and sends back data (usually JSON). Marks a
				// web entry point
@RequestMapping("/api/auth") // // shared URL prefix: register, login, refresh and logout all start with
								// /api/auth
public class AuthController {

	// Thin controller: it receives the request, delegates to the service,
	// and shapes the response. No business logic lives here.
	private final AuthService authService;

	// Constructor injection: Spring builds an AuthService somewhere else and
	// hands a working instance to this constructor automatically.
	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	// POST /api/auth/register (public). Always creates an EMPLOYEE: the role is
	// decided inside AuthService, never taken from the request.
	// ResponseEntity<Void> means the response has a status code but no body.
	// @RequestBody takes the incoming JSON and converts it into a RegisterRequest.
	@PostMapping("/register")
	public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
		// The controller doesn't know or care how registration works (validation,
		// hashing, saving): it just delegates to the service.
		authService.register(request.getEmail(), request.getPassword());
		// 201 Created with no body: "your request worked and something new now exists".
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	// POST /api/auth/login (public). Returns both tokens:
	// {"accessToken": "...", "refreshToken": "..."}.
	// Wrong credentials -> BadCredentialsException -> 401, and the return line
	// never runs.
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
		TokenResponse tokens = authService.login(request.getEmail(), request.getPassword());
		return ResponseEntity.ok(tokens);
	}

	// POST /api/auth/refresh (public). Send the refresh token, get a NEW pair back.
	// The old refresh token stops working (it can be used only once).
	// Unknown, used or expired token -> 401.
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
		return ResponseEntity.ok(authService.refresh(request.refreshToken()));
	}

	// POST /api/auth/logout (public). Kills the refresh token.
	// Always returns 204, even for a token that doesn't exist.
	// The current access token still works until it expires (15 minutes).
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}
}