package com.davidr.secureft.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import com.davidr.secureft.datamodels.LoginSession;
import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.repositories.LoginSessionRepository;
import com.davidr.secureft.repositories.UserRepo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String COOKIE_NAME = "session_token";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 7; // 7 days

    private final LoginSessionRepository loginSessionRepository;
    private final UserRepo userRepository;

    public AuthService(LoginSessionRepository loginSessionRepository,
            UserRepo userRepository) {
        this.loginSessionRepository = loginSessionRepository;
        this.userRepository = userRepository;
    }

    public void logUserIn(HttpServletResponse response, User user) {
        String token = UUID.randomUUID().toString();

        LoginSession session = new LoginSession();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        loginSessionRepository.save(session);

        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // use true on HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);

        response.addCookie(cookie);
    }

    public User getLoggedUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String token = null;

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (token == null || token.isBlank()) {
            return null;
        }

        Optional<LoginSession> sessionOpt = loginSessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        LoginSession session = sessionOpt.get();

        if (session.getExpiresAt() == null || session.getExpiresAt().isBefore(Instant.now())) {
            loginSessionRepository.delete(session);
            return null;
        }

        return userRepository.findById(session.getUserId().toString()).orElse(null);
    }

    // Alias for backward compatibility with old method name
    public User checkLoggedUser(HttpServletRequest request) {
        return getLoggedUser(request);
    }

    public void logUserOut(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String token = cookie.getValue();
                loginSessionRepository.deleteById(token);

                Cookie deleteCookie = new Cookie(COOKIE_NAME, "");
                deleteCookie.setHttpOnly(true);
                deleteCookie.setSecure(true);
                deleteCookie.setPath("/");
                deleteCookie.setMaxAge(0);

                response.addCookie(deleteCookie);
                break;
            }
        }
    }
}
}