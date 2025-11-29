package se.ifmo.ru.back.ws;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/marines")
public class SpaceMarineWebSocket {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        // Можно реализовать логику от клиента, например "subscribe to events"
        session.getBasicRemote().sendText("ACK: " + message);
    }

    public static void broadcast(String message) {
        for (Session session : sessions) {
            session.getAsyncRemote().sendText(message);
        }
    }
}
