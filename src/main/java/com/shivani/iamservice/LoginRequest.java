package com.shivani.iamservice;

// The shape of the JSON a client sends to POST /api/auth/login.
public class LoginRequest {

	private String email;
	private String password;

	// No-args constructor: needed here for a completely different reason
	// than User's — this one exists so Spring can build the object from
	// incoming JSON (Jackson, the library Spring uses for JSON conversion,
	// constructs it empty first, then sets each field).
	public LoginRequest() {
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	// Setters ARE needed here, unlike User — Jackson uses these to fill
	// in the fields after construction, since JSON deserialization
	// doesn't know our parameterized constructor exists.
	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}