package com.ssafy.domain.dm.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Pub/Sub 구독자
 * 다른 서버에서 발행한 DM 메시지를 수신하여 로컬 WebSocket 세션에 전달
 */
@Service
public class DmRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(DmRedisSubscriber.class);
    private static final String DM_CHANNEL_PREFIX = "dm:user:";

    private final RedisMessageListenerContainer listenerContainer;
    private final DmSessionService dmSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // 구독 중인 채널 관리 (userId -> ChannelTopic)
    private final Map<Long, ChannelTopic> subscribedChannels = new ConcurrentHashMap<>();

    public DmRedisSubscriber(RedisMessageListenerContainer listenerContainer,
                              DmSessionService dmSessionService,
                              SimpMessagingTemplate messagingTemplate,
                              ObjectMapper redisObjectMapper) {
        this.listenerContainer = listenerContainer;
        this.dmSessionService = dmSessionService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = redisObjectMapper;
    }

    /**
     * 사용자 채널 구독 시작
     */
    public void subscribeUser(Long userId) {
        if (subscribedChannels.containsKey(userId)) {
            return;
        }
        String channelName = DM_CHANNEL_PREFIX + userId;
        ChannelTopic topic = new ChannelTopic(channelName);
        listenerContainer.addMessageListener(this, topic);
        subscribedChannels.put(userId, topic);
        log.debug("Subscribed to Redis channel: {}", channelName);
    }

    /**
     * 사용자 채널 구독 해제
     */
    public void unsubscribeUser(Long userId) {
        ChannelTopic topic = subscribedChannels.remove(userId);
        if (topic != null) {
            listenerContainer.removeMessageListener(this, topic);
            log.debug("Unsubscribed from Redis channel: {}", topic.getTopic());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            DmRedisPublisher.DmRedisMessage dmMessage =
                    objectMapper.readValue(json, DmRedisPublisher.DmRedisMessage.class);

            Long userId = dmMessage.getUserId();
            String destination = dmMessage.getDestination();
            Object payload = dmMessage.getPayload();

            // Principal name(userId)을 사용하여 사용자에게 메시지 전달
            // WebSocketUserInterceptor에서 userId를 Principal로 설정했으므로
            // convertAndSendToUser의 첫 번째 인자로 userId 문자열 사용
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),  // Principal name = userId
                    destination,
                    payload
            );
            log.debug("Forwarded Redis message to userId={} via Principal", userId);

        } catch (Exception e) {
            log.error("Failed to process Redis message", e);
        }
    }
}
