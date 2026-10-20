package com.shivani.iamservice.dto;

//The shape of the JSON a client sends to POST /api/auth/refresh and /api/auth/logout.
//Example body: {"refreshToken": "<the long random value from login>"}
//A record, so Jackson fills it from the JSON automatically
//(no setters or empty constructor needed, unlike the older DTO classes).
public record RefreshRequest(String refreshToken) {

}