package com.example.modularchatapi.modules.chat.websocket;

import com.example.modularchatapi.modules.chat.service.ChatService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Kullanıcı sadece üyesi olduğu sohbetin kanalına abone olabilir: /topic/conversations/{id}[/typing]
@Component
public class ConversationSubscriptionGuard implements ChannelInterceptor {

    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/conversations/(\\d+)(/typing)?$");

    private final ChatService chatService;

    // @Lazy: ChatService -> SimpMessagingTemplate -> broker config -> bu interceptor döngüsünü kırar
    public ConversationSubscriptionGuard(@Lazy ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        Matcher m = CONVERSATION_TOPIC.matcher(destination);
        if (m.matches()) {
            Long conversationId = Long.valueOf(m.group(1));
            Long userId = chatService.requireUser(accessor.getUser().getName()).id();
            if (!chatService.isParticipant(userId, conversationId)) {
                throw new MessageDeliveryException("Bu sohbete abone olma yetkiniz yok");
            }
        }
        return message;
    }
}
