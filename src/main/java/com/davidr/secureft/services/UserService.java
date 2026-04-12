package com.davidr.secureft.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.datamodels.UserRole;
import com.davidr.secureft.repositories.UserRepo;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public User newUser(String username, String password, String email) {

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }

        emailChecker(email);

        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        String hashedPassword = passwordEncoder.encode(password);
        UserRole role = UserRole.ROLE_USER;
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        Instant lastLoginAt = Instant.now();

        return new User(username, hashedPassword, email, role, id, createdAt, lastLoginAt);
    }

    private void emailChecker(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        String normalizedEmail = email.trim();
        int atIndex = normalizedEmail.indexOf('@');
        int lastAtIndex = normalizedEmail.lastIndexOf('@');

        if (normalizedEmail.isEmpty()
                || atIndex <= 0
                || atIndex != lastAtIndex
                || atIndex == normalizedEmail.length() - 1) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }

        int dotIndex = normalizedEmail.indexOf('.', atIndex + 1);
        if (dotIndex <= atIndex + 1 || dotIndex == normalizedEmail.length() - 1) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
    }

    public boolean addUserToDB(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            return false;
        }
        userRepo.insert(user);
        return true;
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User createUser(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + user.getUsername());
        }
        return userRepo.insert(user);
    }

    public User ensureOAuthUser(String email, String displayName, String avatarUrl) {
        emailChecker(email);

        User existingUser = userRepo.findByEmail(email.trim());
        if (existingUser != null) {
            existingUser.setLastLoginAt(Instant.now());
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                existingUser.setAvatarURL(avatarUrl);
            }
            return userRepo.save(existingUser);
        }

        String username = buildAvailableOAuthUsername(email, displayName);
        String generatedPassword = UUID.randomUUID().toString();
        User user = new User(
                username,
                passwordEncoder.encode(generatedPassword),
                email.trim(),
                UserRole.ROLE_USER,
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now());

        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarURL(avatarUrl);
        }

        return userRepo.insert(user);
    }

    public User updateUser(String username, String oldPassword, String newUsername, String newPassword, String newEmail,
            UserRole newRole, String newAvatarURL) {
        User existingUser = userRepo.findByUsername(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("User dose not exists: " + username);
        }

        if (!passwordEncoder.matches(oldPassword, existingUser.getHashedPassword())) {
            throw new IllegalArgumentException("old password isnt correct!");
        }

        // if (!isValidPassword(newPassword)){
        //     throw new IllegalArgumentException("password isnt valid");
        // }

        if (!username.equals(newUsername)) {
            if (userRepo.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("User already exists: " + newUsername);
            }
            existingUser.setUsername(newUsername);
        }

        if (!existingUser.getEmail().equals(newEmail)) {
            emailChecker(newEmail);
            if (userRepo.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email already exists: " + newEmail);
            }
        }

        existingUser.setHashedPassword(passwordEncoder.encode(newPassword));
        existingUser.setEmail(newEmail);
        existingUser.setRole(newRole);
        existingUser.setAvatarURL(newAvatarURL);

        return userRepo.save(existingUser);
    }

    public boolean deleteUser(String username) {
        if (!userRepo.existsByUsername(username)) {
            return false;
        }
        userRepo.deleteByUsername(username);
        return true;
    }

    private String buildAvailableOAuthUsername(String email, String displayName) {
        String base = normalizeUsernameCandidate(displayName);
        if (base.isBlank()) {
            int atIndex = email.indexOf('@');
            base = atIndex > 0 ? normalizeUsernameCandidate(email.substring(0, atIndex)) : "googleuser";
        }
        if (base.isBlank()) {
            base = "googleuser";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepo.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeUsernameCandidate(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("[^A-Za-z0-9._-]", "");
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }

        return hasLower && hasUpper && hasDigit && hasSymbol;
    }
}
