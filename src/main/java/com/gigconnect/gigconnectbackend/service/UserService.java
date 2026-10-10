
package com.gigconnect.gigconnectbackend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.gigconnect.gigconnectbackend.entity.User;
import com.gigconnect.gigconnectbackend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Register a new user
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("FREELANCER");
        }

        return userRepository.save(user);
    }

    // Find user by email
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // Find user by ID
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    // Update name and email
    public User updateProfile(Long userId, String name, String email) {
        User user = findById(userId);

        String normalizedEmail = email.trim().toLowerCase();

        User existingUser = findByEmail(normalizedEmail);

        if (existingUser != null
                && !existingUser.getId().equals(user.getId())) {
            throw new RuntimeException("Email already registered");
        }

        user.setName(name.trim());
        user.setEmail(normalizedEmail);

        return userRepository.save(user);
    }

    // Change password securely
    public void changePassword(
            Long userId,
            String currentPassword,
            String newPassword) {

        User user = findById(userId);

        // Verify current password
        if (!passwordEncoder.matches(
                currentPassword, user.getPassword())) {
            throw new RuntimeException(
                    "Current password is incorrect");
        }

        // Prevent reuse of the current password
        if (passwordEncoder.matches(
                newPassword, user.getPassword())) {
            throw new RuntimeException(
                    "New password must be different from your current password");
        }

        // Encode and save the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

