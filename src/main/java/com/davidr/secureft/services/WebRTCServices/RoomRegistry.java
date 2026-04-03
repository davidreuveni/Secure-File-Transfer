package com.davidr.secureft.services.WebRTCServices;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomRegistry {
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public void joinRoom(String roomId, WebSocketSession session) {
        leaveRoom(session);
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);
    }

    public Set<WebSocketSession> getRoomSessions(String roomId) {
        return rooms.getOrDefault(roomId, Set.of());
    }

    public String getRoomId(WebSocketSession session) {
        return sessionToRoom.get(session.getId());
    }

    public void leaveRoom(WebSocketSession session) {
        String roomId = sessionToRoom.remove(session.getId());
        if (roomId == null) return;

        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions == null) return;

        sessions.remove(session);
        if (sessions.isEmpty()) {
            rooms.remove(roomId);
        }
    }
}