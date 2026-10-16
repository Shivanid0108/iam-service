package com.shivani.iamservice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final long refreshExpirationMs;

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
			@Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	public String createRefreshToken(Long userId) {
		String rawToken = generateRawToken();
		LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs));

		RefreshToken refreshToken = new RefreshToken(userId, hash(rawToken), expiry);
		refreshTokenRepository.save(refreshToken);

		return rawToken;

	}

	private String generateRawToken() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
