package com.davidr.secureft.security;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.datamodels.UserRole;
import com.davidr.secureft.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityService {

    private final UserService userService;

    public SecurityService(UserService userService) {
        this.userService = userService;
    }

    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email instanceof String emailValue && !emailValue.isBlank()) {
                return emailValue;
            }
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }

        return username;
    }

    public User getCurrentUser() {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return null;
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            return user;
        }
        return userService.getUserByEmail(username);
    }

    public boolean hasRole(UserRole role) {
        if (role == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.name().equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    public void refreshAuthentication(User user) {
        if (user == null) {
            return;
        }

        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
            return;
        }

        UserRole role = user.getRole() == null ? UserRole.ROLE_USER : user.getRole();
        UserDetails principal = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getHashedPassword())
                .authorities(List.of(new SimpleGrantedAuthority(role.name())))
                .build();

        UsernamePasswordAuthenticationToken updatedAuthentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        currentAuthentication.getCredentials(),
                        principal.getAuthorities());
        updatedAuthentication.setDetails(currentAuthentication.getDetails());

        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
    }
}
