package com.davidr.secureft.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.repositories.UserRepo;

@Service
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public boolean addUserToDB(User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            return false;
        }
        userRepo.insert(user);
        return true;
    }

    public boolean checkLogin(String username, String password) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return false;
        }
        return user.getPassword().equals(password);
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

    public User updateUser(String username, User updatedUser) {
        User existingUser = userRepo.findByUsername(username);
        if (existingUser == null) {
            return null;
        }
        existingUser.setPassword(updatedUser.getPassword());
        return userRepo.save(existingUser);
    }

    public boolean deleteUser(String username) {
        if (!userRepo.existsByUsername(username)) {
            return false;
        }
        userRepo.deleteByUsername(username);
        return true;
    }
}
