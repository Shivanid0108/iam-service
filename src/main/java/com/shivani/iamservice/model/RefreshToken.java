package com.shivani.iamservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")

public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(unique = true, nullable = false)
	private String hashedToken;

	@Column(nullable = false)
	private LocalDateTime expiryDate;

	@Column(nullable = false)
	private boolean revoked;

	protected RefreshToken() {
	}

	public RefreshToken(Long userId, String hashedToken, LocalDateTime expiryDate) {
		this.userId = userId;
		this.hashedToken = hashedToken;
		this.expiryDate = expiryDate;
		this.revoked = false;
	}

	public void revoke() {
		this.revoked = true;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getHashedToken() {
		return hashedToken;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public LocalDateTime getExpiryDate() {
		return expiryDate;
	}

}
