package com.davidr.secureft.services.WebRTCServices;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionRegistry {
    private final Map<String, WebSocketSession> userToSession = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    public void register(String username, WebSocketSession session) {
        userToSession.put(username, session);
        sessionToUser.put(session.getId(), username);
    }

    public WebSocketSession getSession(String username) {
        return userToSession.get(username);
    }

    public String getUsername(WebSocketSession session) {
        return sessionToUser.get(session.getId());
    }

    public void unregister(WebSocketSession session) {
        String username = sessionToUser.remove(session.getId());
        if (username != null) {
            userToSession.remove(username);
        }
    }
}