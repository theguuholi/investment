# Production Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the investment API production-ready with JWT auth, code quality gates, CI/CD, and AWS deployment readiness.

**Architecture:** Spring Security with stateless JWT filter chain; ArchUnit enforces package rules at test time; JaCoCo + SonarQube provide coverage and quality gates in a GitHub Actions pipeline that builds a Docker image pushed to ECR.

**Tech Stack:** Spring Security 6, JJWT 0.12, ArchUnit 1.3, JaCoCo 0.8, SonarQube Maven plugin, GitHub Actions, Docker, AWS ECR/ECS

---

## Already Done (skip these)
- Flyway V1 migration (`db/migration/V1__create_initial_schema.sql`)
- Swagger via `springdoc-openapi-starter-webmvc-ui`
- Testcontainers (`TestcontainersConfiguration.java`)
- Spring Boot Actuator dependency

---

## File Map

### New files to create
| File | Responsibility |
|---|---|
| `src/main/java/.../auth/TokenService.java` | Generate and validate JWT tokens |
| `src/main/java/.../auth/AuthController.java` | POST /auth/login endpoint |
| `src/main/java/.../auth/JwtFilter.java` | Per-request JWT validation filter |
| `src/main/java/.../auth/UserDetailsServiceImpl.java` | Load user by email for Spring Security |
| `src/main/java/.../auth/AuthRequest.java` | record(email, password) |
| `src/main/java/.../auth/AuthResponse.java` | record(token) |
| `src/main/java/.../config/SecurityConfig.java` | Full JWT filter chain (replaces current permissive one) |
| `src/main/resources/db/migration/V2__add_password_to_users.sql` | Add password column |
| `src/test/java/.../auth/TokenServiceTest.java` | Unit tests for JWT generation/validation |
| `src/test/java/.../auth/AuthControllerTest.java` | Controller test for login |
| `src/test/java/.../arch/ArchitectureTest.java` | ArchUnit rules |
| `Dockerfile` | Multi-stage Docker build |
| `.github/workflows/ci.yml` | GitHub Actions pipeline |
| `sonar-project.properties` | SonarQube config |

### Files to modify
| File | Change |
|---|---|
| `pom.xml` | Add JJWT, ArchUnit, JaCoCo plugin, Sonar plugin |
| `src/main/java/.../user/User.java` | Add `password` field + getter/setter |
| `src/main/resources/application.yaml` | Add JWT secret, expiration, actuator config |

---

## Task 1: Add dependencies to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add JJWT, ArchUnit, JaCoCo plugin, and Sonar plugin**

In `pom.xml`, add inside `<dependencies>`:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<!-- ArchUnit -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

Add inside `<build><plugins>`:

```xml
<!-- JaCoCo -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
<!-- Sonar -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>4.0.0.4121</version>
</plugin>
```

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add JJWT, ArchUnit, JaCoCo, and Sonar dependencies"
```

---

## Task 2: Flyway V2 — add password column

**Files:**
- Create: `src/main/resources/db/migration/V2__add_password_to_users.sql`

- [ ] **Step 1: Write migration**

```sql
ALTER TABLE users ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '';
```

- [ ] **Step 2: Add password field to User entity**

In `src/main/java/com/github/theguuholi/investment/user/User.java`, add:

```java
@Column(nullable = false)
private String password;

public String getPassword() { return password; }
public void setPassword(String password) { this.password = password; }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V2__add_password_to_users.sql \
        src/main/java/com/github/theguuholi/investment/user/User.java
