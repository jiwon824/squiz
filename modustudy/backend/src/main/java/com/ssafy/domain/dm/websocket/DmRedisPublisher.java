package com.ssafy.domain.dm.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub를 통한 DM 메시지 발행
 * Blue/Green 배포 시 서버 간 메시지 전달
 */
@Service
public class DmRedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(DmRedisPublisher.class);
    private static final String DM_CHANNEL_PREFIX = "dm:user:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public DmRedisPublisher(RedisTemplate<String, Object> redisTemplate,
                            ObjectMapper redisObjectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = redisObjectMapper;
    }

    /**
     * 특정 사용자에게 DM 이벤트 발행
     */
    public void publishToUser(Long userId, String destination, Object payload) {
        DmRedisMessage message = new DmRedisMessage(userId, destination, payload);
        String channel = DM_CHANNEL_PREFIX + userId;
        redisTemplate.convertAndSend(channel, message);
        log.debug("Published DM to Redis: channel={}, destination={}", channel, destination);
    }

    /**
     * Redis 메시지 래퍼
     */
    public static class DmRedisMessage {
        private Long userId;
        private String destination;
        private Object payload;

        public DmRedisMessage() {}

        public DmRedisMessage(Long userId, String destination, Object payload) {
            this.userId = userId;
            this.destination = destination;
            this.payload = payload;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }
}
