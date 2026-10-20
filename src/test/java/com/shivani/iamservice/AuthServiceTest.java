package com.shivani.iamservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shivani.iamservice.dto.TokenResponse;
import com.shivani.iamservice.model.RefreshToken;
import com.shivani.iamservice.model.Role;
import com.shivani.iamservice.model.User;
import com.shivani.iamservice.repository.UserRepository;
import com.shivani.iamservice.security.JwtService;
import com.shivani.iamservice.service.AuthService;
import com.shivani.iamservice.service.RefreshTokenService;
import com.shivani.iamservice.service.UserService;

// Unit tests for AuthService. Every dependency is a fake (mock), so these tests
// check only AuthService's own logic: no database, no Spring, no real tokens.
class AuthServiceTest {

	// The fakes AuthService needs (the same five as its constructor).
	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private JwtService jwtService;
	private UserService userService;
	private RefreshTokenService refreshTokenService;
	// The real object under test.
	private AuthService authService;

	// Runs before EVERY test, so each test starts with a clean setup.
	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		jwtService = mock(JwtService.class);
		userService = mock(UserService.class);
		refreshTokenService = mock(RefreshTokenService.class);
		authService = new AuthService(userRepository, passwordEncoder, jwtService, userService, refreshTokenService);
	}

	// ---------- REGISTER ----------

	// Success: a new email and a valid password -> a user is saved.
	@Test
	void register_withValidInput_savesUser() {
		// Arrange: no user with this email exists yet; the fake encoder returns a fixed
		// "hash".
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Test1234@")).thenReturn("hashedPassword123");

		authService.register("test@gmail.com", "Test1234@");

		// register returns void, so we verify save() was called with some User.
		verify(userRepository).save(any(User.class));
	}

	// Validation: a bad email is rejected before anything is saved.
	@Test
	void register_withInvalidEmail_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("not-an-email", "Test1234@");
		});
	}

	// Validation: a weak password is rejected.
	@Test
	void register_withInvalidPassword_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("test@gmail.com", "weak");
		});
	}

	// RBAC: everyone who registers becomes an EMPLOYEE; the role is never taken
	// from the request. The captor grabs the User that was passed to save().
	@Test
	void register_assignsEmployeeRole() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Test@1234")).thenReturn("hashedPassword123");
		authService.register("test@gmail.com", "Test@1234");

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());

		assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
	}

	// Duplicate: the email is already taken -> rejected.
	@Test
	void register_withDuplicateEmail_throwsException() {
		// Arrange: the fake database says a user with this email already exists.
		when(userRepository.findByEmail("test@gmail.com"))
				.thenReturn(Optional.of(new User("test@gmail.com", "hashedPassword", Role.EMPLOYEE)));
		// The real call goes INSIDE the lambda so assertThrows can catch the exception.
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("test@gmail.com", "Test@1234");
		});
	}

	// ---------- LOGIN ----------

	// Failure 1: the email isn't in the database -> 401 (BadCredentials).
	@Test
	void login_unknownEmail_throwsBadCredentials() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		assertThrows(BadCredentialsException.class, () -> {
			authService.login("test@gmail.com", "Test@1234");
		});
	}

	// Failure 2: the user exists but the password is wrong -> the same 401.
	// Using the same exception for both failures means an attacker can't tell
	// which part was wrong.
	@Test
	void login_wrongPassword_throwsBadCredentials() {
		User user = new User("test@gmail.com", "hashedPassword123", Role.EMPLOYEE);

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
		// matches(typedPassword, storedHash): the fake encoder says "no match".
		when(passwordEncoder.matches("WrongPass1@", "hashedPassword123")).thenReturn(false);
		assertThrows(BadCredentialsException.class, () -> {
			authService.login("test@gmail.com", "WrongPass1@");
		});
	}

	// Success: the user gets BOTH tokens.
	@Test
	void login_validCredentials_returnsBothTokens() {
		User user = new User("test@gmail.com", "hashedPassword123", Role.EMPLOYEE);

		// Arrange: user exists, password matches, the token services return fake
		// tokens.
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Test@1234", "hashedPassword123")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("fake-access");
		// user.getId() is null here because this User was never saved to a database.
		when(refreshTokenService.createRefreshToken(user.getId())).thenReturn("fake-refresh");

		TokenResponse result = authService.login("test@gmail.com", "Test@1234");

		// assertEquals(expected, actual): the first value is what we expect.
		assertEquals("fake-access", result.accessToken());
		assertEquals("fake-refresh", result.refreshToken());
	}

	// ---------- LOGOUT ----------

	// AuthService just hands the raw token to RefreshTokenService.
	// The method returns void, so we verify the CALL happened.
	@Test
	void logout_callsRevokeOnRefreshTokenService() {
		authService.logout("raw-token");

		verify(refreshTokenService).revokeByRawToken("raw-token");
	}

	// ---------- REFRESH ----------

	// Success: a valid old token is traded for a brand-new pair.
	@Test
	void refresh_validToken_returnsNewTokenPair() {
		RefreshToken oldToken = new RefreshToken(1L, "old-hash", LocalDateTime.now().plusDays(1));
		User user = new User("test@gmail.com", "hashedPassword123", Role.EMPLOYEE);

		// Arrange, in the same order refresh() calls them:
		// 1. verify the old token 2. load the user 3. new access token 4. new refresh
		// token
		when(refreshTokenService.verifyRefreshToken("old-raw")).thenReturn(oldToken);
		when(userService.getById(1L)).thenReturn(user);
		when(jwtService.generateToken(user)).thenReturn("new-access");
		// A mock answers only for the exact argument given. The stub must use the same
		// value the real code passes (user.getId(), which is null in this test).
		when(refreshTokenService.createRefreshToken(user.getId())).thenReturn("new-refresh");

		TokenResponse result = authService.refresh("old-raw");

		assertEquals("new-access", result.accessToken());
		assertEquals("new-refresh", result.refreshToken());
		// Rotation: the old token must be revoked so it can't be used twice.
		verify(refreshTokenService).revoke(oldToken);
	}

	// Failure: a bad token stops everything. thenThrow is the opposite of
	// thenReturn: it makes the fake throw.
	@Test
	void refresh_invalidToken_throwsBadCredentials() {
		when(refreshTokenService.verifyRefreshToken("bad-raw"))
				.thenThrow(new BadCredentialsException("Invalid refresh token"));

		assertThrows(BadCredentialsException.class, () -> authService.refresh("bad-raw"));

		// Nothing after the failed check may have run: no revoke, no new tokens.
		verify(refreshTokenService, never()).revoke(any());
		verify(refreshTokenService, never()).createRefreshToken(any());
	}

}