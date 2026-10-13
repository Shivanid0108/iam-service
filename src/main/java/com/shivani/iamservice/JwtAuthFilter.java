package com.shivani.iamservice;

import java.io.IOException;
import java.util.List; // holds the user's authorities (their roles) for Spring Security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // the "note": who the user is + their roles
import org.springframework.security.core.authority.SimpleGrantedAuthority; // one role, in the form Spring Security understands
import org.springframework.security.core.context.SecurityContextHolder; // the place where the note is stored for this request
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // base class: guarantees the filter runs once per request

import io.jsonwebtoken.Claims; // the data inside a verified token (user id, role, expiry)
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component // Spring creates one instance so SecurityConfig can plug it into the filter
			// chain
public class JwtAuthFilter extends OncePerRequestFilter {

	// Used to verify the token's signature and expiry and read what's inside it.
	private final JwtService jwtService;

	// Constructor injection: Spring hands in a working JwtService.
	public JwtAuthFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	// Runs on every request, before the door rules in SecurityConfig.
	// This filter never blocks anyone. It only writes a "note" (who this is and
	// their role) when the token is valid. The door rules read the note later
	// and decide: 401 if there is no note, 403 if the role isn't allowed.
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// The client sends its token as "Authorization: Bearer <token>".
		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			// Cut off the "Bearer " prefix (7 characters) to get the raw token.
			String token = header.substring(7);

			try {
				// Verifies the signature and expiry once, then returns the claims.
				Claims claims = jwtService.parseClaims(token);
				String userId = claims.getSubject();
				String role = claims.get("role", String.class);

				// Spring's role checks (hasRole) look for the "ROLE_" prefix,
				// so EMPLOYEE in the token becomes ROLE_EMPLOYEE here.
				// A list is just the shape Spring requires: each user has one role.
				List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

				// Credentials are null because the token already proved who this is.
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null,
						authorities);

				// This is the "note" the door rules in SecurityConfig read later.
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} catch (Exception e) {
				// Invalid, tampered or expired token: do nothing. The request stays
				// unauthenticated, so the door rules reject it with a 401.
			}
		}

		// Must run for every request, inside or outside the if-block above.
		// If it sat inside, requests without a token would never move on to
		// the controller (or to the door rules that reject them).
		filterChain.doFilter(request, response);
	}
}