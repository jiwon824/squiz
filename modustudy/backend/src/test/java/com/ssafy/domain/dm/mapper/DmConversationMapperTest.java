package com.ssafy.domain.dm.mapper;

import com.ssafy.domain.dm.entity.DirectMessage;
import com.ssafy.domain.dm.entity.DmConversation;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("DmConversationMapper 통합 테스트")
class DmConversationMapperTest {

    @Autowired
    private DmConversationMapper dmConversationMapper;

    @Autowired
    private DirectMessageMapper directMessageMapper;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .userId("convtest1")
                .email("conv1@test.com")
                .nickname("대화테스트1")
                .name("Conv Test 1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user2 = userRepository.save(User.builder()
                .userId("convtest2")
                .email("conv2@test.com")
                .nickname("대화테스트2")
                .name("Conv Test 2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user3 = userRepository.save(User.builder()
                .userId("convtest3")
                .email("conv3@test.com")
                .nickname("대화테스트3")
                .name("Conv Test 3")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
    }

    @Test
    @DisplayName("대화방 생성 및 ID로 조회")
    void insert_and_findById() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();

        // when
        dmConversationMapper.insert(conversation);

        // then
        assertThat(conversation.getId()).isNotNull();

        DmConversation found = dmConversationMapper.findById(conversation.getId());
        assertThat(found).isNotNull();
        assertThat(found.getUser1Id()).isEqualTo(user1.getId());
        assertThat(found.getUser2Id()).isEqualTo(user2.getId());
        // profile JOIN 확인
        assertThat(found.getUser1Nickname()).isEqualTo("대화테스트1");
        assertThat(found.getUser2Nickname()).isEqualTo("대화테스트2");
        assertThat(found.getUser1IsOnline()).isTrue();
        assertThat(found.getUser2IsOnline()).isFalse();
    }

    @Test
    @DisplayName("두 사용자 간 대화방 조회")
    void findByUsers() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);

        // when - 정방향
        DmConversation found1 = dmConversationMapper.findByUsers(user1.getId(), user2.getId());
        // when - 역방향
        DmConversation found2 = dmConversationMapper.findByUsers(user2.getId(), user1.getId());

        // then
        assertThat(found1).isNotNull();
        assertThat(found2).isNotNull();
        assertThat(found1.getId()).isEqualTo(found2.getId());
    }

    @Test
    @DisplayName("사용자의 대화방 목록 조회")
    void findByUserId() {
        // given - user1과 user2, user1과 user3의 대화방 생성
        DmConversation conv1 = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conv1);

        DmConversation conv2 = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user3.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conv2);

        // when
        List<DmConversation> user1Conversations = dmConversationMapper.findByUserId(user1.getId());

        // then
        assertThat(user1Conversations).hasSize(2);
    }

    @Test
    @DisplayName("삭제된 대화방은 목록에서 제외")
    void findByUserId_excludesDeleted() {
        // given
        DmConversation conv1 = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conv1);
        // INSERT는 항상 false로 저장하므로 별도로 삭제 처리
        dmConversationMapper.markUser1Deleted(conv1.getId());

        DmConversation conv2 = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user3.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conv2);

        // when
        List<DmConversation> user1Conversations = dmConversationMapper.findByUserId(user1.getId());

        // then - 삭제된 대화방은 제외
        assertThat(user1Conversations).hasSize(1);
        assertThat(user1Conversations.get(0).getUser2Id()).isEqualTo(user3.getId());
    }

    @Test
    @DisplayName("마지막 메시지 시간 업데이트")
    void updateLastMessageAt() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);
        LocalDateTime now = LocalDateTime.now();

        // when
        dmConversationMapper.updateLastMessageAt(conversation.getId(), now);

        // then
        DmConversation updated = dmConversationMapper.findById(conversation.getId());
        assertThat(updated.getLastMessageAt()).isNotNull();
    }

    @Test
    @DisplayName("마지막 읽은 메시지 ID 업데이트 (user1)")
    void updateUser1LastReadMessageId() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);

        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user2.getId())
                .content("테스트 메시지")
                .build();
        directMessageMapper.insert(message);

        // when
        dmConversationMapper.updateUser1LastReadMessageId(conversation.getId(), message.getId());

        // then
        DmConversation updated = dmConversationMapper.findById(conversation.getId());
        assertThat(updated.getUser1LastReadMessageId()).isEqualTo(message.getId());
    }

    @Test
    @DisplayName("대화방 삭제 처리 (user1)")
    void markUser1Deleted() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);

        // when
        dmConversationMapper.markUser1Deleted(conversation.getId());

        // then
        DmConversation updated = dmConversationMapper.findById(conversation.getId());
        assertThat(updated.getUser1Deleted()).isTrue();
        assertThat(updated.getUser2Deleted()).isFalse();
    }

    @Test
    @DisplayName("대화방 복원 (user1)")
    void restoreUser1() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);
        // 먼저 삭제 처리
        dmConversationMapper.markUser1Deleted(conversation.getId());

        // 삭제 상태 확인
        DmConversation deleted = dmConversationMapper.findById(conversation.getId());
        assertThat(deleted.getUser1Deleted()).isTrue();

        // when
        dmConversationMapper.restoreUser1(conversation.getId());

        // then
        DmConversation updated = dmConversationMapper.findById(conversation.getId());
        assertThat(updated.getUser1Deleted()).isFalse();
    }

    @Test
    @DisplayName("사용자 참여 여부 확인")
    void existsByIdAndUserId() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);

        // when & then
        assertThat(dmConversationMapper.existsByIdAndUserId(conversation.getId(), user1.getId())).isTrue();
        assertThat(dmConversationMapper.existsByIdAndUserId(conversation.getId(), user2.getId())).isTrue();
        assertThat(dmConversationMapper.existsByIdAndUserId(conversation.getId(), user3.getId())).isFalse();
    }

    @Test
    @DisplayName("마지막 메시지 내용 조회 (profile JOIN 포함)")
    void findById_withLastMessage() {
        // given
        DmConversation conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);

        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user1.getId())
                .content("마지막 메시지입니다")
                .build();
        directMessageMapper.insert(message);

        // when
        DmConversation found = dmConversationMapper.findById(conversation.getId());

        // then - 마지막 메시지 정보도 함께 조회됨
        assertThat(found.getLastMessageContent()).isEqualTo("마지막 메시지입니다");
        assertThat(found.getLastMessageSenderId()).isEqualTo(user1.getId());
    }
}
