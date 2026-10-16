package com.shivani.iamservice.dto;

// The shape of the JSON a client sends to POST /api/auth/register.
// There is deliberately NO role field: even if a client adds "role": "ADMIN"
// to the JSON, it is ignored, because there is nowhere for it to land.
// The role is always set to EMPLOYEE inside AuthService.
public class RegisterRequest {

	private String email;
	private String password;

	// No-args constructor: needed here for a completely different reason
	// than User's — this one exists so Spring can build the object from
	// incoming JSON (Jackson, the library Spring uses for JSON conversion,
	// constructs it empty first, then sets each field).
	public RegisterRequest() {
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