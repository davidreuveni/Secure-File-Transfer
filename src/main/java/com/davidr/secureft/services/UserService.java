package com.davidr.secureft.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.repositories.UserRepo;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {
    private final UserRepo userRepo;

    private final AuthService authService;

    public UserService(UserRepo userRepo, AuthService authService) {
        this.userRepo = userRepo;
        this.authService = authService;
    }

    public User newUser(String username, String password, String email) {

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }

        emailChecker(email);

        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        String hashedPassword = passwordEncoder().encode(password);
        String role = "user";
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

    public boolean checkLogin(String username, String rawPassword, HttpServletResponse response) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return false;
        }

        if (passwordEncoder().matches(rawPassword, user.getHashedPassword())) {
            authService.logUserIn(response, user);
            return true;
        }

        return false;
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public User createUser(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + user.getUsername());
        }
        return userRepo.insert(user);
    }

    public User updateUser(String username, String oldPassword, String newUsername, String newPassword, String newEmail,
            String newRole, String newAvatarURL) {
        User existingUser = userRepo.findByUsername(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("User dose not exists: " + username);
        }

        if (!passwordEncoder().matches(oldPassword, existingUser.getHashedPassword())) {
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

        existingUser.setHashedPassword(passwordEncoder().encode(newPassword));
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

    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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
