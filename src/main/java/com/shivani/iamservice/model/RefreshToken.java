package com.shivani.iamservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // maps this class to a database table
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // the database assigns the id (1, 2, 3...)
	private Long id;

	// Which user this token belongs to. A plain Long (not a link to the User
	// object),
	// because we only ever need the id.
	@Column(nullable = false)
	private Long userId;

	// SHA-256 hash of the real token. The raw token is NEVER stored, so a leaked
	// database can't be used to log in. unique = true also gives a fast lookup
	// (index).
	@Column(unique = true, nullable = false)
	private String hashedToken;

	// After this moment the token is dead, even if it was never used.
	@Column(nullable = false)
	private LocalDateTime expiryDate;

	// true = already used (rotation) or logged out. A revoked token is never
	// accepted.
	@Column(nullable = false)
	private boolean revoked;

	// Needed by Hibernate to build objects from database rows.
	// protected so our own code can't create a half-empty token by accident.
	protected RefreshToken() {
	}

	// The constructor our code uses: a new token always starts as not revoked.
	public RefreshToken(Long userId, String hashedToken, LocalDateTime expiryDate) {
		this.userId = userId;
		this.hashedToken = hashedToken;
		this.expiryDate = expiryDate;
		this.revoked = false;
	}

	// The only change allowed is false -> true, and it can never be undone.
	// That's why there's no setRevoked().
	public void revoke() {
		this.revoked = true;
	}

	// Getters: read-only access to the fields.
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
