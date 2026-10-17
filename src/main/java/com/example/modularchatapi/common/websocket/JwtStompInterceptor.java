package com.example.modularchatapi.common.websocket;

import com.example.modularchatapi.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

// SignalR'da Hub'a [Authorize] koyup token'ı access_token ile yolluyordum.
// Burada socket'e gelen her mesaj önce bu interceptor'dan geçiyor (HTTP filter'ın socket hali gibi).
// İstemci bağlanırken STOMP CONNECT mesajına "Authorization: Bearer <token>" header'ı koyuyor,
// ben de burada kontrol ediyorum. Token yoksa ya da bozuksa bağlantıyı reddediyorum.
@Component
@RequiredArgsConstructor
public class JwtStompInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // preSend: mesaj işlenmeden hemen önce çalışıyor. Exception atarsam mesaj hiç işlenmiyor.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Authorization header eksik");
            }
            String token = header.substring(7);
            try {
                UserDetails user = userDetailsService.loadUserByUsername(jwtService.extractSubject(token));
                if (!jwtService.isTokenValid(token, user)) {
                    throw new MessageDeliveryException("Geçersiz token");
                }
                // Bu kullanıcı oturum boyunca Principal olarak kalır (Principal.getName() = email)
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            } catch (JwtException | UsernameNotFoundException e) {
                throw new MessageDeliveryException("Geçersiz token");
            }
        // CONNECT olmadan mesaj atmaya ya da abone olmaya çalışan olursa engelliyorum
        } else if (isClientCommand(accessor.getCommand()) && accessor.getUser() == null) {
            throw new MessageDeliveryException("Kimlik doğrulanmamış bağlantı");
        }
        return message;
    }

    private boolean isClientCommand(StompCommand command) {
        return command == StompCommand.SEND || command == StompCommand.SUBSCRIBE;
    }
}
