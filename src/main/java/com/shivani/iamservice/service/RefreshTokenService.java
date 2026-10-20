package com.shivani.iamservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.shivani.iamservice.model.RefreshToken;
import com.shivani.iamservice.repository.RefreshTokenRepository;

// Business logic for refresh tokens: create, check and revoke them.
@Service
public class RefreshTokenService {

	// Does the database work for the refresh_tokens table.
	private final RefreshTokenRepository refreshTokenRepository;

	// How long a refresh token lives, in milliseconds (604800000 = 7 days).
	private final long refreshExpirationMs;

	// Constructor injection: Spring hands in the repository.
	// @Value reads jwt.refresh-expiration from application.properties; the part
	// after the colon is the default if the property is missing.
	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
			@Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	// Creates a new token for a user and returns the RAW token (the voucher
	// itself).
	// This is the only moment it exists in readable form: we save only its hash.
	public String createRefreshToken(Long userId) {
		String rawToken = generateRawToken();
		LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs));

		RefreshToken refreshToken = new RefreshToken(userId, hash(rawToken), expiry);
		refreshTokenRepository.save(refreshToken);

		return rawToken;

	}

	// 32 random bytes from SecureRandom (cryptographically strong, unlike Random),
	// written as text that is safe to put in JSON and URLs (43 characters).
	private String generateRawToken() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// One-way fingerprint of the token (64 hex characters). SHA-256, not BCrypt,
	// because we must look the row up BY its hash: the same input always gives the
	// same output. The token is already 256 bits of randomness, so it can't be
	// guessed the way a password can, and a slow hash isn't needed.
	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed); // bytes -> readable hex text
		} catch (NoSuchAlgorithmException e) {
			// Every Java install has SHA-256, so this "can't happen".
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	// Marks a token as dead and saves the change (without save, the database
	// would still say it is valid).
	public void revoke(RefreshToken token) {
		token.revoke();
		refreshTokenRepository.save(token);
	}

	// Used by /refresh. Returns the token row if it passes all three gates,
	// otherwise throws. All three failures give the SAME message, so an attacker
	// can't tell "doesn't exist" from "already used" from "expired".
	public RefreshToken verifyRefreshToken(String rawToken) {
		String hashed = hash(rawToken);

		// Gate 1: does a row with this hash exist?
		RefreshToken token = refreshTokenRepository.findByHashedToken(hashed)
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

		// Gate 2: was it already used (rotation) or logged out?
		if (token.isRevoked()) {
			throw new BadCredentialsException("Invalid refresh token");
		}

		// Gate 3: did its 7 days run out?
		if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
			throw new BadCredentialsException("Invalid refresh token");
		}

		return token;
	}

	// Used by /logout. Unlike verifyRefreshToken it never throws: if the token is
	// unknown there is nothing to do, and logout still succeeds.
	// ifPresent runs the revoke only when a matching row was found.
	public void revokeByRawToken(String rawToken) {
		refreshTokenRepository.findByHashedToken(hash(rawToken)).ifPresent(token -> revoke(token));
	}
}