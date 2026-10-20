package com.shivani.iamservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shivani.iamservice.model.Role;
import com.shivani.iamservice.model.User;
import com.shivani.iamservice.repository.UserRepository;
import com.shivani.iamservice.service.UserService;

// Unit tests for UserService.changeRole. The database is a fake (mock).
class UserServiceTest {

	private UserRepository userRepository;
	private UserService userService;

	// Runs before EVERY test, so each starts with a clean setup.
	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		userService = new UserService(userRepository);
	}

	// Existing user: the role changes AND the change is saved.
	@Test
	void changeRole_existingUser_updatesRoleAndSaves() {
		// Arrange: user 1 exists and is currently an EMPLOYEE.
		User user = new User("worker@gmail.com", "hashedPassword123", Role.EMPLOYEE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		// save() returns the saved user; the fake must do the same.
		when(userRepository.save(user)).thenReturn(user);

		// Act: promote user 1 to MANAGER.
		User result = userService.changeRole(1L, Role.MANAGER);

		// Assert: the role changed on the object...
		assertEquals(Role.MANAGER, result.getRole());
		// ...and it was saved. Without save(), the database would never change.
		verify(userRepository).save(user);
	}

	// Missing user: nothing to change, so it fails and saves nothing.
	// The 404 itself comes later, from GlobalExceptionHandler.
	@Test
	void changeRole_missingUser_throwsNoSuchElement() {
		// Arrange: the fake database finds no user with id 99.
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		// Act + Assert: the real call goes INSIDE the lambda so assertThrows can catch
		// it.
		assertThrows(NoSuchElementException.class, () -> userService.changeRole(99L, Role.ADMIN));

		// Nothing may have been saved for a user that doesn't exist.
		verify(userRepository, never()).save(any());
	}
}