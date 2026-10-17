package com.example.modularchatapi.modules.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Chat modülü kendi socket kurallarını common'a dokunmadan ekler.
// Order: common'daki JWT interceptor'dan SONRA çalışır (kullanıcı önce doğrulanmış olur).
@Configuration
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ConversationSubscriptionGuard subscriptionGuard;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionGuard);
    }
}
