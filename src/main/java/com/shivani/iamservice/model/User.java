package com.shivani.iamservice.model;

import java.time.LocalDateTime; // a class for representing date and time

// JPA annotations that map this class to a database table
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // tells Hibernate/JPA to map this class to a table in the database.
		// Specifically for persistence: data that must survive after the program
		// stops running and be saved somewhere permanent -> PostgreSQL
@Table(name = "users") // without @Table, Hibernate would name the table "user", which is a reserved
						// word in PostgreSQL. @Table tells Hibernate what to call it explicitly.

public class User {

	@Id // marks this field as the primary key (the unique identifier of each row)
	@GeneratedValue(strategy = GenerationType.IDENTITY) // the DB generates this value. PostgreSQL keeps an internal
														// counter called a sequence.
	private Long id;

	@Column(unique = true, nullable = false) // no 2 emails can be the same, and email cannot be null
	private String email;

	@Column(nullable = false) // the hashed password cannot be null
	private String hashedPassword;

	private LocalDateTime createdAt;

	// STRING stores the word "ADMIN" in the database. The default (ORDINAL)
	// stores 0, 1, 2, so reordering the enum would silently change everyone's role.
	@Enumerated(EnumType.STRING)
	@Column(name = "user_role", nullable = false)
	private Role role;

	protected User() {
	} // No-args constructor: NOT meant for our own code to call.
		// Hibernate needs this to build a blank User object first,
		// then fill in each field afterward via reflection, when
		// reconstructing a row read back from the database.
		// protected (not private) so Hibernate can still reach it.

	public User(String email, String hashedPassword, Role role) { // The real constructor: this is what OUR code
																	// actually uses when creating a brand-new user.
		// Notice "id" isn't a parameter here at all: the database assigns it, not you.
		// role is passed in by our own code (AuthService always passes EMPLOYEE),
		// never taken from the client's request.
		this.email = email;
		this.hashedPassword = hashedPassword;
		this.role = role;
		this.createdAt = LocalDateTime.now();
	}

	// No setters for id/email/hashedPassword/createdAt/role
	// encapsulation discipline: once set,
	// these fields shouldn't be mutated from outside the class.

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public Role getRole() {
		return role;
	}

}