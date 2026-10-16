package com.shivani.iamservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration: "this class defines how to build certain objects".
// Similar in spirit to @Service, but for setup/wiring rather than business logic.
@Configuration
@EnableWebSecurity // turns on Spring Security's web protection using the rules defined below
public class SecurityConfig {

	// The guard that reads the token and writes the "note" (who the user is and
	// their role).
	private final JwtAuthFilter jwtAuthFilter;

	// Constructor injection: Spring hands in the JwtAuthFilter it already created.
	public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	// @Bean: Spring calls this method once at startup and registers the returned
	// object, so any class that asks for a PasswordEncoder (like AuthService) gets
	// it.
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// The building's rulebook: which doors are open, which need a badge, and which
	// need a specific role.
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// CSRF protects cookie-based sessions. We use tokens in a header and no
				// sessions, so there is no cookie for an attacker to abuse. Safe to disable.
				.csrf(csrf -> csrf.disable())

				// STATELESS: the server remembers nobody between requests. Every request
				// must carry its own token, or it is treated as an unknown visitor.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Unknown visitor (no valid token) gets a plain 401 instead of a login page.
				// A known user with the wrong role gets 403 (Spring's default).
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

				// Door rules. The FIRST matching rule wins, so order matters:
				// specific rules at the top, the catch-all last.
				.authorizeHttpRequests(auth -> auth
						// Public: you can't have a token before you register or log in.
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						// Spring forwards failures to /error internally. If this isn't open,
						// that forward is blocked too and a 403 gets reported as a 401.
						.requestMatchers("/error").permitAll()
						// Admin only. hasRole("ADMIN") checks for the authority ROLE_ADMIN.
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						// Managers, and Admins too (an Admin can do everything a Manager can).
						.requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
						// Default deny: anything not listed above needs a valid token.
						.anyRequest().authenticated())

				// Run our JWT filter before Spring's login-form filter, so the "note" is
				// already written by the time the door rules above are checked.
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}