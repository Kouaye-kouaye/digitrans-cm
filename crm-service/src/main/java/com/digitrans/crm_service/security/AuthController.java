package com.digitrans.crm_service.security;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final InMemoryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()) != null) {
            throw new RuntimeException("Username already taken: " + request.username());
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        String token = jwtService.generateToken(extraClaims, user);

        AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                jwtProperties.getExpiration()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username());
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + request.username());
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        String token = jwtService.generateToken(extraClaims, user);

        AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                jwtProperties.getExpiration()
        );

        return ResponseEntity.ok(response);
    }
}
