package com.shivani.iamservice.exception;

public class TooManyRequestsException extends RuntimeException {

	public TooManyRequestsException(String message) {
		super(message);
	}
}
