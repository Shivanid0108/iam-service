package com.shivani.iamservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private JwtService jwtService;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		jwtService = mock(JwtService.class);
		authService = new AuthService(userRepository, passwordEncoder, jwtService);
	}

	@Test
	void register_withValidInput_savesUser() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Test1234@")).thenReturn("hashedPassword123");

		authService.register("test@gmail.com", "Test1234@");

		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_withInvalidEmail_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("not-an-email", "Test1234@");
		});
	}

	@Test
	void register_withInvalidPassword_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("test@gmail.com", "weak");
		});
	}

	@Test
	void register_assignsEmployeeRole() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Test@1234")).thenReturn("hashedPassword123");
		authService.register("test@gmail.com", "Test@1234");

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());

		assertEquals(Role.EMPLOYEE, captor.getValue().getRole());

	}

	@Test
	void register_withDuplicateEmail_ThrowsException() {
		when(userRepository.findByEmail("test@gmail.com"))
				.thenReturn(Optional.of(new User("test@gmail.com", "hashedPassword", Role.EMPLOYEE)));
		assertThrows(IllegalArgumentException.class, () -> {
			authService.register("test@gmail.com", "Test@1234");
		});
	}

	@Test
	void login_unknownEmail_throwsBadCredentials() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
		assertThrows(BadCredentialsException.class, () -> {
			authService.login("test@gmail.com", "Test@1234");
		});
	}

	@Test
	void login_wrongPassword_throwsBadCredentials() {
		User user = new User("test@gmail.com", "hashedPassword123", Role.EMPLOYEE);

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("WrongPass1@", "hashedPassword123")).thenReturn(false);
		assertThrows(BadCredentialsException.class, () -> {
			authService.login("test@gmail.com", "WrongPass1@");
		});
	}

	@Test
	void login_validCredentials_returnsToken() {
		User user = new User("test@gmail.com", "hashedPassword123", Role.EMPLOYEE);

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Test@1234", "hashedPassword123")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("fake-token");
		String token = authService.login("test@gmail.com", "Test@1234");
		assertEquals("fake-token", token);
	}

}
