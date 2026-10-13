package com.shivani.iamservice;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

// Business logic for looking up users. UserController asks this class
// for a user, and this class asks the repository (the database layer).
@Service
public class UserService {

	// Does the actual database work; this class only decides what to do with the
	// result.
	private final UserRepository userRepository;

	// Constructor injection: Spring hands in a working UserRepository.
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Finds a user by id. The id comes from the verified token (via
	// UserController),
	// so a missing user means the token is valid but the account no longer exists
	// (for example, it was deleted). We treat that as an authentication failure,
	// which GlobalExceptionHandler turns into a 401.
	public User getById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new BadCredentialsException("Authentication failed"));
	}
}