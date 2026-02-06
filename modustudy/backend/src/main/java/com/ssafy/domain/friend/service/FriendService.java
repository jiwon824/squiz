package com.ssafy.domain.friend.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.friend.dto.response.*;
import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.entity.FriendshipStatus;
import com.ssafy.domain.friend.entity.UserBlock;
import com.ssafy.domain.friend.mapper.FriendshipMapper;
import com.ssafy.domain.friend.mapper.UserBlockMapper;
import com.ssafy.domain.friend.mapper.UserSearchMapper;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipMapper friendshipMapper;
    private final UserBlockMapper userBlockMapper;
    private final UserSearchMapper userSearchMapper;
    private final NotificationService notificationService;

    /**
     * 사용자 검색 (닉네임)
     */
    public List<UserSearchResponse> searchUsers(Long myId, String keyword) {
        if (keyword == null || keyword.length() < 2) {
            return Collections.emptyList();
        }

        // 나를 차단한 사용자 ID 목록
        List<Long> blockedByIds = userBlockMapper.findBlockerIdsByBlockedId(myId);

        // 검색 결과
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(keyword, myId, blockedByIds);

        // 친구 상태 업데이트
        return results.stream()
                .map(user -> {
                    UserSearchResponse.FriendStatus status = getFriendStatus(myId, user.userId());
                    return new UserSearchResponse(
                            user.userId(),
                            user.nickname(),
                            user.profileImage(),
                            user.isOnline(),
                            status
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 친구 상태 확인
     */
    private UserSearchResponse.FriendStatus getFriendStatus(Long myId, Long targetId) {
        // 내가 차단했는지
        if (userBlockMapper.existsByBlockerAndBlocked(myId, targetId)) {
            return UserSearchResponse.FriendStatus.BLOCKED;
        }

        // 친구 관계 확인
        Friendship friendship = friendshipMapper.findByUsers(myId, targetId);
        if (friendship == null) {
            return UserSearchResponse.FriendStatus.NONE;
        }

        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            return UserSearchResponse.FriendStatus.FRIEND;
        }

        // PENDING 상태
        if (friendship.getRequesterId().equals(myId)) {
            return UserSearchResponse.FriendStatus.PENDING_SENT;
        }
        return UserSearchResponse.FriendStatus.PENDING_RECEIVED;
    }

    /**
     * 친구 요청 보내기
     */
    @Transactional
    public FriendRequestResponse sendFriendRequest(Long requesterId, Long addresseeId) {
        // 자기 자신에게 요청 불가
        if (requesterId.equals(addresseeId)) {
            throw new BusinessException("INVALID_REQUEST", "자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        // 차단 여부 확인
        if (userBlockMapper.existsByBlockerAndBlocked(addresseeId, requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "상대방이 차단한 사용자입니다.");
        }
        if (userBlockMapper.existsByBlockerAndBlocked(requesterId, addresseeId)) {
            throw new BusinessException("INVALID_REQUEST", "차단한 사용자에게 친구 요청을 보낼 수 없습니다.");
        }

        // 이미 관계가 있는지 확인
        Friendship existing = friendshipMapper.findByUsers(requesterId, addresseeId);
        if (existing != null) {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException("INVALID_REQUEST", "이미 친구입니다.");
            }
            throw new BusinessException("INVALID_REQUEST", "이미 친구 요청이 존재합니다.");
        }

        // 친구 요청 생성
        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipMapper.insert(friendship);

        // 저장된 데이터 다시 조회 (사용자 정보 포함)
        Friendship saved = friendshipMapper.findById(friendship.getId());

        // 친구 요청 알림 생성 (요청 받은 사람에게)
        notificationService.createNotification(
                addresseeId,
                NotificationType.FRIEND,
                "친구 요청",
                saved.getRequesterNickname() + "님이 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST",
                saved.getId()
        );

        return FriendRequestResponse.fromSent(saved);
    }

    /**
     * 받은 친구 요청 목록
     */
    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        return friendshipMapper.findReceivedRequests(userId)
                .stream()
                .map(FriendRequestResponse::fromReceived)
                .collect(Collectors.toList());
    }

    /**
     * 보낸 친구 요청 목록
     */
    public List<FriendRequestResponse> getSentRequests(Long userId) {
        return friendshipMapper.findSentRequests(userId)
                .stream()
                .map(FriendRequestResponse::fromSent)
                .collect(Collectors.toList());
    }

    /**
     * 친구 요청 수락
     */
    @Transactional
    public FriendResponse acceptFriendRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipMapper.findById(requestId);
        if (friendship == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "친구 요청을 찾을 수 없습니다.");
        }

        // 요청 받은 사람만 수락 가능
        if (!friendship.getAddresseeId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "친구 요청을 수락할 권한이 없습니다.");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException("INVALID_REQUEST", "이미 처리된 요청입니다.");
        }

        friendshipMapper.updateStatus(requestId, FriendshipStatus.ACCEPTED);

        // 업데이트된 데이터 조회
        Friendship updated = friendshipMapper.findById(requestId);

        // 친구 수락 알림 생성 (요청 보낸 사람에게)
        notificationService.createNotification(
                updated.getRequesterId(),
                NotificationType.FRIEND,
                "친구 수락",
                updated.getAddresseeNickname() + "님이 친구 요청을 수락했습니다.",
                "FRIEND_ACCEPT",
                updated.getId()
        );

        return FriendResponse.from(updated, userId);
    }

    /**
     * 친구 요청 거절
     */
    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipMapper.findById(requestId);
        if (friendship == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "친구 요청을 찾을 수 없습니다.");
        }

        // 요청 받은 사람만 거절 가능
        if (!friendship.getAddresseeId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "친구 요청을 거절할 권한이 없습니다.");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException("INVALID_REQUEST", "이미 처리된 요청입니다.");
        }

        friendshipMapper.delete(requestId);
    }

    /**
     * 친구 목록 조회
     */
    public List<FriendResponse> getFriends(Long userId) {
        return friendshipMapper.findFriends(userId)
                .stream()
                .map(f -> FriendResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    /**
     * 친구 삭제
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.findByIdAndUserId(friendshipId, userId);
        if (friendship == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "친구 관계를 찾을 수 없습니다.");
        }

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BusinessException("INVALID_REQUEST", "친구 관계가 아닙니다.");
        }

        friendshipMapper.delete(friendshipId);
    }

    /**
     * 사용자 차단
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException("INVALID_REQUEST", "자기 자신을 차단할 수 없습니다.");
        }

        if (userBlockMapper.existsByBlockerAndBlocked(blockerId, blockedId)) {
            throw new BusinessException("INVALID_REQUEST", "이미 차단한 사용자입니다.");
        }

        // 차단 생성
        UserBlock block = UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        userBlockMapper.insert(block);

        // 친구 관계가 있으면 삭제
        Friendship friendship = friendshipMapper.findByUsers(blockerId, blockedId);
        if (friendship != null) {
            friendshipMapper.delete(friendship.getId());
        }
    }

    /**
     * 차단 해제
     */
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        UserBlock block = userBlockMapper.findByBlockerAndBlocked(blockerId, blockedId);
        if (block == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "차단 관계를 찾을 수 없습니다.");
        }

        userBlockMapper.delete(block.getId());
    }

    /**
     * 차단 목록 조회
     */
    public List<BlockedUserResponse> getBlockedUsers(Long userId) {
        return userBlockMapper.findByBlockerId(userId)
                .stream()
                .map(BlockedUserResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 친구 여부 확인 (DM에서 사용)
     */
    public boolean isFriend(Long userId1, Long userId2) {
        Friendship friendship = friendshipMapper.findByUsers(userId1, userId2);
        return friendship != null && friendship.getStatus() == FriendshipStatus.ACCEPTED;
    }

    /**
     * 차단 여부 확인 (DM에서 사용)
     */
    public boolean isBlocked(Long userId1, Long userId2) {
        return userBlockMapper.existsAnyBlock(userId1, userId2);
    }
}
