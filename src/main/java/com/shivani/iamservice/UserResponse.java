package com.shivani.iamservice;

import java.time.LocalDateTime;

// A record is an immutable data carrier: Java generates the constructor,
// getters (id(), email(), createdAt(), role()), equals, hashCode and toString.
//
// This is the RESPONSE DTO for GET /api/users/me. It lists only the fields that
// are safe to send to a client. User (the entity) also holds hashedPassword,
// which is not here, so it can never leak by accident. role is included so a
// client can show the user's level (and hide admin-only buttons, which is only
// cosmetic: the server still enforces access in SecurityConfig).
public record UserResponse(Long id, String email, LocalDateTime createdAt, Role role) {

}