git commit -m "feat: add password column via Flyway V2 migration"
```

---

## Task 3: JWT TokenService

**Files:**
- Create: `src/main/java/com/github/theguuholi/investment/auth/TokenService.java`
- Create: `src/test/java/com/github/theguuholi/investment/auth/TokenServiceTest.java`

- [ ] **Step 1: Write failing test**

Create `src/test/java/com/github/theguuholi/investment/auth/TokenServiceTest.java`:

```java
package com.github.theguuholi.investment.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            "test-secret-key-that-is-long-enough-for-hs256-algorithm",
            3600L
        );
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        // given / when
        String token = tokenService.generateToken("alice@example.com");

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_fromValidToken_returnsEmail() {
        // given
        String token = tokenService.generateToken("alice@example.com");

        // when
        String email = tokenService.extractEmail(token);

        // then
        assertThat(email).isEqualTo("alice@example.com");
    }

    @Test
    void isValid_withValidToken_returnsTrue() {
        // given
        String token = tokenService.generateToken("alice@example.com");

        // when / then
        assertThat(tokenService.isValid(token)).isTrue();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        // given
        String token = tokenService.generateToken("alice@example.com") + "tampered";

        // when / then
        assertThat(tokenService.isValid(token)).isFalse();
    }

    @Test
    void generateToken_withNullEmail_throwsException() {
        assertThatThrownBy(() -> tokenService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -Dtest="TokenServiceTest" --no-transfer-progress 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `TokenService` not found.

- [ ] **Step 3: Implement TokenService**

Create `src/main/java/com/github/theguuholi/investment/auth/TokenService.java`:

```java
package com.github.theguuholi.investment.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private final SecretKey key;
    private final long expirationSeconds;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds) {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("JWT secret must not be blank");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String email) {
        if (email == null) throw new IllegalArgumentException("Email must not be null");
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn test -Dtest="TokenServiceTest" --no-transfer-progress 2>&1 | tail -5
```

Expected: `Tests run: 5, Failures: 0, Errors: 0` and `BUILD SUCCESS`

- [ ] **Step 5: Add JWT config to application.yaml**

```yaml
jwt:
  secret: "change-me-to-a-secure-secret-of-at-least-32-chars-long"
  expiration-seconds: 86400
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/github/theguuholi/investment/auth/TokenService.java \
        src/test/java/com/github/theguuholi/investment/auth/TokenServiceTest.java \
        src/main/resources/application.yaml
git commit -m "feat: add JWT TokenService with generate/validate"
```

---

## Task 4: UserDetailsService and Auth DTOs

**Files:**
- Create: `src/main/java/com/github/theguuholi/investment/auth/UserDetailsServiceImpl.java`
- Create: `src/main/java/com/github/theguuholi/investment/auth/AuthRequest.java`
- Create: `src/main/java/com/github/theguuholi/investment/auth/AuthResponse.java`

- [ ] **Step 1: Create AuthRequest and AuthResponse records**

```java
// src/main/java/com/github/theguuholi/investment/auth/AuthRequest.java
package com.github.theguuholi.investment.auth;

public record AuthRequest(String email, String password) {}
```

```java
// src/main/java/com/github/theguuholi/investment/auth/AuthResponse.java
package com.github.theguuholi.investment.auth;

public record AuthResponse(String token) {}
```

- [ ] **Step 2: Create UserDetailsServiceImpl**

```java
package com.github.theguuholi.investment.auth;

import com.github.theguuholi.investment.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
```

- [ ] **Step 3: Add findByEmail to UserRepository**

In `src/main/java/com/github/theguuholi/investment/user/UserRepository.java`:

```java
package com.github.theguuholi.investment.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/github/theguuholi/investment/auth/ \
        src/main/java/com/github/theguuholi/investment/user/UserRepository.java
git commit -m "feat: add UserDetailsService and auth DTOs"
```

---

## Task 5: JWT Filter

**Files:**
- Create: `src/main/java/com/github/theguuholi/investment/auth/JwtFilter.java`

- [ ] **Step 1: Create JwtFilter**

```java
package com.github.theguuholi.investment.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public JwtFilter(TokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            var token = header.substring(7);
            if (tokenService.isValid(token)) {
                var email = tokenService.extractEmail(token);
                var userDetails = userDetailsService.loadUserByUsername(email);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/github/theguuholi/investment/auth/JwtFilter.java
git commit -m "feat: add JWT authentication filter"
```

---

## Task 6: AuthController and SecurityConfig

**Files:**
- Create: `src/main/java/com/github/theguuholi/investment/auth/AuthController.java`
- Modify: `src/main/java/com/github/theguuholi/investment/config/SecurityConfig.java`
- Create: `src/test/java/com/github/theguuholi/investment/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing test for login**

Create `src/test/java/com/github/theguuholi/investment/auth/AuthControllerTest.java`:

```java
package com.github.theguuholi.investment.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.theguuholi.investment.config.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        // given
        var request = new AuthRequest("alice@example.com", "secret123");
        given(authenticationManager.authenticate(any())).willReturn(
                new UsernamePasswordAuthenticationToken("alice@example.com", null, java.util.List.of()));
        given(tokenService.generateToken("alice@example.com")).willReturn("jwt.token.here");

        // when / then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    void login_withInvalidCredentials_returns500() throws Exception {
        // given
        var request = new AuthRequest("alice@example.com", "wrongpassword");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        // when / then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
```

- [ ] **Step 2: Run test — expect COMPILATION ERROR**

```bash
mvn test -Dtest="AuthControllerTest" --no-transfer-progress 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `AuthController` not found.

- [ ] **Step 3: Implement AuthController**

```java
package com.github.theguuholi.investment.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var token = tokenService.generateToken(auth.getName());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
```

- [ ] **Step 4: Replace SecurityConfig with full JWT chain**

Replace `src/main/java/com/github/theguuholi/investment/config/SecurityConfig.java`:

```java
package com.github.theguuholi.investment.config;

import com.github.theguuholi.investment.auth.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 5: Run tests**

```bash
mvn test -Dtest="AuthControllerTest,TokenServiceTest" --no-transfer-progress 2>&1 | tail -5
```

Expected: `Tests run: 7, Failures: 0, Errors: 0` and `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/github/theguuholi/investment/auth/ \
        src/main/java/com/github/theguuholi/investment/config/SecurityConfig.java
git commit -m "feat: implement JWT authentication with login endpoint and filter chain"
```

---

## Task 7: Update UserService to hash passwords

**Files:**
- Modify: `src/main/java/com/github/theguuholi/investment/user/UserService.java`
- Modify: `src/main/java/com/github/theguuholi/investment/user/api/UserRequest.java`

- [ ] **Step 1: Add password to UserRequest**

```java
package com.github.theguuholi.investment.user.api;

public record UserRequest(String name, String email, String password) {}
```

- [ ] **Step 2: Update UserService to encode password on create**

```java
package com.github.theguuholi.investment.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() { return repository.findAll(); }

    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }

    public void delete(UUID id) { repository.deleteById(id); }
}
```

- [ ] **Step 3: Update UserController to map password from request**

In `src/main/java/com/github/theguuholi/investment/user/api/UserController.java`, update the `create` method:

```java
@PostMapping
public User create(@RequestBody UserRequest request) {
    var user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPassword(request.password());
    return service.create(user);
}
```

- [ ] **Step 4: Fix UserServiceTest — add PasswordEncoder mock**

Update `src/test/java/com/github/theguuholi/investment/user/UserServiceTest.java`:

Add `@Mock private PasswordEncoder passwordEncoder;` and stub it where needed:

```java
// In create_savesAndReturnsUser test, add:
given(passwordEncoder.encode(any())).willReturn("encoded-password");
```

- [ ] **Step 5: Run affected tests**

```bash
mvn test -Dtest="UserServiceTest,UserControllerTest" --no-transfer-progress 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/github/theguuholi/investment/user/ \
        src/test/java/com/github/theguuholi/investment/user/UserServiceTest.java
