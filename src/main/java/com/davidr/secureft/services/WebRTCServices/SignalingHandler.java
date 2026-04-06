package com.davidr.secureft.services.WebRTCServices;

import com.davidr.secureft.datamodels.SignalMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SignalingHandler extends TextWebSocketHandler {

    private final UserSessionRegistry userSessionRegistry;
    private final ObjectMapper objectMapper;

    public SignalingHandler(UserSessionRegistry userSessionRegistry, ObjectMapper objectMapper) {
        this.userSessionRegistry = userSessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connected: " + session.getId());
        session.sendMessage(new TextMessage("connected to /signal"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalMessage signal = objectMapper.readValue(message.getPayload(), SignalMessage.class);

        switch (signal.getType()) {
            case "register" -> {
                userSessionRegistry.register(signal.getFrom(), session);
                session.sendMessage(new TextMessage("registered as: " + signal.getFrom()));
            }

            case "call-user", "offer", "answer", "ice-candidate", "getkey", "herekey", "secret-encrypted" -> {
                WebSocketSession targetSession = userSessionRegistry.getSession(signal.getTo());
                if (targetSession != null && targetSession.isOpen()) {
                    targetSession.sendMessage(new TextMessage(message.getPayload()));
                } else {
                    session.sendMessage(new TextMessage("user not available: " + signal.getTo()));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessionRegistry.unregister(session);
        System.out.println("WebSocket disconnected: " + session.getId());
    }
}
