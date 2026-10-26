package com.shivani.iamservice.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.shivani.iamservice.exception.TooManyRequestsException;

@Service
public class RateLimitService {

	private static final int MAX_ATTEMPTS = 5; // allowed tries
	private static final Duration WINDOW = Duration.ofSeconds(60); // per minute

	private final StringRedisTemplate redisTemplate; // helper to talk to Redis

	public RateLimitService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// CALL THIS BEFORE EVERY LOGIN ATTEMPT
	public void checkLoginAttempt(String email) {
		String key = keyFor(email);

		// ADD ONE TO THE COUNTER AND GET THE NEW NUMBER
		Long count = redisTemplate.opsForValue().increment(key);

		// FIRST ATTEMPT IN THIS WINDOW : START THE 60 SECOND TIMER
		if (count != null && count == 1) {
			redisTemplate.expire(key, WINDOW);
		}

		// TOO MANY TRIES: STOP HERE
		if (count != null && count > MAX_ATTEMPTS) {
			throw new TooManyRequestsException("Too many login attempts. Try again later.");
		}
	}

	// CALL THIS AFTER A SUCCESSFUL LOGIN SO THE USER STARTS FRESH
	public void resetLoginAttempts(String email) {
		redisTemplate.delete(keyFor(email));
	}

	// one place that builds the key to avoid mistyping
	private String keyFor(String email) {
		return "login:attempts:" + email.toLowerCase();
	}

}
