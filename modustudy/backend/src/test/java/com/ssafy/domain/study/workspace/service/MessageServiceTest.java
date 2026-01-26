package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.request.MessageUpdateRequest;
import com.ssafy.domain.study.workspace.dto.response.MessagePageResponse;
import com.ssafy.domain.study.workspace.dto.response.MessageResponse;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.MessageType;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.repository.MessageRepository;
import com.ssafy.domain.study.workspace.repository.WorkspaceRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MessageService 통합 테스트
 */
@SpringBootTest
@Transactional
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

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
    }

    @Nested
    @DisplayName("메시지 생성")
    class CreateMessage {

        @Test
        @DisplayName("성공 - 텍스트 메시지 생성")
        void createTextMessage_Success() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(
                    workspace.getId(), "새 메시지입니다");

            // when
            MessageResponse response = messageService.createMessage(request, user1.getId());

            // then
            assertThat(response.getId()).isNotNull();
            assertThat(response.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(response.getUserId()).isEqualTo(user1.getId());
            assertThat(response.getNickname()).isEqualTo("테스트유저1");
            assertThat(response.getContent()).isEqualTo("새 메시지입니다");
            assertThat(response.getMessageType()).isEqualTo(MessageType.TEXT);
        }

        @Test
        @DisplayName("성공 - 이미지 메시지 생성")
        void createImageMessage_Success() {
            // given
            MessageCreateRequest request = MessageCreateRequest.image(
                    workspace.getId(), "이미지입니다", "https://example.com/image.jpg");

            // when
            MessageResponse response = messageService.createMessage(request, user1.getId());

            // then
            assertThat(response.getMessageType()).isEqualTo(MessageType.IMAGE);
            assertThat(response.getFileUrl()).isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("성공 - 파일 메시지 생성")
        void createFileMessage_Success() {
            // given
            MessageCreateRequest request = MessageCreateRequest.file(
                    workspace.getId(), "파일입니다", "https://example.com/file.pdf");

            // when
            MessageResponse response = messageService.createMessage(request, user1.getId());

            // then
            assertThat(response.getMessageType()).isEqualTo(MessageType.FILE);
            assertThat(response.getFileUrl()).isEqualTo("https://example.com/file.pdf");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스")
        void createMessage_WorkspaceNotFound() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(99999L, "메시지");

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(request, user1.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void createMessage_UserNotFound() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(
                    workspace.getId(), "메시지");

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(request, 99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class GetMessage {

        @Test
        @DisplayName("성공 - 메시지 상세 조회")
        void getMessage_Success() {
            // when
            MessageResponse response = messageService.getMessage(message1.getId());

            // then
            assertThat(response.getId()).isEqualTo(message1.getId());
            assertThat(response.getContent()).isEqualTo("안녕하세요!");
            assertThat(response.getNickname()).isEqualTo("테스트유저1");
        }

        @Test
        @DisplayName("성공 - 삭제된 메시지 조회 시 삭제 표시")
        void getMessage_Deleted() {
            // given
            message1.delete();
            messageRepository.flush();
            entityManager.clear();

            // when
            MessageResponse response = messageService.getMessage(message1.getId());

            // then
            assertThat(response.getIsDeleted()).isTrue();
            assertThat(response.getContent()).isEqualTo("삭제된 메시지입니다.");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 메시지")
        void getMessage_NotFound() {
            // when & then
            assertThatThrownBy(() -> messageService.getMessage(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 목록 조회")
    class GetMessages {

        @Test
        @DisplayName("성공 - 페이징 조회")
        void getMessages_Success() {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user1.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            // when
            MessagePageResponse response = messageService.getMessages(
                    workspace.getId(), PageRequest.of(0, 3));

            // then
            assertThat(response.getContent()).hasSize(3);
            assertThat(response.getTotalElements()).isEqualTo(6);
            assertThat(response.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스")
        void getMessages_WorkspaceNotFound() {
            // when & then
            assertThatThrownBy(() -> messageService.getMessages(
                    99999L, PageRequest.of(0, 10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("최근 메시지 조회")
    class GetRecentMessages {

        @Test
        @DisplayName("성공 - 최근 메시지 조회")
        void getRecentMessages_Success() {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user1.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            // when
            List<MessageResponse> response = messageService.getRecentMessages(
                    workspace.getId(), 3);

            // then
            assertThat(response).hasSize(3);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스")
        void getRecentMessages_WorkspaceNotFound() {
            // when & then
            assertThatThrownBy(() -> messageService.getRecentMessages(99999L, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("특정 시간 이후 메시지 조회")
    class GetMessagesAfter {

        @Test
        @DisplayName("성공 - 폴링용 새 메시지 조회")
        void getMessagesAfter_Success() {
            // given
            LocalDateTime beforeTime = LocalDateTime.now().minusHours(1);

            // when
            List<MessageResponse> response = messageService.getMessagesAfter(
                    workspace.getId(), beforeTime);

            // then
            assertThat(response).hasSize(1);
        }
    }

    @Nested
    @DisplayName("메시지 검색")
    class SearchMessages {

        @Test
        @DisplayName("성공 - 키워드 검색")
        void searchMessages_Success() {
            // when
            MessagePageResponse response = messageService.searchMessages(
                    workspace.getId(), "안녕", PageRequest.of(0, 10));

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getContent()).contains("안녕");
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void searchMessages_NoResult() {
            // when
            MessagePageResponse response = messageService.searchMessages(
                    workspace.getId(), "존재하지않는키워드", PageRequest.of(0, 10));

            // then
            assertThat(response.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("메시지 타입별 조회")
    class GetMessagesByType {

        @Test
        @DisplayName("성공 - 타입별 조회")
        void getMessagesByType_Success() {
            // given
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user1.getId(), "이미지",
                    "https://example.com/img.jpg"));
            messageRepository.flush();

            // when
            MessagePageResponse textResponse = messageService.getMessagesByType(
                    workspace.getId(), MessageType.TEXT, PageRequest.of(0, 10));
            MessagePageResponse imageResponse = messageService.getMessagesByType(
                    workspace.getId(), MessageType.IMAGE, PageRequest.of(0, 10));

            // then
            assertThat(textResponse.getContent()).hasSize(1);
            assertThat(imageResponse.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class UpdateMessage {

        @Test
        @DisplayName("성공 - 메시지 수정")
        void updateMessage_Success() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정된 메시지")
                    .build();

            // when
            MessageResponse response = messageService.updateMessage(
                    message1.getId(), request, user1.getId());

            // then
            assertThat(response.getContent()).isEqualTo("수정된 메시지");
        }

        @Test
        @DisplayName("실패 - 작성자가 아닌 사용자")
        void updateMessage_NotAuthor() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    message1.getId(), request, user2.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한 메시지만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("실패 - 삭제된 메시지")
        void updateMessage_Deleted() {
            // given
            message1.delete();
            messageRepository.flush();

            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    message1.getId(), request, user1.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제된 메시지는 수정할 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 메시지")
        void updateMessage_NotFound() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    99999L, request, user1.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("성공 - 작성자가 삭제")
        void deleteMessage_Success() {
            // when
            messageService.deleteMessage(message1.getId(), user1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message1.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("실패 - 작성자가 아닌 사용자")
        void deleteMessage_NotAuthor() {
            // when & then
            assertThatThrownBy(() -> messageService.deleteMessage(
                    message1.getId(), user2.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한 메시지만 삭제할 수 있습니다");
        }

        @Test
        @DisplayName("성공 - 관리자 삭제")
        void deleteMessageByAdmin_Success() {
            // when
            messageService.deleteMessageByAdmin(message1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message1.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 통계")
    class MessageStatistics {

        @Test
        @DisplayName("성공 - 메시지 개수 조회")
        void getMessageCount_Success() {
            // when
            long count = messageService.getMessageCount(workspace.getId());

            // then
            assertThat(count).isEqualTo(1);
        }
    }
}