package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageRepository 통합 테스트
 */
@SpringBootTest
@Transactional
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Study study;
    private Workspace workspace;
    private Message message1;
    private Message message2;
    private Message deletedMessage;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. User 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
                .name("테스트1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        user2 = userRepository.save(User.builder()
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .name("테스트2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // 4. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("알고리즘 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 5. Workspace 생성
        workspace = workspaceRepository.save(Workspace.create(study.getId()));
        workspaceRepository.flush();

        // 6. Message 생성
        message1 = messageRepository.save(Message.createTextMessage(
                workspace.getId(), user1.getId(), "안녕하세요!"));
        messageRepository.flush();

        message2 = messageRepository.save(Message.createTextMessage(
                workspace.getId(), user2.getId(), "반갑습니다!"));
        messageRepository.flush();

        // 7. 삭제된 메시지 생성
        deletedMessage = messageRepository.save(Message.builder()
                .workspaceId(workspace.getId())
                .userId(user1.getId())
                .content("삭제된 메시지")
                .messageType(MessageType.TEXT)
                .isDeleted(true)
                .build());
        messageRepository.flush();
    }

    @Nested
    @DisplayName("메시지 생성")
    class CreateMessage {

        @Test
        @DisplayName("성공 - 텍스트 메시지 생성")
        void createTextMessage_Success() {
            // given
            Message message = Message.createTextMessage(
                    workspace.getId(), user1.getId(), "새 메시지");

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(saved.getUserId()).isEqualTo(user1.getId());
            assertThat(saved.getContent()).isEqualTo("새 메시지");
            assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(saved.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("성공 - 이미지 메시지 생성")
        void createImageMessage_Success() {
            // given
            Message message = Message.createImageMessage(
                    workspace.getId(), user1.getId(), "이미지입니다",
                    "https://example.com/image.jpg");

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getMessageType()).isEqualTo(MessageType.IMAGE);
            assertThat(saved.getFileUrl()).isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("성공 - 파일 메시지 생성")
        void createFileMessage_Success() {
            // given
            Message message = Message.createFileMessage(
                    workspace.getId(), user1.getId(), "파일입니다",
                    "https://example.com/file.pdf");

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getMessageType()).isEqualTo(MessageType.FILE);
            assertThat(saved.getFileUrl()).isEqualTo("https://example.com/file.pdf");
        }

        @Test
        @DisplayName("성공 - 시스템 메시지 생성")
        void createSystemMessage_Success() {
            // given
            Message message = Message.createSystemMessage(
                    workspace.getId(), "테스트유저1님이 입장했습니다.");

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getMessageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(saved.getUserId()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class FindMessage {

        @Test
        @DisplayName("성공 - 워크스페이스별 메시지 조회 (삭제 제외)")
        void findByWorkspaceIdAndNotDeleted_Success() {
            // when
            Page<Message> result = messageRepository.findByWorkspaceIdAndNotDeleted(
                    workspace.getId(), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).noneMatch(Message::isDeleted);
        }

        @Test
        @DisplayName("성공 - 워크스페이스별 메시지 조회 (삭제 포함)")
        void findByWorkspaceId_Success() {
            // when
            Page<Message> result = messageRepository.findByWorkspaceId(
                    workspace.getId(), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("성공 - 최근 메시지 조회")
        void findRecentMessages_Success() {
            // when
            List<Message> result = messageRepository.findRecentMessages(
                    workspace.getId(), PageRequest.of(0, 5));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).noneMatch(Message::isDeleted);
        }

        @Test
        @DisplayName("성공 - 사용자별 메시지 조회")
        void findByUserId_Success() {
            // when
            Page<Message> result = messageRepository.findByUserId(
                    user1.getId(), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("성공 - 메시지 타입별 조회")
        void findByWorkspaceIdAndMessageType_Success() {
            // given
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user1.getId(), "이미지",
                    "https://example.com/img.jpg"));
            messageRepository.flush();

            // when
            Page<Message> textMessages = messageRepository.findByWorkspaceIdAndMessageType(
                    workspace.getId(), MessageType.TEXT, PageRequest.of(0, 10));
            Page<Message> imageMessages = messageRepository.findByWorkspaceIdAndMessageType(
                    workspace.getId(), MessageType.IMAGE, PageRequest.of(0, 10));

            // then
            assertThat(textMessages.getContent()).hasSize(2);
            assertThat(imageMessages.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("메시지 검색")
    class SearchMessage {

        @Test
        @DisplayName("성공 - 키워드 검색")
        void searchByContent_Success() {
            // when
            Page<Message> result = messageRepository.searchByContent(
                    workspace.getId(), "안녕", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).contains("안녕");
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void searchByContent_NoResult() {
            // when
            Page<Message> result = messageRepository.searchByContent(
                    workspace.getId(), "존재하지않는키워드", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("특정 시간 이후 메시지 조회")
    class FindMessagesAfter {

        @Test
        @DisplayName("성공 - 특정 시간 이후 메시지 조회")
        void findMessagesAfter_Success() {
            // given
            LocalDateTime beforeTime = LocalDateTime.now().minusHours(1);

            // when
            List<Message> result = messageRepository.findMessagesAfter(
                    workspace.getId(), beforeTime);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 미래 시간 조회 시 빈 결과")
        void findMessagesAfter_Empty() {
            // given
            LocalDateTime futureTime = LocalDateTime.now().plusHours(1);

            // when
            List<Message> result = messageRepository.findMessagesAfter(
                    workspace.getId(), futureTime);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("메시지 통계")
    class MessageStatistics {

        @Test
        @DisplayName("성공 - 메시지 개수 조회")
        void countByWorkspaceId_Success() {
            // when
            long count = messageRepository.countByWorkspaceId(workspace.getId());

            // then
            assertThat(count).isEqualTo(2); // 삭제된 메시지 제외
        }
    }

    @Nested
    @DisplayName("고정 메시지")
    class PinnedMessage {

        @Test
        @DisplayName("성공 - 고정 메시지 목록 조회")
        void findPinnedMessages_Success() {
            // given
            message1.pin();
            messageRepository.flush();

            // when
            List<Message> pinnedMessages = messageRepository.findPinnedMessages(workspace.getId());

            // then
            assertThat(pinnedMessages).hasSize(1);
            assertThat(pinnedMessages.get(0).getId()).isEqualTo(message1.getId());
            assertThat(pinnedMessages.get(0).getIsPinned()).isTrue();
        }

        @Test
        @DisplayName("성공 - 고정 메시지 없음")
        void findPinnedMessages_Empty() {
            // when
            List<Message> pinnedMessages = messageRepository.findPinnedMessages(workspace.getId());

            // then
            assertThat(pinnedMessages).isEmpty();
        }

        @Test
        @DisplayName("성공 - 삭제된 메시지는 고정 목록에서 제외")
        void findPinnedMessages_ExcludeDeleted() {
            // given
            message1.pin();
            message1.delete();
            messageRepository.flush();

            // when
            List<Message> pinnedMessages = messageRepository.findPinnedMessages(workspace.getId());

            // then
            assertThat(pinnedMessages).isEmpty();
        }

        @Test
        @DisplayName("성공 - 고정 메시지 수 조회")
        void countPinnedMessages_Success() {
            // given
            message1.pin();
            message2.pin();
            messageRepository.flush();

            // when
            long count = messageRepository.countPinnedMessages(workspace.getId());

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("성공 - 고정 메시지 수 0")
        void countPinnedMessages_Zero() {
            // when
            long count = messageRepository.countPinnedMessages(workspace.getId());

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("성공 - 워크스페이스별 전체 삭제")
        void deleteAllByWorkspaceId_Success() {
            // when
            messageRepository.deleteAllByWorkspaceId(workspace.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            long count = messageRepository.countByWorkspaceId(workspace.getId());
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("성공 - 사용자별 전체 삭제")
        void deleteAllByUserId_Success() {
            // when
            messageRepository.deleteAllByUserId(user1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Page<Message> result = messageRepository.findByUserId(
                    user1.getId(), PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("엔티티 비즈니스 로직")
    class EntityBusinessLogic {

        @Test
        @DisplayName("성공 - 메시지 내용 수정")
        void updateContent_Success() {
            // when
            message1.updateContent("수정된 내용");
            messageRepository.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message1.getId()).orElseThrow();
            assertThat(found.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("성공 - 메시지 삭제 (Soft Delete)")
        void delete_Success() {
            // when
            message1.delete();
            messageRepository.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message1.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("성공 - 작성자 확인")
        void isAuthor_Success() {
            // then
            assertThat(message1.isAuthor(user1.getId())).isTrue();
            assertThat(message1.isAuthor(user2.getId())).isFalse();
        }

        @Test
        @DisplayName("성공 - 파일 첨부 여부 확인")
        void hasFile_Success() {
            // given
            Message imageMessage = Message.createImageMessage(
                    workspace.getId(), user1.getId(), "이미지", "url");
            Message textMessage = Message.createTextMessage(
                    workspace.getId(), user1.getId(), "텍스트");

            // then
            assertThat(imageMessage.hasFile()).isTrue();
            assertThat(textMessage.hasFile()).isFalse();
        }

        @Test
        @DisplayName("성공 - 메시지 고정/해제 토글")
        void togglePin_Success() {
            // given
            assertThat(message1.getIsPinned()).isFalse();

            // when - 고정
            message1.togglePin();

            // then
            assertThat(message1.getIsPinned()).isTrue();

            // when - 해제
            message1.togglePin();

            // then
            assertThat(message1.getIsPinned()).isFalse();
        }

        @Test
        @DisplayName("성공 - 메시지 고정")
        void pin_Success() {
            // when
            message1.pin();

            // then
            assertThat(message1.getIsPinned()).isTrue();
        }

        @Test
        @DisplayName("성공 - 메시지 고정 해제")
        void unpin_Success() {
            // given
            message1.pin();
            assertThat(message1.getIsPinned()).isTrue();

            // when
            message1.unpin();

            // then
            assertThat(message1.getIsPinned()).isFalse();
        }
    }
}