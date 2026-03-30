package com.example.CoinbaseClone.controller;

import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> register(@RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String firstName,
                                      @RequestParam String lastName) {
        try {
            User user = userService.registerUser(email, password, firstName, lastName);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "walletAddress", user.getWalletAddress() == null ? "" : user.getWalletAddress(),
                    "verified", user.isVerified(),
                    "role", user.getRole() == null ? "USER" : user.getRole()
            ));
        } catch (RuntimeException e) {
            HttpStatus status = "User already exists".equalsIgnoreCase(e.getMessage())
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", e.getMessage()));
        }
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.CoinbaseClone.config.JwtUtil jwtUtil;

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        var userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Invalid email or password"));
        }

        var user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Invalid email or password"));
        }

        final String token = jwtUtil.generateToken(user.getEmail(), java.util.Map.of(
                "role", userService.normalizeRole(user)
        ));

        return ResponseEntity.ok(java.util.Map.of(
                "token", token,
                "type", "Bearer",
                "expiresIn", 86400,
                "role", userService.normalizeRole(user)
        ));
    }
}
