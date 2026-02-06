package com.ssafy.domain.friend.websocket;

import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.mapper.FriendshipMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendPresenceService {

    private final FriendshipMapper friendshipMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPresence(Long userId, Boolean isOnline, LocalDateTime lastSeenAt) {
        List<Friendship> friends = friendshipMapper.findFriends(userId);
        if (friends == null || friends.isEmpty()) {
            return;
        }

        FriendPresenceEvent event = new FriendPresenceEvent(userId, isOnline, lastSeenAt);
        for (Friendship friendship : friends) {
            Long friendId = friendship.getRequesterId().equals(userId)
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId();
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(friendId),
                    "/queue/friends/presence",
                    event
            );
        }
    }
}
