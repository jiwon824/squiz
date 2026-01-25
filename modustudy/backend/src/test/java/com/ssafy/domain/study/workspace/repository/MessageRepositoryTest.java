package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.entity.WorkspaceType;
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
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
    private EntityManager entityManager;

    private User user;
    private User user2;
    private Study study;
    private Workspace workspace;
    private Workspace workspace2;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
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
        workspace = workspaceRepository.save(Workspace.builder()
                .studyId(study.getId())
                .name("일반")
                .type(WorkspaceType.TEXT)
                .build());
        workspaceRepository.flush();

        workspace2 = workspaceRepository.save(Workspace.builder()
                .studyId(study.getId())
                .name("공지")
                .type(WorkspaceType.TEXT)
                .build());
        workspaceRepository.flush();
    }

    // ============================================================
    // 워크스페이스별 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("워크스페이스별 조회 테스트")
    class FindByWorkspaceIdTest {

        @Test
        @DisplayName("워크스페이스의 메시지 목록 페이징 조회")
        void findByWorkspaceIdAndIsDeletedFalse_Success() {
            // given
            for (int i = 1; i <= 15; i++) {
                messageRepository.save(Message.builder()
                        .workspaceId(workspace.getId())
                        .userId(user.getId())
                        .content("메시지 " + i)
                        .messageType(MessageType.TEXT)
                        .build());
            }
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

            // when
            Page<Message> result = messageRepository.findByWorkspaceIdAndIsDeletedFalse(
                    workspace.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.getTotalElements()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("삭제된 메시지는 조회되지 않음")
        void findByWorkspaceIdAndIsDeletedFalse_ExcludeDeleted() {
            // given
            Message message1 = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지1")
                    .messageType(MessageType.TEXT)
                    .build());

            Message message2 = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지2")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // 메시지2 삭제
            message2.delete();
            entityManager.flush();
            entityManager.clear();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByWorkspaceIdAndIsDeletedFalse(
                    workspace.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("메시지1");
        }

        @Test
        @DisplayName("워크스페이스의 최근 메시지 50개 조회")
        void findTop50ByWorkspaceIdAndIsDeletedFalse_Success() {
            // given
            for (int i = 1; i <= 60; i++) {
                messageRepository.save(Message.builder()
                        .workspaceId(workspace.getId())
                        .userId(user.getId())
                        .content("메시지 " + i)
                        .messageType(MessageType.TEXT)
                        .build());
            }
            messageRepository.flush();

            // when
            List<Message> result = messageRepository.findTop50ByWorkspaceIdAndIsDeletedFalse(
                    workspace.getId());

            // then
            assertThat(result).hasSize(50);
        }

        @Test
        @DisplayName("특정 시간 이후 메시지 조회")
        void findMessagesSince_Success() throws InterruptedException {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이전 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            LocalDateTime since = LocalDateTime.now();
            Thread.sleep(10); // 시간 차이 확보

            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이후 메시지1")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이후 메시지2")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            List<Message> result = messageRepository.findMessagesSince(workspace.getId(), since);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("content")
                    .containsExactly("이후 메시지1", "이후 메시지2");
        }
    }

    // ============================================================
    // 개별 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("개별 조회 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("메시지 ID와 워크스페이스 ID로 조회")
        void findByIdAndWorkspaceIdAndIsDeletedFalse_Success() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("테스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            Optional<Message> result = messageRepository.findByIdAndWorkspaceIdAndIsDeletedFalse(
                    message.getId(), workspace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getContent()).isEqualTo("테스트 메시지");
        }

        @Test
        @DisplayName("다른 워크스페이스 ID로 조회하면 빈 Optional 반환")
        void findByIdAndWorkspaceIdAndIsDeletedFalse_WrongWorkspaceId() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("테스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            Optional<Message> result = messageRepository.findByIdAndWorkspaceIdAndIsDeletedFalse(
                    message.getId(), workspace2.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제된 메시지는 조회되지 않음")
        void findByIdAndWorkspaceIdAndIsDeletedFalse_Deleted() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("테스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            message.delete();
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Message> result = messageRepository.findByIdAndWorkspaceIdAndIsDeletedFalse(
                    message.getId(), workspace.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("메시지 ID로만 조회")
        void findByIdAndIsDeletedFalse_Success() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("테스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            Optional<Message> result = messageRepository.findByIdAndIsDeletedFalse(message.getId());

            // then
            assertThat(result).isPresent();
        }
    }

    // ============================================================
    // 사용자별 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("사용자별 조회 테스트")
    class FindByUserIdTest {

        @Test
        @DisplayName("사용자의 메시지 목록 페이징 조회")
        void findByUserIdAndIsDeletedFalse_Success() {
            // given
            for (int i = 1; i <= 5; i++) {
                messageRepository.save(Message.builder()
                        .workspaceId(workspace.getId())
                        .userId(user.getId())
                        .content("user1 메시지 " + i)
                        .messageType(MessageType.TEXT)
                        .build());
            }
            for (int i = 1; i <= 3; i++) {
                messageRepository.save(Message.builder()
                        .workspaceId(workspace.getId())
                        .userId(user2.getId())
                        .content("user2 메시지 " + i)
                        .messageType(MessageType.TEXT)
                        .build());
            }
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.findByUserIdAndIsDeletedFalse(
                    user.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(5);
            assertThat(result.getContent()).allMatch(m -> m.getUserId().equals(user.getId()));
        }

        @Test
        @DisplayName("워크스페이스와 사용자로 메시지 조회")
        void findByWorkspaceIdAndUserIdAndIsDeletedFalse_Success() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("user1 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user2.getId())
                    .content("user2 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace2.getId())
                    .userId(user.getId())
                    .content("다른 워크스페이스 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            List<Message> result = messageRepository.findByWorkspaceIdAndUserIdAndIsDeletedFalse(
                    workspace.getId(), user.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("user1 메시지");
        }
    }

    // ============================================================
    // 검색 테스트
    // ============================================================

    @Nested
    @DisplayName("검색 테스트")
    class SearchTest {

        @Test
        @DisplayName("키워드로 메시지 검색")
        void searchByKeyword_Success() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("안녕하세요 반갑습니다")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("오늘 회의 시작합니다")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("안녕히 가세요")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.searchByKeyword(
                    workspace.getId(), "안녕", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("삭제된 메시지는 검색되지 않음")
        void searchByKeyword_ExcludeDeleted() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("안녕하세요")
                    .messageType(MessageType.TEXT)
                    .build());

            Message deletedMessage = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("안녕히가세요")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            deletedMessage.delete();
            entityManager.flush();
            entityManager.clear();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Message> result = messageRepository.searchByKeyword(
                    workspace.getId(), "안녕", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ============================================================
    // 통계 테스트
    // ============================================================

    @Nested
    @DisplayName("통계 테스트")
    class CountTest {

        @Test
        @DisplayName("워크스페이스의 메시지 개수 조회")
        void countByWorkspaceIdAndIsDeletedFalse_Success() {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.builder()
                        .workspaceId(workspace.getId())
                        .userId(user.getId())
                        .content("메시지 " + i)
                        .messageType(MessageType.TEXT)
                        .build());
            }
            messageRepository.flush();

            // when
            long count = messageRepository.countByWorkspaceIdAndIsDeletedFalse(workspace.getId());

            // then
            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("워크스페이스의 특정 타입 메시지 개수 조회")
        void countByWorkspaceIdAndMessageTypeAndIsDeletedFalse_Success() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("텍스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이미지")
                    .messageType(MessageType.IMAGE)
                    .fileUrl("http://example.com/image.png")
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(0L)
                    .content("님이 입장하셨습니다.")
                    .messageType(MessageType.SYSTEM)
                    .build());
            messageRepository.flush();

            // when
            long textCount = messageRepository.countByWorkspaceIdAndMessageTypeAndIsDeletedFalse(
                    workspace.getId(), MessageType.TEXT);
            long imageCount = messageRepository.countByWorkspaceIdAndMessageTypeAndIsDeletedFalse(
                    workspace.getId(), MessageType.IMAGE);
            long systemCount = messageRepository.countByWorkspaceIdAndMessageTypeAndIsDeletedFalse(
                    workspace.getId(), MessageType.SYSTEM);

            // then
            assertThat(textCount).isEqualTo(1);
            assertThat(imageCount).isEqualTo(1);
            assertThat(systemCount).isEqualTo(1);
        }
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("워크스페이스의 모든 메시지 Soft Delete")
        void softDeleteByWorkspaceId_Success() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지1")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지2")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace2.getId())
                    .userId(user.getId())
                    .content("다른 워크스페이스 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            messageRepository.softDeleteByWorkspaceId(workspace.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            long workspace1Count = messageRepository.countByWorkspaceIdAndIsDeletedFalse(workspace.getId());
            long workspace2Count = messageRepository.countByWorkspaceIdAndIsDeletedFalse(workspace2.getId());

            assertThat(workspace1Count).isEqualTo(0);
            assertThat(workspace2Count).isEqualTo(1);
        }

        @Test
        @DisplayName("워크스페이스의 모든 메시지 Hard Delete")
        void deleteByWorkspaceId_Success() {
            // given
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지1")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("메시지2")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            messageRepository.deleteByWorkspaceId(workspace.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            List<Message> result = messageRepository.findTop50ByWorkspaceIdAndIsDeletedFalse(workspace.getId());
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // CRUD 테스트
    // ============================================================

    @Nested
    @DisplayName("CRUD 테스트")
    class CrudTest {

        @Test
        @DisplayName("메시지 저장 - 기본값 확인")
        void save_Success() {
            // given
            Message message = Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("테스트 메시지")
                    .build();

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT); // 기본값
            assertThat(saved.getIsDeleted()).isFalse(); // 기본값
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미지 메시지 저장")
        void save_ImageMessage() {
            // given
            Message message = Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이미지입니다")
                    .messageType(MessageType.IMAGE)
                    .fileUrl("http://example.com/image.png")
                    .build();

            // when
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            // then
            assertThat(saved.getMessageType()).isEqualTo(MessageType.IMAGE);
            assertThat(saved.getFileUrl()).isEqualTo("http://example.com/image.png");
        }

        @Test
        @DisplayName("시스템 메시지 생성")
        void createSystemMessage_Success() {
            // given
            Message systemMessage = Message.createSystemMessage(
                    workspace.getId(), "테스트유저님이 입장하셨습니다.");

            // when
            Message saved = messageRepository.save(systemMessage);
            messageRepository.flush();

            // then
            assertThat(saved.getMessageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(saved.getUserId()).isEqualTo(0L);
            assertThat(saved.getContent()).isEqualTo("테스트유저님이 입장하셨습니다.");
        }

        @Test
        @DisplayName("메시지 수정")
        void update_Success() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("원본 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            message.updateContent("수정된 메시지");
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.getContent()).isEqualTo("수정된 메시지");
        }

        @Test
        @DisplayName("메시지 Soft Delete")
        void softDelete_Success() {
            // given
            Message message = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("삭제될 메시지")
                    .messageType(MessageType.TEXT)
                    .build());
            messageRepository.flush();

            // when
            message.delete();
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.getIsDeleted()).isTrue();
            assertThat(found.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("메시지 비즈니스 로직 테스트")
        void businessLogic_Success() {
            // given
            Message textMessage = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("텍스트 메시지")
                    .messageType(MessageType.TEXT)
                    .build());

            Message imageMessage = messageRepository.save(Message.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .content("이미지")
                    .messageType(MessageType.IMAGE)
                    .fileUrl("http://example.com/image.png")
                    .build());

            Message systemMessage = Message.createSystemMessage(workspace.getId(), "시스템 메시지");
            messageRepository.save(systemMessage);
            messageRepository.flush();

            // then
            assertThat(textMessage.isAuthor(user.getId())).isTrue();
            assertThat(textMessage.isAuthor(user2.getId())).isFalse();
            assertThat(textMessage.isSystemMessage()).isFalse();
            assertThat(imageMessage.hasFile()).isTrue();
            assertThat(systemMessage.isSystemMessage()).isTrue();
        }
    }
}