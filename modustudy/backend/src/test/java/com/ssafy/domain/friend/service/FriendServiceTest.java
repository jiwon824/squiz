package com.ssafy.domain.friend.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.friend.dto.response.*;
import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.entity.FriendshipStatus;
import com.ssafy.domain.friend.entity.UserBlock;
import com.ssafy.domain.friend.mapper.FriendshipMapper;
import com.ssafy.domain.friend.mapper.UserBlockMapper;
import com.ssafy.domain.friend.mapper.UserSearchMapper;
import com.ssafy.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriendServiceTest {

    @Mock
    private FriendshipMapper friendshipMapper;

    @Mock
    private UserBlockMapper userBlockMapper;

    @Mock
    private UserSearchMapper userSearchMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FriendService friendService;

    // ============================================================
    // 사용자 검색 테스트
    // ============================================================

    @Nested
    @DisplayName("사용자 검색")
    class SearchUsers {

        @Test
        @DisplayName("정상 검색 - 결과 반환")
        void searchUsers_WithValidKeyword_ShouldReturnResults() {
            // given
            Long myId = 1L;
            String keyword = "테스트";

            List<UserSearchResponse> mockResults = List.of(
                    new UserSearchResponse(2L, "테스트유저", null, true, UserSearchResponse.FriendStatus.NONE)
            );

            given(userBlockMapper.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
            given(userSearchMapper.searchByNickname(eq(keyword), eq(myId), anyList())).willReturn(mockResults);
            given(userBlockMapper.existsByBlockerAndBlocked(myId, 2L)).willReturn(false);
            given(friendshipMapper.findByUsers(myId, 2L)).willReturn(null);

            // when
            List<UserSearchResponse> results = friendService.searchUsers(myId, keyword);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).nickname()).isEqualTo("테스트유저");
            assertThat(results.get(0).friendStatus()).isEqualTo(UserSearchResponse.FriendStatus.NONE);
        }

        @Test
        @DisplayName("키워드가 2자 미만이면 빈 결과 반환")
        void searchUsers_WithShortKeyword_ShouldReturnEmpty() {
            // given
            Long myId = 1L;
            String keyword = "테";

            // when
            List<UserSearchResponse> results = friendService.searchUsers(myId, keyword);

            // then
            assertThat(results).isEmpty();
            verify(userSearchMapper, never()).searchByNickname(anyString(), anyLong(), anyList());
        }

        @Test
        @DisplayName("null 키워드면 빈 결과 반환")
        void searchUsers_WithNullKeyword_ShouldReturnEmpty() {
            // given
            Long myId = 1L;

            // when
            List<UserSearchResponse> results = friendService.searchUsers(myId, null);

            // then
            assertThat(results).isEmpty();
        }
    }

    // ============================================================
    // 친구 요청 테스트
    // ============================================================

    @Nested
    @DisplayName("친구 요청 보내기")
    class SendFriendRequest {

        @Test
        @DisplayName("정상적인 친구 요청 성공")
        void sendFriendRequest_WithValidInput_ShouldSucceed() {
            // given
            Long requesterId = 1L;
            Long addresseeId = 2L;

            Friendship savedFriendship = Friendship.builder()
                    .id(1L)
                    .requesterId(requesterId)
                    .addresseeId(addresseeId)
                    .status(FriendshipStatus.PENDING)
                    .addresseeNickname("친구")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userBlockMapper.existsByBlockerAndBlocked(addresseeId, requesterId)).willReturn(false);
            given(userBlockMapper.existsByBlockerAndBlocked(requesterId, addresseeId)).willReturn(false);
            given(friendshipMapper.findByUsers(requesterId, addresseeId)).willReturn(null);

            // MyBatis가 insert 후 ID를 설정하는 것을 시뮬레이션
            doAnswer(invocation -> {
                Friendship f = invocation.getArgument(0);
                java.lang.reflect.Field idField = Friendship.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(f, 1L);
                return null;
            }).when(friendshipMapper).insert(any(Friendship.class));

            given(friendshipMapper.findById(1L)).willReturn(savedFriendship);

            // when
            FriendRequestResponse response = friendService.sendFriendRequest(requesterId, addresseeId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(addresseeId);
            verify(friendshipMapper, times(1)).insert(any(Friendship.class));
        }

        @Test
        @DisplayName("자기 자신에게 친구 요청 시 예외 발생")
        void sendFriendRequest_ToSelf_ShouldThrowException() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> friendService.sendFriendRequest(userId, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자기 자신에게 친구 요청을 보낼 수 없습니다");
        }

        @Test
        @DisplayName("상대방이 나를 차단한 경우 예외 발생")
        void sendFriendRequest_WhenBlockedByTarget_ShouldThrowException() {
            // given
            Long requesterId = 1L;
            Long addresseeId = 2L;

            given(userBlockMapper.existsByBlockerAndBlocked(addresseeId, requesterId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendService.sendFriendRequest(requesterId, addresseeId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("상대방이 차단한 사용자입니다");
        }

        @Test
        @DisplayName("내가 차단한 사용자에게 친구 요청 시 예외 발생")
        void sendFriendRequest_ToBlockedUser_ShouldThrowException() {
            // given
            Long requesterId = 1L;
            Long addresseeId = 2L;

            given(userBlockMapper.existsByBlockerAndBlocked(addresseeId, requesterId)).willReturn(false);
            given(userBlockMapper.existsByBlockerAndBlocked(requesterId, addresseeId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendService.sendFriendRequest(requesterId, addresseeId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("차단한 사용자에게 친구 요청을 보낼 수 없습니다");
        }

        @Test
        @DisplayName("이미 친구인 경우 예외 발생")
        void sendFriendRequest_AlreadyFriend_ShouldThrowException() {
            // given
            Long requesterId = 1L;
            Long addresseeId = 2L;

            Friendship existingFriendship = Friendship.builder()
                    .id(1L)
                    .requesterId(requesterId)
                    .addresseeId(addresseeId)
                    .status(FriendshipStatus.ACCEPTED)
                    .build();

            given(userBlockMapper.existsByBlockerAndBlocked(anyLong(), anyLong())).willReturn(false);
            given(friendshipMapper.findByUsers(requesterId, addresseeId)).willReturn(existingFriendship);

            // when & then
            assertThatThrownBy(() -> friendService.sendFriendRequest(requesterId, addresseeId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 친구입니다");
        }
    }

    // ============================================================
    // 친구 요청 수락 테스트
    // ============================================================

    @Nested
    @DisplayName("친구 요청 수락")
    class AcceptFriendRequest {

        @Test
        @DisplayName("정상적인 친구 요청 수락 성공")
        void acceptFriendRequest_WithValidInput_ShouldSucceed() {
            // given
            Long userId = 2L;
            Long requestId = 1L;

            Friendship pendingRequest = Friendship.builder()
                    .id(requestId)
                    .requesterId(1L)
                    .addresseeId(userId)
                    .status(FriendshipStatus.PENDING)
                    .requesterNickname("요청자")
                    .build();

            Friendship acceptedRequest = Friendship.builder()
                    .id(requestId)
                    .requesterId(1L)
                    .addresseeId(userId)
                    .status(FriendshipStatus.ACCEPTED)
                    .requesterNickname("요청자")
                    .build();

            given(friendshipMapper.findById(requestId)).willReturn(pendingRequest, acceptedRequest);

            // when
            FriendResponse response = friendService.acceptFriendRequest(userId, requestId);

            // then
            assertThat(response).isNotNull();
            verify(friendshipMapper, times(1)).updateStatus(requestId, FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("존재하지 않는 요청 수락 시 예외 발생")
        void acceptFriendRequest_NotFound_ShouldThrowException() {
            // given
            Long userId = 2L;
            Long requestId = 999L;

            given(friendshipMapper.findById(requestId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(userId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("친구 요청을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("요청 받은 사람이 아닌 경우 예외 발생")
        void acceptFriendRequest_NotAddressee_ShouldThrowException() {
            // given
            Long userId = 3L;  // 요청 받은 사람이 아님
            Long requestId = 1L;

            Friendship pendingRequest = Friendship.builder()
                    .id(requestId)
                    .requesterId(1L)
                    .addresseeId(2L)  // 다른 사용자
                    .status(FriendshipStatus.PENDING)
                    .build();

            given(friendshipMapper.findById(requestId)).willReturn(pendingRequest);

            // when & then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(userId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("친구 요청을 수락할 권한이 없습니다");
        }
    }

    // ============================================================
    // 친구 요청 거절 테스트
    // ============================================================

    @Nested
    @DisplayName("친구 요청 거절")
    class RejectFriendRequest {

        @Test
        @DisplayName("정상적인 친구 요청 거절 성공")
        void rejectFriendRequest_WithValidInput_ShouldSucceed() {
            // given
            Long userId = 2L;
            Long requestId = 1L;

            Friendship pendingRequest = Friendship.builder()
                    .id(requestId)
                    .requesterId(1L)
                    .addresseeId(userId)
                    .status(FriendshipStatus.PENDING)
                    .build();

            given(friendshipMapper.findById(requestId)).willReturn(pendingRequest);

            // when
            friendService.rejectFriendRequest(userId, requestId);

            // then
            verify(friendshipMapper, times(1)).delete(requestId);
        }
    }

    // ============================================================
    // 친구 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("친구 목록 조회")
    class GetFriends {

        @Test
        @DisplayName("친구 목록 정상 조회")
        void getFriends_ShouldReturnFriendList() {
            // given
            Long userId = 1L;

            List<Friendship> friendships = List.of(
                    Friendship.builder()
                            .id(1L)
                            .requesterId(userId)
                            .addresseeId(2L)
                            .status(FriendshipStatus.ACCEPTED)
                            .addresseeNickname("친구1")
                            .addresseeIsOnline(true)
                            .build(),
                    Friendship.builder()
                            .id(2L)
                            .requesterId(3L)
                            .addresseeId(userId)
                            .status(FriendshipStatus.ACCEPTED)
                            .requesterNickname("친구2")
                            .requesterIsOnline(false)
                            .build()
            );

            given(friendshipMapper.findFriends(userId)).willReturn(friendships);

            // when
            List<FriendResponse> friends = friendService.getFriends(userId);

            // then
            assertThat(friends).hasSize(2);
        }
    }

    // ============================================================
    // 사용자 차단 테스트
    // ============================================================

    @Nested
    @DisplayName("사용자 차단")
    class BlockUser {

        @Test
        @DisplayName("정상적인 사용자 차단 성공")
        void blockUser_WithValidInput_ShouldSucceed() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            given(userBlockMapper.existsByBlockerAndBlocked(blockerId, blockedId)).willReturn(false);
            given(friendshipMapper.findByUsers(blockerId, blockedId)).willReturn(null);

            // when
            friendService.blockUser(blockerId, blockedId);

            // then
            verify(userBlockMapper, times(1)).insert(any(UserBlock.class));
        }

        @Test
        @DisplayName("자기 자신 차단 시 예외 발생")
        void blockUser_Self_ShouldThrowException() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> friendService.blockUser(userId, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자기 자신을 차단할 수 없습니다");
        }

        @Test
        @DisplayName("이미 차단한 사용자 차단 시 예외 발생")
        void blockUser_AlreadyBlocked_ShouldThrowException() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            given(userBlockMapper.existsByBlockerAndBlocked(blockerId, blockedId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendService.blockUser(blockerId, blockedId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 차단한 사용자입니다");
        }

        @Test
        @DisplayName("차단 시 기존 친구 관계 삭제")
        void blockUser_WithExistingFriendship_ShouldDeleteFriendship() {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            Friendship existingFriendship = Friendship.builder()
                    .id(1L)
                    .requesterId(blockerId)
                    .addresseeId(blockedId)
                    .status(FriendshipStatus.ACCEPTED)
                    .build();

            given(userBlockMapper.existsByBlockerAndBlocked(blockerId, blockedId)).willReturn(false);
            given(friendshipMapper.findByUsers(blockerId, blockedId)).willReturn(existingFriendship);

            // when
            friendService.blockUser(blockerId, blockedId);

            // then
            verify(userBlockMapper, times(1)).insert(any(UserBlock.class));
            verify(friendshipMapper, times(1)).delete(existingFriendship.getId());
        }
    }

    // ============================================================
    // 친구/차단 여부 확인 테스트
    // ============================================================

    @Nested
    @DisplayName("친구/차단 여부 확인")
    class CheckRelationship {

        @Test
        @DisplayName("친구 여부 확인 - 친구인 경우")
        void isFriend_WhenFriends_ShouldReturnTrue() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;

            Friendship friendship = Friendship.builder()
                    .status(FriendshipStatus.ACCEPTED)
                    .build();

            given(friendshipMapper.findByUsers(userId1, userId2)).willReturn(friendship);

            // when
            boolean result = friendService.isFriend(userId1, userId2);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("친구 여부 확인 - 친구 아닌 경우")
        void isFriend_WhenNotFriends_ShouldReturnFalse() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;

            given(friendshipMapper.findByUsers(userId1, userId2)).willReturn(null);

            // when
            boolean result = friendService.isFriend(userId1, userId2);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("차단 여부 확인")
        void isBlocked_ShouldReturnCorrectResult() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;

            given(userBlockMapper.existsAnyBlock(userId1, userId2)).willReturn(true);

            // when
            boolean result = friendService.isBlocked(userId1, userId2);

            // then
            assertThat(result).isTrue();
        }
    }
}