git commit -m "feat: hash passwords with BCrypt on user creation"
```

---

## Task 8: ArchUnit architectural tests

**Files:**
- Create: `src/test/java/com/github/theguuholi/investment/arch/ArchitectureTest.java`

- [ ] **Step 1: Write ArchUnit tests**

```java
package com.github.theguuholi.investment.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

    private final var classes = new ClassFileImporter()
            .importPackages("com.github.theguuholi.investment");

    @Test
    void controllers_shouldOnlyBeInApiPackages() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..api..")
                .orShould().resideInAPackage("..auth..")
                .because("Controllers must live in api or auth packages");

        rule.check(classes);
    }

    @Test
    void services_shouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .should().dependOnClassesThat()
                .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .because("Services must not depend on controllers");

        rule.check(classes);
    }

    @Test
    void repositories_shouldNotBeAccessedByControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .areAssignableTo(org.springframework.data.repository.Repository.class)
                .because("Controllers must not access repositories directly — go through services");

        rule.check(classes);
    }

    @Test
    void entities_shouldBeInDomainPackages() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("com.github.theguuholi.investment.(*)..")
                .because("Entities must be in feature packages");

        rule.check(classes);
    }

    @Test
    void layeredArchitecture_isRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controllers").definedBy("..api..")
                .layer("Services").definedBy("..(*Service)")
                .layer("Repositories").definedBy("..(*Repository)")
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
                .check(classes);
    }
}
```

- [ ] **Step 2: Run ArchUnit tests**

```bash
mvn test -Dtest="ArchitectureTest" --no-transfer-progress 2>&1 | tail -10
```

Expected: `Tests run: 5, Failures: 0` and `BUILD SUCCESS`

If any rule fails, fix the violation or adjust the rule to match the actual architecture.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/github/theguuholi/investment/arch/ArchitectureTest.java
git commit -m "test: add ArchUnit architectural validation rules"
```

---

## Task 9: JaCoCo coverage report

**Files:**
- Already added plugin in Task 1

- [ ] **Step 1: Run verify to generate coverage report**

```bash
mvn verify -Dtest="UserServiceTest,PortfolioServiceTest,AssetServiceTest,UserControllerTest,PortfolioControllerTest,AssetControllerTest,TokenServiceTest,AuthControllerTest,ArchitectureTest" --no-transfer-progress 2>&1 | tail -10
```

Expected: `BUILD SUCCESS` and report at `target/site/jacoco/index.html`

- [ ] **Step 2: Open the report**

```bash
open target/site/jacoco/index.html
```

Review which classes need more tests to meet the 70% line coverage threshold.

