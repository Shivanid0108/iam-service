package com.shivani.iamservice.repository;

import java.util.Optional; // handles the "this might not exist" case instead of risking a null

import org.springframework.data.jpa.repository.JpaRepository; // a ready-made interface that gives us
// pre-built database operations: save, findById, findAll, delete

import com.shivani.iamservice.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	// An interface has no code: it only says which methods must exist.
	// We never write an implementation. Spring generates one at startup (a proxy).
	//
	// <User, Long> answers 2 questions:
	// 1. Which entity does this repository manage? -> User
	// 2. What type is that entity's primary key? -> Long

	// Derived query: Spring reads the method name and builds the SQL itself
	// (SELECT ... FROM users WHERE email = ?). The value is bound as a parameter,
	// so there is no SQL injection. Optional means "a user, or nothing".
	Optional<User> findByEmail(String email);

}