package com.shivani.iamservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivani.iamservice.dto.UserResponse;
import com.shivani.iamservice.model.User;
import com.shivani.iamservice.service.UserService;

@RestController // return values become JSON, not web pages
@RequestMapping("/api/users") // shared URL prefix for every method in this class
public class UserController {

	// Thin controller: it receives the request and delegates to the service.
	// Constructor injection: Spring hands in a ready UserService.
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	// GET /api/users/me: a protected endpoint (any valid token, any role).
	// Spring passes in the Authentication object that JwtAuthFilter set,
	// so no client input is needed to know who is asking.
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(Authentication authentication) {
		// The user id comes from the verified token (its subject), never from the
		// request, so a client can't ask for someone else's profile.
		Long userId = Long.valueOf(authentication.getName());
		User user = userService.getById(userId);
		// Return a UserResponse (the DTO), never the User entity,
		// so hashedPassword can't leak.
		return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), user.getRole()));
	}

}