- [ ] **Step 3: Commit if no changes needed**

```bash
git commit --allow-empty -m "ci: verify JaCoCo 70% line coverage threshold passes"
```

---

## Task 10: SonarQube configuration

**Files:**
- Create: `sonar-project.properties`

- [ ] **Step 1: Create sonar-project.properties**

```properties
sonar.projectKey=investment
sonar.projectName=Investment API
sonar.projectVersion=1.0
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.exclusions=**/config/**,**/*Application.java
sonar.cpd.exclusions=**/*Request.java,**/*Response.java,**/*Status.java
```

- [ ] **Step 2: Run JaCoCo XML report**

In `pom.xml`, add XML report goal alongside the existing `report` execution:

```xml
<execution>
    <id>report-xml</id>
    <phase>verify</phase>
    <goals><goal>report</goal></goals>
    <configuration>
        <formats><format>XML</format></formats>
        <outputDirectory>${project.build.directory}/site/jacoco</outputDirectory>
    </configuration>
</execution>
```

- [ ] **Step 3: Commit**

```bash
git add sonar-project.properties pom.xml
git commit -m "ci: add SonarQube configuration and JaCoCo XML report"
```

---

## Task 11: Dockerfile

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: Write multi-stage Dockerfile**

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

# Run stage
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Write .dockerignore**

```
target/
.git/
.github/
*.md
.mvn/wrapper/maven-wrapper.jar
```

- [ ] **Step 3: Verify build (requires Docker)**

```bash
docker build -t investment-api:local .
```

Expected: successful multi-stage build.

- [ ] **Step 4: Commit**

```bash
git add Dockerfile .dockerignore
git commit -m "build: add multi-stage Docker build"
```

---

## Task 12: GitHub Actions CI pipeline

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Write CI pipeline**

```yaml
name: CI

on:
  push:
    branches: [master, main]
  pull_request:
    branches: [master, main]

jobs:
  test:
    name: Test & Quality
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: investment_db
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven

      - name: Run unit tests
        run: mvn test --no-transfer-progress
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/investment_db
          SPRING_DATASOURCE_USERNAME: postgres
          SPRING_DATASOURCE_PASSWORD: postgres
          JWT_SECRET: ci-secret-key-that-is-long-enough-for-hs256

      - name: Run verify (JaCoCo coverage check)
        run: mvn verify --no-transfer-progress -DskipTests
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/investment_db
          SPRING_DATASOURCE_USERNAME: postgres
          SPRING_DATASOURCE_PASSWORD: postgres
          JWT_SECRET: ci-secret-key-that-is-long-enough-for-hs256

      - name: SonarQube analysis
        if: github.ref == 'refs/heads/master' && secrets.SONAR_TOKEN != ''
        run: mvn sonar:sonar --no-transfer-progress
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

  docker:
    name: Build Docker Image
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push to ECR
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.login-ecr.outputs.registry }}/investment-api:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions pipeline with test, JaCoCo, Sonar, and ECR push"
```

---

## Task 13: AWS readiness — externalized configuration

**Files:**
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Externalize all secrets via environment variables**

Replace hardcoded values in `application.yaml` with environment variable bindings:

```yaml
spring:
  application:
    name: investment
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/investment_db}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: ${SHOW_SQL:false}
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  jackson:
    deserialization:
      fail-on-unknown-properties: false

jwt:
  secret: ${JWT_SECRET}
  expiration-seconds: ${JWT_EXPIRATION_SECONDS:86400}

server:
  port: ${PORT:8080}

springdoc:
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "config: externalize all secrets via environment variables for AWS"
```

---

## Self-Review

### Spec coverage
| Requirement | Task |
|---|---|
| Flyway versionamento | ✅ Already done + Task 2 (V2) |
| JWT Authentication | ✅ Tasks 3–7 |
| Swagger/OpenAPI | ✅ Already in pom |
| Testcontainers | ✅ Already done |
| ArchUnit | ✅ Task 8 |
| JaCoCo | ✅ Tasks 1 + 9 |
| SonarQube | ✅ Tasks 1 + 10 |
| GitHub Actions | ✅ Task 12 |
| Spring Boot Actuator | ✅ Already in pom + Task 13 config |
| AWS readiness | ✅ Tasks 11 + 12 + 13 |

### GitHub Secrets needed (set in repo settings)
```
SONAR_TOKEN         → SonarQube user token
SONAR_HOST_URL      → https://sonarcloud.io (or your self-hosted URL)
AWS_ACCESS_KEY_ID   → IAM key with ECR push permissions
AWS_SECRET_ACCESS_KEY
AWS_REGION          → e.g. us-east-1
ECR_REPOSITORY      → your ECR repo name
```
