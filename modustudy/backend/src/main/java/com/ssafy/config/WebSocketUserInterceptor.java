package com.ssafy.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

/**
 * WebSocket 메시지에서 userId를 Principal로 설정하는 Interceptor
 * convertAndSendToUser()가 정상 작동하도록 함
 */
 public class WebSocketUserInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 시 userId 헤더에서 Principal 설정
            String userId = accessor.getFirstNativeHeader("userId");
            if (userId != null) {
                accessor.setUser(new UserPrincipal(userId));
            }
        }

        return message;
    }

    /**
     * 간단한 Principal 구현
     */
    public static class UserPrincipal implements Principal {
        private final String name;

        public UserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
