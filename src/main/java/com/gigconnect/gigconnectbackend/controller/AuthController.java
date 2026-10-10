
package com.gigconnect.gigconnectbackend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigconnect.gigconnectbackend.entity.User;
import com.gigconnect.gigconnectbackend.service.UserService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class AuthController {

    private final UserService userService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // REGISTER USER
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody Map<String, String> data) {

        try {
            String name = data.get("name");
            String email = data.get("email");
            String password = data.get("password");
            String role = data.get("role");

            if (name == null || name.isBlank()
                    || email == null || email.isBlank()
                    || password == null || password.isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                "Name, email, and password are required"));
            }

            if (password.length() < 8) {
                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                "Password must contain at least 8 characters"));
            }

            User user = new User();
            user.setName(name.trim());
            user.setEmail(email.trim());
            user.setPassword(password);

            String normalizedRole =
                    role == null || role.isBlank()
                            ? "FREELANCER"
                            : role.trim().toUpperCase();

            if (!normalizedRole.equals("CLIENT")
                    && !normalizedRole.equals("FREELANCER")) {

                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                "Role must be CLIENT or FREELANCER"));
            }

            user.setRole(normalizedRole);

            // UserService hashes the password before saving.
            User savedUser = userService.registerUser(user);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Registration successful",
                            "id", savedUser.getId(),
                            "name", savedUser.getName(),
                            "email", savedUser.getEmail(),
                            "role", savedUser.getRole()
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            e.getMessage() == null
                                    ? "Registration failed"
                                    : e.getMessage()
                    ));
        }
    }

    // LOGIN USER
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> data) {

        String email = data.get("email");
        String password = data.get("password");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Email and password are required"));
        }

        try {
            User user = userService.findByEmail(email.trim());

            if (user == null
                    || user.getPassword() == null
                    || !passwordEncoder.matches(
                            password, user.getPassword())) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("error", "Invalid email or password"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Login successful",
                            "id", user.getId(),
                            "name", user.getName(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error",
                            "Login failed. Please try again."));
        }
    }
}

