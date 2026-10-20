package com.shivani.iamservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;

import com.shivani.iamservice.model.RefreshToken;
import com.shivani.iamservice.repository.RefreshTokenRepository;
import com.shivani.iamservice.service.RefreshTokenService;

public class RefreshTokenServiceTest {

	// The fake database helper (a mock) and the real service we are testing.
	private RefreshTokenRepository refreshTokenRepository;
	private RefreshTokenService refreshTokenService;

	// Runs before EVERY test, so each test starts with a fresh, clean setup.
	@BeforeEach
	void setUp() {
		refreshTokenRepository = mock(RefreshTokenRepository.class);
		// 604800000 ms = 7 days: the token lifetime passed into the real service.
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, 604800000L);
	}

	// GATE 1: the token doesn't exist in the database -> rejected.
	@Test
	void verifyRefreshToken_unknownToken_throwsBadCredentials() {
		// Arrange: whatever hash is looked up, the fake database finds nothing.
		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.empty());
		// Act + Assert: the real call goes INSIDE the lambda so assertThrows can catch
		// it.
		assertThrows(BadCredentialsException.class, () -> refreshTokenService.verifyRefreshToken("anything"));
	}

	// GATE 2: the token exists but was already used or logged out -> rejected.
	@Test
	void verifyRefreshToken_revokedToken_throwsBadCredentials() {
		// Arrange: a row that is NOT expired (tomorrow) but IS revoked,
		// so only the revoked check can reject it.
		RefreshToken token = new RefreshToken(1L, "some-hash", LocalDateTime.now().plusDays(1));
		token.revoke();

		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(token));
		assertThrows(BadCredentialsException.class, () -> refreshTokenService.verifyRefreshToken("anything"));
	}

	// GATE 3: the token exists and is not revoked, but its 7 days ran out ->
	// rejected.
	@Test
	void verifyRefreshToken_expiredToken_throwsBadCredentials() {
		// Arrange: expiry was yesterday, never revoked,
		// so only the expiry check can reject it.
		RefreshToken token = new RefreshToken(1L, "some-hash", LocalDateTime.now().minusDays(1));

		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(token));
		assertThrows(BadCredentialsException.class, () -> refreshTokenService.verifyRefreshToken("anything"));
	}

	// HAPPY PATH: exists, not revoked, not expired -> the row is returned.
	@Test
	void verifyRefreshToken_validToken_returnsToken() {
		RefreshToken token = new RefreshToken(1L, "some-hash", LocalDateTime.now().plusDays(6));

		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(token));
		RefreshToken result = refreshTokenService.verifyRefreshToken("anything");
		// Here the outcome is a returned value, not an exception.
		assertEquals(token, result);
	}

	// LOGOUT, real token: it must be marked revoked AND the change must be saved.
	@Test
	void revokeByRawToken_existingToken_marksRevoked() {
		RefreshToken token = new RefreshToken(1L, "some-hash", LocalDateTime.now().plusDays(1));

		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(token));
		refreshTokenService.revokeByRawToken("anything");

		// Catches "forgot to call revoke()": the flag must have flipped.
		assertTrue(token.isRevoked());
		// Catches "forgot to save": without save, the database would still say valid.
		verify(refreshTokenRepository).save(token);
	}

	// LOGOUT, unknown token: nothing to revoke, so it quietly does nothing.
	// No assertThrows here: if the call threw, the test would fail by itself.
	@Test
	void revokeByRawToken_unknownToken_doesNothing() {
		when(refreshTokenRepository.findByHashedToken(anyString())).thenReturn(Optional.empty());

		refreshTokenService.revokeByRawToken("anything");

		// never() = save must have been called ZERO times.
		verify(refreshTokenRepository, never()).save(any());
	}

	// SECURITY: the database must hold the HASH of the token, never the raw token.
	@Test
	void createRefreshToken_savesHashedTokenNotRawToken() {
		// Act: the service returns the RAW token (sent to the client once).
		String rawToken = refreshTokenService.createRefreshToken(1L);

		// The captor grabs the exact object the service passed to save(...).
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		RefreshToken saved = captor.getValue();

		assertNotNull(rawToken);
		// The saved value must differ from the raw token...
		assertNotEquals(rawToken, saved.getHashedToken());
		// ...and look like a SHA-256 hash: 32 bytes x 2 hex characters = 64
		// (a raw token is only 43 characters).
		assertEquals(64, saved.getHashedToken().length());
		assertEquals(1L, saved.getUserId());
		assertFalse(saved.isRevoked()); // a new token starts valid
		assertTrue(saved.getExpiryDate().isAfter(LocalDateTime.now())); // and expires in the future
	}
}
