package com.davidr.secureft.datamodels;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Users")
public class User {
    @Id
    private String id;
    private String username;
    private String hashedPassword;
    private String email;
    private String role;
    private Instant createdAt;
    private Instant lastLoginAt;
    private String avatarURL;
    

    public User(String username, String hashedPassword, String email, String role, String id, Instant createdAt,
            Instant lastLoginAt) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.role = role;
        this.id = id;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
     public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    @Override
    public String toString() {
        return "User [username=" + username + ", hashedPassword=" + hashedPassword + ", email=" + email + ", role="
                + role + ", id=" + id + ", createdAt=" + createdAt + ", lastLoginAt=" + lastLoginAt + ", avatarURL="
                + avatarURL + "]";
    }
    
}
