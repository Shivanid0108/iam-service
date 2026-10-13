package com.shivani.iamservice;

import java.util.Map; // used to build the {"token": "..."} body in the login response

import org.springframework.http.HttpStatus; // named HTTP status codes (HttpStatus.CREATED -> 201)
import org.springframework.http.ResponseEntity; // a full HTTP response: status code, headers and body together
import org.springframework.web.bind.annotation.PostMapping; // routes POST requests to a method
import org.springframework.web.bind.annotation.RequestBody; // binds the incoming JSON to a Java object
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // this class receives HTTP requests and sends back data (usually JSON). Marks a
				// web entry point
@RequestMapping("/api/auth") // shared URL prefix: both endpoints below live at /api/auth/register and
								// /api/auth/login
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

	// POST /api/auth/login (public). Returns 200 with {"token": "<JWT>"}.
	// If the credentials are wrong, login() throws BadCredentialsException,
	// GlobalExceptionHandler turns it into a 401, and the return line never runs.
	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
		String token = authService.login(request.getEmail(), request.getPassword());
		// Map.of("token", token) is a single key-value pair, which Spring
		// converts to JSON before sending it to the client.
		return ResponseEntity.ok(Map.of("token", token));
	}

}