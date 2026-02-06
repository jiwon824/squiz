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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("DirectMessageMapper 통합 테스트")
class DirectMessageMapperTest {

    @Autowired
    private DirectMessageMapper directMessageMapper;

    @Autowired
    private DmConversationMapper dmConversationMapper;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private DmConversation conversation;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .userId("dmtest1")
                .email("dm1@test.com")
                .nickname("DM테스트1")
                .name("DM Test 1")
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
                .userId("dmtest2")
                .email("dm2@test.com")
                .nickname("DM테스트2")
                .name("DM Test 2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        // 대화방 생성
        conversation = DmConversation.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Deleted(false)
                .user2Deleted(false)
                .build();
        dmConversationMapper.insert(conversation);
    }

    @Test
    @DisplayName("메시지 저장 및 조회")
    void insert_and_findById() {
        // given
        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user1.getId())
                .content("안녕하세요!")
                .build();

        // when
        directMessageMapper.insert(message);

        // then
        assertThat(message.getId()).isNotNull();

        DirectMessage found = directMessageMapper.findById(message.getId());
        assertThat(found).isNotNull();
        assertThat(found.getContent()).isEqualTo("안녕하세요!");
        assertThat(found.getSenderId()).isEqualTo(user1.getId());
        // profile JOIN 확인
        assertThat(found.getSenderNickname()).isEqualTo("DM테스트1");
    }

    @Test
    @DisplayName("대화방별 메시지 목록 조회 (페이징)")
    void findByConversationId() {
        // given - 5개 메시지 생성
        for (int i = 1; i <= 5; i++) {
            DirectMessage message = DirectMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId(i % 2 == 0 ? user1.getId() : user2.getId())
                    .content("메시지 " + i)
                    .build();
            directMessageMapper.insert(message);
        }

        // when - 첫 페이지 (3개)
        List<DirectMessage> page1 = directMessageMapper.findByConversationId(
                conversation.getId(), 3, 0
        );

        // when - 두번째 페이지 (2개)
        List<DirectMessage> page2 = directMessageMapper.findByConversationId(
                conversation.getId(), 3, 3
        );

        // then
        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(2);
        // profile JOIN 확인
        assertThat(page1.get(0).getSenderNickname()).isNotNull();
    }

    @Test
    @DisplayName("대화방의 최신 메시지 조회")
    void findLatestByConversationId() {
        // given
        DirectMessage msg1 = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user1.getId())
                .content("첫번째 메시지")
                .build();
        directMessageMapper.insert(msg1);

        DirectMessage msg2 = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user2.getId())
                .content("두번째 메시지")
                .build();
        directMessageMapper.insert(msg2);

        // when
        DirectMessage latest = directMessageMapper.findLatestByConversationId(conversation.getId());

        // then
        assertThat(latest).isNotNull();
        assertThat(latest.getContent()).isEqualTo("두번째 메시지");
    }

    @Test
    @DisplayName("안 읽은 메시지 개수 조회")
    void countUnreadMessages() {
        // given - 3개 메시지 생성
        DirectMessage msg1 = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user2.getId())
                .content("메시지 1")
                .build();
        directMessageMapper.insert(msg1);

        DirectMessage msg2 = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user2.getId())
                .content("메시지 2")
                .build();
        directMessageMapper.insert(msg2);

        DirectMessage msg3 = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user2.getId())
                .content("메시지 3")
                .build();
        directMessageMapper.insert(msg3);

        // when - msg1까지 읽은 경우
        int unread = directMessageMapper.countUnreadMessages(
                conversation.getId(), msg1.getId()
        );

        // then - msg2, msg3이 안 읽음
        assertThat(unread).isEqualTo(2);
    }

    @Test
    @DisplayName("안 읽은 메시지 개수 조회 - 읽은 메시지 없는 경우")
    void countUnreadMessages_noLastRead() {
        // given - 3개 메시지 생성
        for (int i = 1; i <= 3; i++) {
            DirectMessage message = DirectMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId(user2.getId())
                    .content("메시지 " + i)
                    .build();
            directMessageMapper.insert(message);
        }

        // when - 읽은 메시지가 없는 경우 (null)
        int unread = directMessageMapper.countUnreadMessages(
                conversation.getId(), null
        );

        // then - 모든 메시지가 안 읽음
        assertThat(unread).isEqualTo(3);
    }

    @Test
    @DisplayName("메시지 삭제 (soft delete)")
    void markDeleted() {
        // given
        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(user1.getId())
                .content("삭제될 메시지")
                .build();
        directMessageMapper.insert(message);

        // when
        directMessageMapper.markDeleted(message.getId());

        // then
        DirectMessage deleted = directMessageMapper.findById(message.getId());
        assertThat(deleted.getIsDeleted()).isTrue();
        assertThat(deleted.getContent()).isEqualTo("삭제된 메시지입니다.");
    }

    @Test
    @DisplayName("대화방 메시지 개수 조회")
    void countByConversationId() {
        // given
        for (int i = 1; i <= 5; i++) {
            DirectMessage message = DirectMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId(user1.getId())
                    .content("메시지 " + i)
                    .build();
            directMessageMapper.insert(message);
        }

        // when
        int count = directMessageMapper.countByConversationId(conversation.getId());

        // then
        assertThat(count).isEqualTo(5);
    }
}
