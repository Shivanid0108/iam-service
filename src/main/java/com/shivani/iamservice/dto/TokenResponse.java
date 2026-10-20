package com.shivani.iamservice.dto;

//The shape of the JSON the server sends back from login and refresh:
//{"accessToken": "...", "refreshToken": "..."}
//accessToken = a JWT that opens doors, expires in 15 minutes.
//refreshToken = a random value used only to get a new pair, lasts 7 days.
//The raw refresh token is shown here once; the database keeps only its hash.
public record TokenResponse(String accessToken, String refreshToken) {

}