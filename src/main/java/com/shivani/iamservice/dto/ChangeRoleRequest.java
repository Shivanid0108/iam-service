package com.shivani.iamservice.dto;

import com.shivani.iamservice.model.Role;

//The shape of the JSON an admin sends to PATCH /api/admin/users/{id}/role.
//Example body: {"role": "MANAGER"}
//A record is a short way to write a plain data class: Java generates the
//constructor, the getter (role()), equals, hashCode and toString.
//The field type is Role (the enum), not String: Spring converts "MANAGER" into
//Role.MANAGER, and a value that isn't in the enum (like "BOSS") is rejected with 400
//before our code runs.
public record ChangeRoleRequest(Role role) {

}
