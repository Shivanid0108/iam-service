package com.shivani.iamservice.model;

//The three levels of access. Stored in the database as text ("ADMIN") because
//User uses @Enumerated(EnumType.STRING).
//Everyone who registers starts as EMPLOYEE; only an ADMIN can change a role.
public enum Role {
	EMPLOYEE, MANAGER, ADMIN
}
