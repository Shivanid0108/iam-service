package com.shivani.iamservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.shivani.iamservice.dto.ChangeRoleRequest;
import com.shivani.iamservice.service.UserService;

@RestController // returns JSON, not web pages
public class AdminController {

	// Thin controller: receives the request and passes it to the service.
	private final UserService userService;

	// Constructor injection: Spring hands in a ready UserService.
	public AdminController(UserService userService) {
		this.userService = userService;
	}

	// GET /api/admin/ping: a test endpoint. Only ADMIN tokens get past the door
	// rule in SecurityConfig, so no role check is needed here.
	@GetMapping("/api/admin/ping")
	public String ping() {
		return "admin ok";
	}

	// PATCH /api/admin/users/{id}/role (ADMIN only). Body: {"role": "MANAGER"}.
	// PATCH = change one part of something that already exists.
	// @PathVariable takes {id} from the URL. @RequestBody turns the JSON into an
	// object.
	// Unknown role (like "BOSS") -> Spring returns 400. Unknown user -> 404.
	@PatchMapping("/api/admin/users/{id}/role")
	public ResponseEntity<Void> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest request) {
		userService.changeRole(id, request.role());
		// 204 No Content: "done, nothing to send back".
		return ResponseEntity.noContent().build();
	}

}