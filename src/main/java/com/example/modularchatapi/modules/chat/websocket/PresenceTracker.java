package com.example.modularchatapi.modules.chat.websocket;

import com.example.modularchatapi.modules.auth.api.UserQueryApi;
import com.example.modularchatapi.modules.chat.dto.ChatDtos.PresenceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// SignalR'daki OnConnectedAsync / OnDisconnectedAsync karşılığı.
// Aynı kullanıcı birden fazla sekmeden bağlanabileceği için oturum sayısı tutulur.
@Component
@RequiredArgsConstructor
public class PresenceTracker {

    private final ConcurrentHashMap<String, Integer> sessionCounts = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final UserQueryApi userQueryApi;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user == null) return;
        if (sessionCounts.merge(user.getName(), 1, Integer::sum) == 1) {
            broadcast(user.getName(), true);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) return;
        Integer left = sessionCounts.computeIfPresent(user.getName(), (k, v) -> v > 1 ? v - 1 : null);
        if (left == null) {
            broadcast(user.getName(), false);
        }
    }

    public Set<String> onlineEmails() {
        return Set.copyOf(sessionCounts.keySet());
    }

    private void broadcast(String email, boolean online) {
        userQueryApi.findByEmail(email).ifPresent(u ->
                messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(u, online)));
    }
}
