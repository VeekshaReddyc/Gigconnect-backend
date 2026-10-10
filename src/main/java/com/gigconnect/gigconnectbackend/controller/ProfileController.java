
package com.gigconnect.gigconnectbackend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigconnect.gigconnectbackend.entity.User;
import com.gigconnect.gigconnectbackend.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175"
})
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    // GET: Retrieve profile information
    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));

        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    // PUT: Update profile information
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long userId,
            @RequestBody ProfileRequest request) {

        try {
            if (request.name == null || request.name.isBlank()
                    || request.email == null || request.email.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Name and email are required"
                        ));
            }

            User user = userService.updateProfile(
                    userId,
                    request.name,
                    request.email
            );

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));

        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? "Unable to update profile"
                    : exception.getMessage();

            HttpStatus status = message.equals("User not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status)
                    .body(Map.of("message", message));
        }
    }

    // PUT: Change password
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordRequest request) {

        try {
            if (request == null
                    || request.currentPassword == null
                    || request.currentPassword.isBlank()
                    || request.newPassword == null
                    || request.newPassword.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Current and new passwords are required"
                        ));
            }

            // Validate the new password
            if (request.newPassword.length() < 8
                    || !request.newPassword.matches(".*[A-Z].*")
                    || !request.newPassword.matches(".*[a-z].*")
                    || !request.newPassword.matches(".*\\d.*")
                    || !request.newPassword.matches(".*[^A-Za-z0-9].*")) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "New password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"
                        ));
            }

            userService.changePassword(
                    userId,
                    request.currentPassword,
                    request.newPassword
            );

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Password changed successfully"
            ));

        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? "Unable to change password"
                    : exception.getMessage();

            HttpStatus status = message.equals("User not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status)
                    .body(Map.of("message", message));
        }
    }

    // Request body for updating profile
    public static class ProfileRequest {
        public String name;
        public String email;
    }

    // Request body for changing password
    public static class PasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}

