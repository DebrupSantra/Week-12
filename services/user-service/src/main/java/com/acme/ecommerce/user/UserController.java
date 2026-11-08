package com.acme.ecommerce.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserController {
    private final CustomerRepository customers;
    private final PasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecretKey jwtKey;
    public UserController(CustomerRepository customers,
                          @Value("${security.jwt.secret:ecommerce-local-dev-secret-key-change-this-32bytes!}") String secret) {
        this.customers = customers;
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/register")
    public CustomerView register(@Valid @RequestBody Credentials request) {
        if (customers.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Customer saved = customers.save(new Customer(request.email().trim().toLowerCase(),
                passwords.encode(request.password())));
        return new CustomerView(saved.getId(), saved.getEmail());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody Credentials request) {
        Customer customer = customers.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwords.matches(request.password(), customer.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(customer.getId().toString())
                .claim("email", customer.getEmail())
                .claim("roles", List.of(customer.getRole()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(1))))
                .signWith(jwtKey, Jwts.SIG.HS256)
                .compact();
        return new LoginResponse(token, "Bearer", 3600);
    }

    public record Credentials(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {}
    public record CustomerView(java.util.UUID id, String email) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
}
