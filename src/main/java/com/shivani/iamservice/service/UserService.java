package com.shivani.iamservice.service;

import java.util.NoSuchElementException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.shivani.iamservice.model.Role;
import com.shivani.iamservice.model.User;
import com.shivani.iamservice.repository.UserRepository;

// Business logic for users: looking them up and changing their role.
// Controllers ask this class, and this class asks the repository (the database layer).
@Service
public class UserService {

	// Does the actual database work; this class only decides what to do with the
	// result.
	private final UserRepository userRepository;

	// Constructor injection: Spring hands in a working UserRepository.
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Finds a user by id. The id comes from a verified token (via UserController
	// or the refresh flow), so a missing user means the token is valid but the
	// account no longer exists (for example, it was deleted). We treat that as an
	// authentication failure, which GlobalExceptionHandler turns into a 401.
	public User getById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new BadCredentialsException("Authentication failed"));
	}

	// Used by the admin endpoint. Finds the user, changes the role, saves.
	// Here a missing user is NOT an authentication problem (the admin is fine,
	// the id they asked about doesn't exist), so it throws NoSuchElementException,
	// which GlobalExceptionHandler turns into a 404.
	// save() is what actually writes the change to the database.
	public User changeRole(Long id, Role newRole) {

		User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));

		user.changeRole(newRole);
		return userRepository.save(user);

	}

}