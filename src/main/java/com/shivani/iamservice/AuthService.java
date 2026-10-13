package com.shivani.iamservice;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

	// Creates the signed JWT (with the user's id and role) after a successful
	// login.
	private final JwtService jwtService;

	// Constructor: Spring calls this automatically when it creates
	// an AuthService, and it AUTOMATICALLY supplies working instances
	// of all three parameters below — you never write "new AuthService(...)"
	// anywhere yourself. This is called "dependency injection."
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		// Each line just stores the object Spring handed us into
		// the matching field, so the rest of this class can use them.
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
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
	public String login(String email, String password) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getHashedPassword())) {
			throw new BadCredentialsException("Invalid email or password");
		}

		return jwtService.generateToken(user);
	}
}