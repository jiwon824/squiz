package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("MessageRepository 테스트")
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
    private EntityManager entityManager;

    private User user;
    private User user2;
    private Study study;
    private Workspace workspace;
    private Message message;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user = userRepository.save(User.builder()
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

        // 2. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("테스트 스터디")
                .topic("Java")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 3. Workspace 생성
        workspace = workspaceRepository.save(Workspace.create(study.getId()));
        workspaceRepository.flush();

        // 4. Message 생성
        message = messageRepository.save(Message.createTextMessage(
                workspace.getId(),
                user.getId(),
                "테스트 메시지입니다."
        ));
        messageRepository.flush();
    }

    @Nested
    @DisplayName("메시지 생성")
    class CreateMessage {

        @Test
        @DisplayName("텍스트 메시지 생성 성공")
        void createTextMessage_Success() {
            // given & when
            Message textMessage = messageRepository.save(Message.createTextMessage(
                    workspace.getId(),
                    user.getId(),
                    "안녕하세요!"
            ));
            messageRepository.flush();

            // then
            assertThat(textMessage.getId()).isNotNull();
            assertThat(textMessage.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(textMessage.getUserId()).isEqualTo(user.getId());
            assertThat(textMessage.getContent()).isEqualTo("안녕하세요!");
            assertThat(textMessage.getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(textMessage.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("이미지 메시지 생성 성공")
        void createImageMessage_Success() {
            // given & when
            Message imageMessage = messageRepository.save(Message.createImageMessage(
                    workspace.getId(),
                    user.getId(),
                    "이미지 설명",
                    "https://example.com/image.png"
            ));
            messageRepository.flush();

            // then
            assertThat(imageMessage.getMessageType()).isEqualTo(MessageType.IMAGE);
            assertThat(imageMessage.getFileUrl()).isEqualTo("https://example.com/image.png");
            assertThat(imageMessage.hasFile()).isTrue();
        }

        @Test
        @DisplayName("파일 메시지 생성 성공")
        void createFileMessage_Success() {
            // given & when
            Message fileMessage = messageRepository.save(Message.createFileMessage(
                    workspace.getId(),
                    user.getId(),
                    "문서 파일",
                    "https://example.com/document.pdf"
            ));
            messageRepository.flush();

            // then
            assertThat(fileMessage.getMessageType()).isEqualTo(MessageType.FILE);
            assertThat(fileMessage.getFileUrl()).isEqualTo("https://example.com/document.pdf");
            assertThat(fileMessage.hasFile()).isTrue();
        }

        @Test
        @DisplayName("시스템 메시지 생성 성공 - 저장 없이 객체 테스트")
        void createSystemMessage_Success() {
            // given & when - DB 저장 없이 객체만 생성 (userId=0L은 FK 위반)
            Message systemMessage = Message.createSystemMessage(
                    workspace.getId(),
                    "테스트유저1님이 입장하셨습니다."
            );

            // then
            assertThat(systemMessage.getMessageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(systemMessage.getUserId()).isEqualTo(0L);
            assertThat(systemMessage.isSystemMessage()).isTrue();
            assertThat(systemMessage.getWorkspaceId()).isEqualTo(workspace.getId());
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class FindMessages {

        @Test
        @DisplayName("워크스페이스 ID로 메시지 목록 조회 (삭제되지 않은 것만)")
        void findByWorkspaceIdAndNotDeleted_Success() {
            // given
            Message message2 = messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "두 번째 메시지"));
            Message deletedMessage = messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "삭제된 메시지"));
            deletedMessage.delete();
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByWorkspaceIdAndNotDeleted(
                    workspace.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).noneMatch(Message::isDeleted);
        }

        @Test
        @DisplayName("워크스페이스 ID로 메시지 목록 조회 (삭제 포함)")
        void findByWorkspaceId_IncludeDeleted() {
            // given
            Message deletedMessage = messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "삭제된 메시지"));
            deletedMessage.delete();
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByWorkspaceId(workspace.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("최근 메시지 N개 조회")
        void findRecentMessages_Success() {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 3);

            // when
            List<Message> result = messageRepository.findRecentMessages(workspace.getId(), pageable);

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("특정 시간 이후의 메시지 조회")
        void findMessagesAfter_Success() {
            // given
            LocalDateTime baseTime = LocalDateTime.now().minusMinutes(1);

            Message newMessage = messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "새 메시지"));
            messageRepository.flush();

            // when
            List<Message> result = messageRepository.findMessagesAfter(workspace.getId(), baseTime);

            // then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 메시지 목록 조회")
        void findByUserId_Success() {
            // given
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user2.getId(), "user2의 메시지"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByUserId(user.getId(), pageable);

            // then
            assertThat(result.getContent()).allMatch(m -> m.getUserId().equals(user.getId()));
        }

        @Test
        @DisplayName("메시지 타입별 조회")
        void findByWorkspaceIdAndMessageType_Success() {
            // given
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지", "https://example.com/img.png"));
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지2", "https://example.com/img2.png"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByWorkspaceIdAndMessageType(
                    workspace.getId(), MessageType.IMAGE, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(m -> m.getMessageType() == MessageType.IMAGE);
        }

        @Test
        @DisplayName("메시지 내용 검색")
        void searchByContent_Success() {
            // given
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "Java 공부하자"));
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "Spring 공부하자"));
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "Python 배우자"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.searchByContent(
                    workspace.getId(), "공부", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(m -> m.getContent().contains("공부"));
        }

        @Test
        @DisplayName("워크스페이스 내 메시지 수 조회")
        void countByWorkspaceId_Success() {
            // given
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "메시지 1"));
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "메시지 2"));
            messageRepository.flush();

            // when
            long count = messageRepository.countByWorkspaceId(workspace.getId());

            // then
            assertThat(count).isEqualTo(3); // setUp에서 1개 + 여기서 2개
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class UpdateMessage {

        @Test
        @DisplayName("메시지 내용 수정 성공")
        void updateContent_Success() {
            // given
            String newContent = "수정된 메시지입니다.";

            // when
            message.updateContent(newContent);
            messageRepository.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.getContent()).isEqualTo(newContent);
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("메시지 Soft Delete 성공")
        void softDelete_Success() {
            // when
            message.delete();
            messageRepository.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("워크스페이스 ID로 모든 메시지 삭제 성공")
        void deleteAllByWorkspaceId_Success() {
            // given
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "메시지 1"));
            messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "메시지 2"));
            messageRepository.flush();

            // when
            messageRepository.deleteAllByWorkspaceId(workspace.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            long count = messageRepository.countByWorkspaceId(workspace.getId());
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("사용자 ID로 모든 메시지 삭제 성공")
        void deleteAllByUserId_Success() {
            // given
            Long userId = user.getId();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            messageRepository.deleteAllByUserId(userId);
            entityManager.flush();
            entityManager.clear();

            // then
            Page<Message> result = messageRepository.findByUserId(userId, pageable);
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogic {

        @Test
        @DisplayName("작성자 확인")
        void isAuthor_Success() {
            // then
            assertThat(message.isAuthor(user.getId())).isTrue();
            assertThat(message.isAuthor(user2.getId())).isFalse();
        }

        @Test
        @DisplayName("시스템 메시지 여부 확인 - 저장 없이 객체 테스트")
        void isSystemMessage_Success() {
            // given - DB 저장 없이 객체만 생성 (userId=0L은 FK 위반이므로)
            Message systemMessage = Message.builder()
                    .workspaceId(1L)
                    .userId(0L)
                    .content("시스템 메시지")
                    .messageType(MessageType.SYSTEM)
                    .build();

            Message textMessage = Message.builder()
                    .workspaceId(1L)
                    .userId(1L)
                    .content("일반 메시지")
                    .messageType(MessageType.TEXT)
                    .build();

            // then
            assertThat(systemMessage.isSystemMessage()).isTrue();
            assertThat(textMessage.isSystemMessage()).isFalse();
        }

        @Test
        @DisplayName("파일 첨부 여부 확인 - 저장 없이 객체 테스트")
        void hasFile_Success() {
            // given - DB 저장 없이 객체만 생성
            Message imageMessage = Message.builder()
                    .workspaceId(1L)
                    .userId(1L)
                    .content("이미지")
                    .messageType(MessageType.IMAGE)
                    .fileUrl("https://example.com/img.png")
                    .build();

            Message fileMessage = Message.builder()
                    .workspaceId(1L)
                    .userId(1L)
                    .content("파일")
                    .messageType(MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .build();

            Message textMessage = Message.builder()
                    .workspaceId(1L)
                    .userId(1L)
                    .content("텍스트")
                    .messageType(MessageType.TEXT)
                    .build();

            // then
            assertThat(imageMessage.hasFile()).isTrue();
            assertThat(fileMessage.hasFile()).isTrue();
            assertThat(textMessage.hasFile()).isFalse();
        }
    }
}