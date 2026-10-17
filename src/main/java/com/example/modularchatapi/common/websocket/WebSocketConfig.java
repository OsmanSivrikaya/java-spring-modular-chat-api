package com.example.modularchatapi.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// .NET'teki app.MapHub<ChatHub>("/hub") karşılığı.
// SignalR yerine STOMP protokolü kullanılıyor:
//   client -> /app/...      : sunucudaki @MessageMapping metotlarına gider (Hub metodu çağırmak gibi)
//   server -> /topic/...    : o kanala abone olan herkese yayın (Clients.Group(...))
//   server -> /user/queue/..: tek bir kullanıcıya özel (Clients.User(...))
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // kimlik doğrulama interceptor'ı diğer modüllerinkinden önce çalışsın
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompInterceptor jwtStompInterceptor;

    // Interface'in metotları default (boş gövdeli), sadece ihtiyacım olanları override ediyorum.

    // İstemcinin bağlanacağı adres. .NET'teki app.MapHub<ChatHub>("/hub") gibi.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ws://localhost:8080/ws adresinden bağlanılıyor.
        // "*" her domainden bağlantıya izin veriyor (CORS). Canlıda kendi domainimle sınırlamam lazım.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        // SockJS: WebSocket desteklemeyen ortamlar için yedek yol.
        // SignalR'ın WebSocket olmayınca long polling'e düşmesi gibi.
        registry.addEndpoint("/ws-sockjs").setAllowedOriginPatterns("*").withSockJS();
    }

    // Mesajların adrese göre nereye gideceğini burada ayarlıyorum.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic ve /queue ile başlayan adreslere gelen mesajları abone olanlara dağıt.
        // Bellekte çalışan basit broker. Birden fazla sunucuya çıkarsam RabbitMQ gerekecek
        // (SignalR'daki Redis backplane gibi).
        // mesaj kodda işlem yapmasın doğrudan belirtilen gruba gitsin diye bu
        registry.enableSimpleBroker("/topic", "/queue");
        // İstemci /app/... adresine mesaj atınca @MessageMapping metotlarım çalışsın.
        // /app/chat.send -> ChatSocketController.send() (Hub metodu çağırmak gibi)
        registry.setApplicationDestinationPrefixes("/app");
        // /user/... kişiye özel adresler. convertAndSendToUser bunu kullanıyor (Clients.User gibi).
        // mesaj sadece aliye gitsin başkası görmesin diye buna sub oluyor
        registry.setUserDestinationPrefix("/user");
    }

    // İstemciden gelen her mesajın geçtiği kanal. HTTP'deki filter zincirinin socket hali.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Her mesaj önce JWT kontrolünden geçsin, token yoksa bağlantıyı kabul etmesin.
        registration.interceptors(jwtStompInterceptor);
    }
}
