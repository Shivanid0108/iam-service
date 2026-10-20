package com.shivani.iamservice.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shivani.iamservice.dto.TokenResponse;
import com.shivani.iamservice.model.RefreshToken;
import com.shivani.iamservice.model.Role;
import com.shivani.iamservice.model.User;
import com.shivani.iamservice.repository.UserRepository;
import com.shivani.iamservice.security.JwtService;

// @Service: tells Spring "this class contains business logic — create
// one instance of it automatically, and make it available to hand to
// other classes that need it" (like AuthController will).
// This is the same underlying mechanism as @Entity marking a class as
// a table — just a different kind of "stamp" for a different purpose.
@Service
public class AuthService {

	// This class doesn't do database work itself — it asks
	// UserRepository to do it. That's why it's held as a field here.
	private final UserRepository userRepository;

	// Handles hashing a raw password (encode) and checking a raw
	// password against a stored hash (matches) — this is BCrypt,
	// accessed through the general PasswordEncoder interface rather
	// than the specific BCrypt class, so it could be swapped later
	// (e.g. for Argon2) without changing this class at all.
	private final PasswordEncoder passwordEncoder;

	// Creates the signed JWT (the access token, with the user's id and role).
	private final JwtService jwtService;

	// Loads a user by id. Used by refresh() to get the user's CURRENT role from the
	// database (instead of trusting the old token).
	private final UserService userService;

	// Creates, checks and revokes refresh tokens. AuthService never touches the
	// refresh_tokens table directly; this service owns it.
	private final RefreshTokenService refreshTokenService;

	// Constructor: Spring calls this automatically when it creates
	// an AuthService, and it AUTOMATICALLY supplies working instances
	// of all five parameters below — you never write "new AuthService(...)"
	// anywhere yourself. This is called "dependency injection."
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			UserService userService, RefreshTokenService refreshTokenService) {
		// Each line just stores the object Spring handed us into
		// the matching field, so the rest of this class can use them.
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.userService = userService;
		this.refreshTokenService = refreshTokenService;

	}

	// Email must look like name@domain.tld. Null is rejected here, so no
	// NullPointerException later.
	private boolean isValidEmail(String email) {
		if (email == null)
			return false;

		return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	}

	// Password needs 8+ characters with an uppercase letter, a lowercase letter,
	// a digit and one of the allowed specials: # & @
	private boolean isValidPassword(String password) {
		if (password == null || password.length() < 8) {
			return false;
		}

		boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
		for (char c : password.toCharArray()) {
			if (Character.isUpperCase(c))
				hasUpper = true;
			else if (Character.isLowerCase(c))
				hasLower = true;
			else if (Character.isDigit(c))
				hasDigit = true;
			else if (c == '#' || c == '&' || c == '@')
				hasSpecial = true;
		}
		return hasUpper && hasLower && hasDigit && hasSpecial;
	}

	// Cheap checks first (format), then the database check (duplicate email).
	// The role is always EMPLOYEE and is set here, never taken from the client's
	// request, so nobody can register themselves as an Admin (mass assignment).
	// The duplicate check can lose a race between two simultaneous requests,
	// so the UNIQUE constraint on User.email is the real safety net.
	public void register(String email, String password) {
		if (!isValidEmail(email)) {
			throw new IllegalArgumentException("Invalid email");
		}

		if (!isValidPassword(password)) {
			throw new IllegalArgumentException("Password does not meet requirements");
		}

		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already registered");
		}

		String hashedPassword = passwordEncoder.encode(password);
		User user = new User(email, hashedPassword, Role.EMPLOYEE);
		userRepository.save(user);
	}

	// Both failures (unknown email, wrong password) throw the SAME message, so an
	// attacker can't tell which emails are registered (user enumeration
	// protection).
	// BadCredentialsException is mapped to 401 in GlobalExceptionHandler.
	// On success the user gets TWO tokens:
	// - access token: a JWT that opens doors, lasts 15 minutes
	// - refresh token: a random value used only to get a new pair, lasts 7 days
	public TokenResponse login(String email, String password) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getHashedPassword())) {
			throw new BadCredentialsException("Invalid email or password");
		}
		String accessToken = jwtService.generateToken(user);
		String refreshToken = refreshTokenService.createRefreshToken(user.getId());

		return new TokenResponse(accessToken, refreshToken);
	}

	// Trades a valid refresh token for a brand-new pair (rotation).
	// Order matters:
	// 1. check the old token (unknown, used or expired -> 401 and we stop here)
	// 2. revoke it, so it can never be used a second time
	// 3. load the user from the database, so the new access token carries their
	// CURRENT role (this is how a role change reaches the user)
	// 4. create the new access token and the new refresh token
	public TokenResponse refresh(String rawRefreshToken) {

		RefreshToken oldToken = refreshTokenService.verifyRefreshToken(rawRefreshToken);

		refreshTokenService.revoke(oldToken);

		User user = userService.getById(oldToken.getUserId());

		String accessToken = jwtService.generateToken(user);
		String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

		return new TokenResponse(accessToken, newRefreshToken);
	}

	// Kills the refresh token so no new access tokens can be issued with it.
	// Never throws: an unknown or already-used token is simply ignored, so the
	// client always sees success and learns nothing about which tokens exist.
	// The current access token still works until it expires (stateless JWT).
	public void logout(String rawRefreshToken) {
		refreshTokenService.revokeByRawToken(rawRefreshToken);
	}
}