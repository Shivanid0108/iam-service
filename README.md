# IAM Service

![CI](https://github.com/Shivanid0108/iam-service/actions/workflows/ci.yml/badge.svg)

An identity and access management backend: registration, login, JWT access tokens, rotating refresh tokens, role-based access control, and rate-limited login. Built with Java 25, Spring Boot 4, Spring Security 7, PostgreSQL, Redis and Docker.

## Features

- Register and log in with BCrypt-hashed passwords
- Short-lived JWT access tokens (15 min) plus rotating, single-use refresh tokens (7 days)
- Refresh tokens are stored only as SHA-256 hashes, never in plain text
- Logout revokes the refresh token (always returns 204, so it leaks nothing)
- Role-based access control (EMPLOYEE, MANAGER, ADMIN) with default-deny rules
- Admin endpoint to change a user's role
- Login rate limiting backed by Redis (works across multiple app instances)
- HTTPS, Dockerized, unit tested, GitHub Actions CI

## API

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an account (role EMPLOYEE) |
| POST | `/api/auth/login` | public, rate limited | Returns access and refresh tokens |
| POST | `/api/auth/refresh` | public | Trade a refresh token for a new pair |
| POST | `/api/auth/logout` | public | Revoke a refresh token |
| GET | `/api/users/me` | any logged-in user | Current user |
| GET | `/api/admin/ping` | ADMIN | Admin check |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Change a user's role |

Status codes: 400 bad input, 401 not authenticated, 403 not allowed, 404 not found, 429 too many login attempts.

## Run it with Docker

1. Create a `.env` file (git-ignored) in the project root:
   ```
   DB_PASSWORD=choose-a-password
   JWT_SECRET=a-long-random-string-at-least-32-characters
   KEYSTORE_PASSWORD=choose-a-keystore-password
   ```
2. Create a local self-signed keystore for HTTPS (not committed):
   ```
   keytool -genkeypair -alias iam -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 365
   ```
   Use the same password as `KEYSTORE_PASSWORD`.
3. Start everything (3 app copies behind nginx, plus PostgreSQL and Redis):
   ```
   docker compose up --build --scale app=3
   ```
4. The API is at `https://localhost:8080` (self-signed certificate, so turn off SSL verification in Postman or use `-k` with curl).

The first admin has to be promoted by hand, because everyone registers as EMPLOYEE:
```
docker compose exec db psql -U iam_user -d iam_service
UPDATE users SET user_role = 'ADMIN' WHERE email = 'you@example.com';
```

## Architecture

```
Client -> nginx (:8080) -> app x3 -> PostgreSQL
                                  -> Redis
```

- **Stateless app:** the access token carries the role, so any copy can serve any request.
- **Shared state lives outside the app:** users and refresh tokens in PostgreSQL, login attempt counters in Redis.
- **nginx** passes encrypted traffic straight through (TCP mode) and spreads connections across the app containers.

## Code structure

```
com.shivani.iamservice
├── IamServiceApplication   (starts the Spring Boot app)
├── controller   AuthController, UserController, AdminController
├── service      AuthService, UserService, RefreshTokenService, RateLimitService
├── repository   UserRepository, RefreshTokenRepository
├── model        User, Role, RefreshToken
├── dto          LoginRequest, RegisterRequest, RefreshRequest, ChangeRoleRequest, TokenResponse, UserResponse
├── security     SecurityConfig, JwtAuthFilter, JwtService
└── exception    GlobalExceptionHandler, TooManyRequestsException
```

### What each layer does

| Package | Role |
|---|---|
| **controller** | The web layer. Receives HTTP requests, reads the body or path, calls one service method, and returns the response. No business logic here. |
| **service** | The business rules: validate input, check passwords, issue and rotate tokens, enforce the rate limit, change roles. |
| **repository** | The only code that talks to the database (Spring Data JPA interfaces such as `findByEmail`). |
| **model** | The entities mapped to database tables (`User`, `RefreshToken`) and the `Role` enum. They guard their own state with methods like `revoke()` and `changeRole()` instead of public setters. |
| **dto** | Small objects that define exactly what goes in and out of the API, so entities and password hashes never leak into responses. `ChangeRoleRequest`, `RefreshRequest`, `TokenResponse` and `UserResponse` are Java records; `LoginRequest` and `RegisterRequest` are plain classes. |
| **security** | Spring Security setup: which URLs are public or restricted, the filter that reads the JWT on each request, and the service that creates and validates tokens. |
| **exception** | Turns errors into clean HTTP responses (400, 401, 404, 429) in one place. |

### Dependencies between classes

Each layer only calls the one below it:

```
Controller -> Service -> Repository -> Database
                 |
                 +-> JwtService, PasswordEncoder, RateLimitService (Redis)
```

- `AuthController` calls `AuthService`.
- `AuthService` uses `UserRepository` (find the user), `PasswordEncoder` (check the password), `RateLimitService` (count attempts in Redis), `JwtService` (create the access token), `RefreshTokenService` (create, verify and revoke refresh tokens) and `UserService`.
- `RefreshTokenService` uses `RefreshTokenRepository` and stores only a SHA-256 hash of each token.
- `AdminController` calls `UserService.changeRole`.
- Dependencies are passed in through constructors, which is what makes the services easy to unit test with mocks.

### Request flow: login

```
POST /api/auth/login
  -> AuthController.login          (reads LoginRequest)
  -> AuthService.login
       -> reject a missing email or password (401)
       -> RateLimitService          (Redis INCR; 429 if over the limit)
       -> UserRepository.findByEmail
       -> PasswordEncoder.matches   (BCrypt)
       -> RateLimitService.reset    (clears the counter)
       -> JwtService.generateToken  (access token, 15 min)
       -> RefreshTokenService.createRefreshToken (stored as a hash)
  <- TokenResponse (access + refresh token)
```

### Request flow: a protected endpoint

```
GET /api/admin/ping  with  Authorization: Bearer <access token>
  -> JwtAuthFilter       (validates the token, puts the user id and role in the security context)
  -> SecurityConfig      (URL rules: /api/admin/** requires ADMIN; 401 if no token, 403 if wrong role)
  -> AdminController
```

### Error handling

Services throw plain exceptions (`BadCredentialsException`, `IllegalArgumentException`, `NoSuchElementException`, `TooManyRequestsException`). `GlobalExceptionHandler` maps each one to a status code, so controllers stay free of try/catch blocks.

## Spring features and dependencies used

### Spring features

| Feature | Where it is used |
|---|---|
| **Auto-configuration and embedded Tomcat** | Spring Boot starts the web server and configures beans from the dependencies on the classpath. |
| **Dependency injection (constructor)** | Controllers, services and repositories receive what they need through constructors. This keeps classes loosely coupled and testable. |
| **Spring MVC** | Maps HTTP requests to controller methods and converts JSON to and from Java objects. |
| **Spring Data JPA / Hibernate** | Repositories (`findByEmail`, `findByHashedToken`) are generated from method names. Entities map to the `users` and `refresh_tokens` tables. |
| **Spring Security** | Filter chain, URL authorization rules, stateless sessions, custom 401 entry point, and a custom JWT filter placed before the username and password filter. |
| **Password encoding** | BCrypt through Spring Security's `PasswordEncoder`. |
| **Externalized configuration** | `application.properties` reads secrets from environment variables (`${DB_PASSWORD}`, `${JWT_SECRET}`, `${KEYSTORE_PASSWORD}`), and Docker Compose can override any property (for example `SPRING_DATASOURCE_URL`). |
| **SSL / HTTPS** | Configured with a PKCS12 keystore through `server.ssl.*` properties. |
| **Centralized exception handling** | `@RestControllerAdvice` maps exceptions to HTTP status codes. |
| **Spring Data Redis** | `StringRedisTemplate` runs `INCR` and `EXPIRE` for login rate limiting. |

### Dependencies (see `pom.xml`)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc` | REST controllers (Spring MVC), JSON, embedded Tomcat |
| `spring-boot-starter-data-jpa` | Hibernate, repositories, transactions |
| `spring-boot-starter-security` | Authentication, authorization, password encoding |
| `spring-boot-starter-data-redis` | Redis client and `StringRedisTemplate` (login rate limiting) |
| `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.13.0) | Creating and validating JWTs |
| `postgresql` | PostgreSQL JDBC driver (runtime) |
| `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`, `spring-boot-starter-webmvc-test` | Test support (JUnit 5 and Mockito come with them) |

Infrastructure: PostgreSQL 18, Redis 7, nginx, Docker and Docker Compose, GitHub Actions, k6 (load tests).

## Annotations reference

| Annotation | What it does | When to use it |
|---|---|---|
| `@SpringBootApplication` | Turns on auto-configuration and component scanning | Once, on the main class |
| `@RestController` | Marks a class that handles web requests and returns data (JSON) | Every controller |
| `@RequestMapping("/api/auth")` | Sets a common URL prefix | On a controller class to share a path |
| `@GetMapping`, `@PostMapping`, `@PatchMapping` | Map an HTTP method and path to a method | On controller methods, to match the HTTP verb |
| `@RequestBody` | Converts the JSON body into a Java object | A method parameter that reads the request body |
| `@PathVariable` | Reads a value from the URL, e.g. `/users/{id}/role` | A parameter that comes from the path |
| `@Service` | Marks a class that holds business logic and registers it as a bean | Classes in the service layer |
| `@Component` | Registers any class as a bean | Classes that fit no other category, such as `JwtAuthFilter` |
| `@Configuration` | Marks a class that defines beans and settings | Setup classes like `SecurityConfig` |
| `@Bean` | Tells Spring to manage the object a method returns | Inside a `@Configuration` class, for things like the `PasswordEncoder` or the security filter chain |
| `@EnableWebSecurity` | Turns on web security configuration | On the security configuration class |
| `@Entity` | Maps a class to a database table | Every database model class |
| `@Table(name = "...")` | Sets the table name | When the default isn't right: `user` is a reserved word in PostgreSQL, so the table is named `users` |
| `@Id` | Marks the primary key | One field in every entity |
| `@GeneratedValue` | Lets the database generate the key | Alongside `@Id` for auto-generated IDs |
| `@Column` | Customizes a column (name, nullable, unique) | When a field needs rules, such as a unique email, or a custom name like `user_role` |
| `@Enumerated(EnumType.STRING)` | Stores an enum as text, not as a number | Enum fields like `Role`, so reordering the enum never corrupts data |
| `@RestControllerAdvice` | One class that handles exceptions for all controllers | Once, for `GlobalExceptionHandler` |
| `@ExceptionHandler` | Says which exception a method handles and what response to send | Methods inside the advice class |
| `@Value("${...}")` | Injects a value from properties or environment variables; a default can follow a colon, as in `${jwt.refresh-expiration:604800000}` | Reading config such as the JWT secret and expiry |
| `@Override` | Tells Java a method replaces one from a parent class | When implementing a method such as `doFilterInternal`, so the compiler catches signature typos |
| `@Test` | Marks a unit test method | Every test |
| `@BeforeEach` | Runs before every test to build a fresh setup | Test setup methods |

## Load testing (k6)

Tested locally on one laptop with k6 (500 concurrent users, ramped up over 15 s), before rate limiting was enabled:

| Setup | Requests/s | Avg | p(95) | Failed |
|---|---|---|---|---|
| 1 app container | 99 | 3.9 s | 7.0 s | 0% |
| 3 app containers | 91 | 4.4 s | 10.4 s | 0.02% |

Login is CPU-bound (BCrypt), and one container already used nearly all of the laptop's CPU cores, so adding containers on the same machine did not increase throughput. Scaling out helps only with separate machines.

**Brute-force test:** 30 wrong-password attempts against one account, sent through nginx to 3 containers. Result: 5 reached the password check (401), 25 were rejected with 429. Because the counter is shared in Redis, the limit holds no matter which container answers.

Test scripts are in `loadtest/`. Run with:
```
k6 run -e TEST_PASSWORD='your-password' loadtest/login-test.js
k6 run loadtest/attack-test.js
```

## Tests

```
./mvnw test
```
Unit tests (JUnit 5 and Mockito) cover token rotation, login, logout, register, role changes and the rate limiter wiring. CI runs them on every push.

## Design decisions

- **Rotating refresh tokens:** each refresh token works once. Reusing a revoked token fails with a generic 401.
- **Generic login errors:** the same message for an unknown email and a wrong password, so attackers learn nothing.
- **Roles are never taken from the request:** everyone registers as EMPLOYEE, and only an admin can change roles.
- **Rate limiting in Redis:** 5 login attempts per minute per account, counted with Redis `INCR` (atomic) plus a 60 second `EXPIRE`. A successful login clears the counter. The check runs before the database lookup and BCrypt, so blocked requests are cheap.
- **Bad input is rejected first:** a login request with a missing email or password gets a clean 401 before it reaches the rate limiter.

## Known limitations

- Rate limiting is per account (email), so someone could briefly lock out a victim. Per-IP limiting would need the real client IP (for example via `X-Forwarded-For` behind an HTTP proxy).
- The counter's `INCR` and `EXPIRE` are two separate calls; a crash between them could leave a counter with no timer. A Lua script or atomic command would close this.
- If Redis is down, login returns 500 (the limiter fails closed). A production choice would be to fail open: log the error and allow the login, trading protection for availability.
- Access tokens stay valid until they expire, even after logout or a role change (15 minutes at most).
- The schema is created by Hibernate (`ddl-auto=update`); a migration tool like Flyway would be the production approach.
- Self-signed certificate for local use; in production TLS would be handled by a proxy or load balancer.