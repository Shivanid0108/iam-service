package com.shivani.iamservice.repository;

import java.util.Optional; // "a token, or nothing" instead of risking a null

import org.springframework.data.jpa.repository.JpaRepository; // ready-made save, findById, findAll, delete

import com.shivani.iamservice.model.RefreshToken;

// An interface has no code: Spring generates the implementation at startup.
// <RefreshToken, Long> = it manages RefreshToken rows, and their id is a Long.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// Derived query: Spring reads the method name and builds the SQL itself
	// (SELECT ... FROM refresh_tokens WHERE hashed_token = ?).
	// We look tokens up by their HASH, because the raw token is never stored.
	// hashedToken is unique, so there is at most one match (hence Optional).
	Optional<RefreshToken> findByHashedToken(String hashedToken);

}