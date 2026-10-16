package com.shivani.iamservice.security;

import java.util.Date; // when the token was issued / expires

import javax.crypto.SecretKey; // the type of a cryptographic key object

import org.springframework.beans.factory.annotation.Value; // lets @Value("${jwt.secret}") read a value from application.properties
import org.springframework.stereotype.Service; // makes JwtService a Spring-managed class

import com.shivani.iamservice.model.User;

import io.jsonwebtoken.Claims; // everything printed on the token: subject, role, dates
import io.jsonwebtoken.Jwts; // main entry point of the jjwt library: builds and reads tokens
import io.jsonwebtoken.security.Keys; // turns the plain secret string into a cryptographic key object

@Service
public class JwtService {

	// The secret comes from the JWT_SECRET environment variable (via
	// application.properties), so it never appears in source control.
	@Value("${jwt.secret}")
	private String secretKey;

	// Token lifetime in milliseconds (900000 = 15 minutes). A short life limits
	// the damage if a token is stolen, because tokens can't be revoked early.
	@Value("${jwt.expiration}")
	private long expirationTime;

	// One place that builds the key, used both to sign and to verify.
	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	// Builds and signs the token. The payload is only encoded, NOT encrypted,
	// so never put secrets in it. The signature makes tampering detectable.
	public String generateToken(User user) {
		return Jwts.builder().subject(String.valueOf(user.getId())) // who: the user id
				.claim("role", user.getRole().name()) // what level: EMPLOYEE, MANAGER or ADMIN
				.issuedAt(new Date()) // timestamp baked into the token
				.expiration(new Date(System.currentTimeMillis() + expirationTime)) // when it stops working
				.signWith(getKey()) // the signing step that makes tampering detectable
				.compact(); // packages everything into the final JWT string
	}

	// Verifies the signature and expiry ONCE and returns everything inside the
	// token. Throws an exception if the token is fake, tampered with or expired,
	// which JwtAuthFilter catches and treats as "not logged in".
	public Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
	}

}