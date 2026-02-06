package com.ssafy.domain.dm.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.dm.dto.request.DirectMessageRequest;
import com.ssafy.domain.dm.dto.response.DirectMessageResponse;
import com.ssafy.domain.dm.dto.response.DmConversationResponse;
import com.ssafy.domain.dm.entity.DirectMessage;
import com.ssafy.domain.dm.entity.DmConversation;
import com.ssafy.domain.dm.mapper.DirectMessageMapper;
import com.ssafy.domain.dm.mapper.DmConversationMapper;
import com.ssafy.domain.friend.service.FriendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageMapper directMessageMapper;

    @Mock
    private DmConversationMapper dmConversationMapper;

    @Mock
    private FriendService friendService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DirectMessageService directMessageService;

    // ============================================================
    // DM 전송 테스트
    // ============================================================

    @Nested
    @DisplayName("DM 전송")
    class SendMessage {

        @Test
        @DisplayName("정상적인 DM 전송 성공")
        void sendMessage_WithValidInput_ShouldSucceed() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            DirectMessageRequest request = new DirectMessageRequest(receiverId, "안녕하세요!");

            DmConversation conversation = DmConversation.builder()
                    .id(1L)
                    .user1Id(senderId)
                    .user2Id(receiverId)
                    .user1Deleted(false)
                    .user2Deleted(false)
                    .build();

            DirectMessage savedMessage = DirectMessage.builder()
                    .id(1L)
                    .conversationId(1L)
                    .senderId(senderId)
                    .content("안녕하세요!")
                    .senderNickname("발신자")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(friendService.isBlocked(senderId, receiverId)).willReturn(false);
            given(friendService.isFriend(senderId, receiverId)).willReturn(true);
            given(dmConversationMapper.findByUsers(senderId, receiverId)).willReturn(conversation);

            // MyBatis가 insert 후 ID를 설정하는 것을 시뮬레이션
            doAnswer(invocation -> {
                DirectMessage m = invocation.getArgument(0);
                java.lang.reflect.Field idField = DirectMessage.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(m, 1L);
                return null;
            }).when(directMessageMapper).insert(any(DirectMessage.class));

            given(directMessageMapper.findById(1L)).willReturn(savedMessage);

            // when
            DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("안녕하세요!");
            assertThat(response.isMine()).isTrue();
            verify(directMessageMapper, times(1)).insert(any(DirectMessage.class));
            verify(dmConversationMapper, times(1)).updateLastMessageAt(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("자기 자신에게 DM 전송 시 예외 발생")
        void sendMessage_ToSelf_ShouldThrowException() {
            // given
            Long userId = 1L;
            DirectMessageRequest request = new DirectMessageRequest(userId, "안녕하세요!");

            // when & then
            assertThatThrownBy(() -> directMessageService.sendMessage(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자기 자신에게 DM을 보낼 수 없습니다");
        }

        @Test
        @DisplayName("차단된 사용자에게 DM 전송 시 예외 발생")
        void sendMessage_ToBlockedUser_ShouldThrowException() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            DirectMessageRequest request = new DirectMessageRequest(receiverId, "안녕하세요!");

            given(friendService.isBlocked(senderId, receiverId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> directMessageService.sendMessage(senderId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("차단된 사용자와는 DM을 주고받을 수 없습니다");
        }

        @Test
        @DisplayName("친구가 아닌 사용자에게 DM 전송 시 예외 발생")
        void sendMessage_ToNonFriend_ShouldThrowException() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            DirectMessageRequest request = new DirectMessageRequest(receiverId, "안녕하세요!");

            given(friendService.isBlocked(senderId, receiverId)).willReturn(false);
            given(friendService.isFriend(senderId, receiverId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> directMessageService.sendMessage(senderId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("친구만 DM을 보낼 수 있습니다");
        }

        @Test
        @DisplayName("새 대화방 생성 후 DM 전송")
        void sendMessage_CreatesNewConversation_ShouldSucceed() {
            // given
            Long senderId = 1L;
            Long receiverId = 2L;
            DirectMessageRequest request = new DirectMessageRequest(receiverId, "안녕하세요!");

            DmConversation newConversation = DmConversation.builder()
                    .id(1L)
                    .user1Id(senderId)
                    .user2Id(receiverId)
                    .build();

            DirectMessage savedMessage = DirectMessage.builder()
                    .id(1L)
                    .conversationId(1L)
                    .senderId(senderId)
                    .content("안녕하세요!")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(friendService.isBlocked(senderId, receiverId)).willReturn(false);
            given(friendService.isFriend(senderId, receiverId)).willReturn(true);
            given(dmConversationMapper.findByUsers(senderId, receiverId)).willReturn(null);

            // MyBatis가 insert 후 ID를 설정하는 것을 시뮬레이션
            doAnswer(invocation -> {
                DmConversation c = invocation.getArgument(0);
                java.lang.reflect.Field idField = DmConversation.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(c, 1L);
                return null;
            }).when(dmConversationMapper).insert(any(DmConversation.class));

            doAnswer(invocation -> {
                DirectMessage m = invocation.getArgument(0);
                java.lang.reflect.Field idField = DirectMessage.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(m, 1L);
                return null;
            }).when(directMessageMapper).insert(any(DirectMessage.class));

            given(dmConversationMapper.findById(1L)).willReturn(newConversation);
            given(directMessageMapper.findById(1L)).willReturn(savedMessage);

            // when
            DirectMessageResponse response = directMessageService.sendMessage(senderId, request);

            // then
            assertThat(response).isNotNull();
            verify(dmConversationMapper, times(1)).insert(any(DmConversation.class));
        }
    }

    // ============================================================
    // 대화방 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("대화방 목록 조회")
    class GetConversations {

        @Test
        @DisplayName("대화방 목록 정상 조회")
        void getConversations_ShouldReturnList() {
            // given
            Long userId = 1L;

            List<DmConversation> conversations = List.of(
                    DmConversation.builder()
                            .id(1L)
                            .user1Id(userId)
                            .user2Id(2L)
                            .user1LastReadMessageId(10L)
                            .user2Nickname("상대방1")
                            .lastMessageContent("마지막 메시지")
                            .lastMessageSenderId(userId)
                            .lastMessageAt(LocalDateTime.now())
                            .build()
            );

            given(dmConversationMapper.findByUserId(userId)).willReturn(conversations);
            given(directMessageMapper.countUnreadMessages(1L, 10L)).willReturn(0);

            // when
            List<DmConversationResponse> result = directMessageService.getConversations(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).partnerNickname()).isEqualTo("상대방1");
            assertThat(result.get(0).lastMessageIsMine()).isTrue();
        }
    }

    // ============================================================
    // 메시지 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("메시지 목록 조회")
    class GetMessages {

        @Test
        @DisplayName("메시지 목록 정상 조회")
        void getMessages_ShouldReturnList() {
            // given
            Long userId = 1L;
            Long conversationId = 1L;

            List<DirectMessage> messages = List.of(
                    DirectMessage.builder()
                            .id(1L)
                            .conversationId(conversationId)
                            .senderId(userId)
                            .content("안녕하세요")
                            .senderNickname("나")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    DirectMessage.builder()
                            .id(2L)
                            .conversationId(conversationId)
                            .senderId(2L)
                            .content("반갑습니다")
                            .senderNickname("상대방")
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            given(dmConversationMapper.existsByIdAndUserId(conversationId, userId)).willReturn(true);
            given(directMessageMapper.findByConversationId(conversationId, 50, 0)).willReturn(messages);

            // when
            List<DirectMessageResponse> result = directMessageService.getMessages(userId, conversationId, 0, 50);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).isMine()).isTrue();
            assertThat(result.get(1).isMine()).isFalse();
        }

        @Test
        @DisplayName("대화방 참여자가 아닌 경우 예외 발생")
        void getMessages_NotParticipant_ShouldThrowException() {
            // given
            Long userId = 3L;
            Long conversationId = 1L;

            given(dmConversationMapper.existsByIdAndUserId(conversationId, userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> directMessageService.getMessages(userId, conversationId, 0, 50))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("대화방에 접근할 수 없습니다");
        }
    }

    // ============================================================
    // 읽음 처리 테스트
    // ============================================================

    @Nested
    @DisplayName("읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("읽음 처리 성공 (user1)")
        void markAsRead_AsUser1_ShouldSucceed() {
            // given
            Long userId = 1L;
            Long conversationId = 1L;

            DmConversation conversation = DmConversation.builder()
                    .id(conversationId)
                    .user1Id(userId)
                    .user2Id(2L)
                    .build();

            DirectMessage latestMessage = DirectMessage.builder()
                    .id(10L)
                    .conversationId(conversationId)
                    .build();

            given(dmConversationMapper.findById(conversationId)).willReturn(conversation);
            given(directMessageMapper.findLatestByConversationId(conversationId)).willReturn(latestMessage);

            // when
            directMessageService.markAsRead(userId, conversationId);

            // then
            verify(dmConversationMapper, times(1)).updateUser1LastReadMessageId(conversationId, 10L);
        }

        @Test
        @DisplayName("존재하지 않는 대화방 읽음 처리 시 예외 발생")
        void markAsRead_ConversationNotFound_ShouldThrowException() {
            // given
            Long userId = 1L;
            Long conversationId = 999L;

            given(dmConversationMapper.findById(conversationId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> directMessageService.markAsRead(userId, conversationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("대화방을 찾을 수 없습니다");
        }
    }

    // ============================================================
    // 대화방 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("대화방 삭제")
    class DeleteConversation {

        @Test
        @DisplayName("대화방 삭제 성공 (user1)")
        void deleteConversation_AsUser1_ShouldSucceed() {
            // given
            Long userId = 1L;
            Long conversationId = 1L;

            DmConversation conversation = DmConversation.builder()
                    .id(conversationId)
                    .user1Id(userId)
                    .user2Id(2L)
                    .build();

            given(dmConversationMapper.findById(conversationId)).willReturn(conversation);

            // when
            directMessageService.deleteConversation(userId, conversationId);

            // then
            verify(dmConversationMapper, times(1)).markUser1Deleted(conversationId);
        }

        @Test
        @DisplayName("참여자가 아닌 경우 예외 발생")
        void deleteConversation_NotParticipant_ShouldThrowException() {
            // given
            Long userId = 3L;
            Long conversationId = 1L;

            DmConversation conversation = DmConversation.builder()
                    .id(conversationId)
                    .user1Id(1L)
                    .user2Id(2L)
                    .build();

            given(dmConversationMapper.findById(conversationId)).willReturn(conversation);

            // when & then
            assertThatThrownBy(() -> directMessageService.deleteConversation(userId, conversationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("대화방에 접근할 수 없습니다");
        }
    }

    // ============================================================
    // 안 읽은 메시지 개수 테스트
    // ============================================================

    @Nested
    @DisplayName("안 읽은 메시지 개수")
    class GetTotalUnreadCount {

        @Test
        @DisplayName("안 읽은 메시지 총 개수 조회")
        void getTotalUnreadCount_ShouldReturnSum() {
            // given
            Long userId = 1L;

            List<DmConversation> conversations = List.of(
                    DmConversation.builder()
                            .id(1L)
                            .user1Id(userId)
                            .user2Id(2L)
                            .user1LastReadMessageId(5L)
                            .build(),
                    DmConversation.builder()
                            .id(2L)
                            .user1Id(userId)
                            .user2Id(3L)
                            .user1LastReadMessageId(10L)
                            .build()
            );

            given(dmConversationMapper.findByUserId(userId)).willReturn(conversations);
            given(directMessageMapper.countUnreadMessages(1L, 5L)).willReturn(3);
            given(directMessageMapper.countUnreadMessages(2L, 10L)).willReturn(2);

            // when
            int count = directMessageService.getTotalUnreadCount(userId);

            // then
            assertThat(count).isEqualTo(5);
        }
    }
}
