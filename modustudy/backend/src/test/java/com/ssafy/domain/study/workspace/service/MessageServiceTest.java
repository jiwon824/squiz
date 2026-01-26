package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("MessageService 테스트")
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
        void createMessage_Success() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(
                    workspace.getId(), "안녕하세요!");

            // when
            MessageResponse response = messageService.createMessage(request, user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(response.getUserId()).isEqualTo(user.getId());
            assertThat(response.getContent()).isEqualTo("안녕하세요!");
            assertThat(response.getMessageType()).isEqualTo(MessageType.TEXT);
            assertThat(response.getNickname()).isEqualTo("테스트유저1");
        }

        @Test
        @DisplayName("이미지 메시지 생성 성공")
        void createImageMessage_Success() {
            // given
            MessageCreateRequest request = MessageCreateRequest.image(
                    workspace.getId(), "이미지 설명", "https://example.com/img.png");

            // when
            MessageResponse response = messageService.createMessage(request, user.getId());

            // then
            assertThat(response.getMessageType()).isEqualTo(MessageType.IMAGE);
            assertThat(response.getFileUrl()).isEqualTo("https://example.com/img.png");
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스에 메시지 생성 시 예외 발생")
        void createMessage_WorkspaceNotFound() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(999999L, "메시지");

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(request, user.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 메시지 생성 시 예외 발생")
        void createMessage_UserNotFound() {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(workspace.getId(), "메시지");

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(request, 999999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class GetMessage {

        @Test
        @DisplayName("메시지 조회 성공")
        void getMessage_Success() {
            // when
            MessageResponse response = messageService.getMessage(message.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(message.getId());
            assertThat(response.getContent()).isEqualTo("테스트 메시지입니다.");
            assertThat(response.getNickname()).isEqualTo("테스트유저1");
        }

        @Test
        @DisplayName("삭제된 메시지 조회 시 내용 숨김")
        void getMessage_Deleted() {
            // given
            message.delete();
            messageRepository.flush();

            // when
            MessageResponse response = messageService.getMessage(message.getId());

            // then
            assertThat(response.getContent()).isEqualTo("삭제된 메시지입니다.");
            assertThat(response.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 메시지 조회 시 예외 발생")
        void getMessage_NotFound() {
            // when & then
            assertThatThrownBy(() -> messageService.getMessage(999999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 목록 조회")
    class GetMessages {

        @Test
        @DisplayName("워크스페이스 내 메시지 목록 조회 성공")
        void getMessages_Success() {
            // given
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지1"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user2.getId(), "메시지2"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            MessagePageResponse response = messageService.getMessages(workspace.getId(), pageable);

            // then
            assertThat(response.getContent()).hasSize(3); // setUp에서 1개 + 여기서 2개
            assertThat(response.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("삭제된 메시지는 목록에서 제외")
        void getMessages_ExcludeDeleted() {
            // given
            Message deletedMessage = messageRepository.save(Message.createTextMessage(
                    workspace.getId(), user.getId(), "삭제될 메시지"));
            deletedMessage.delete();
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            MessagePageResponse response = messageService.getMessages(workspace.getId(), pageable);

            // then
            assertThat(response.getContent()).hasSize(1); // 삭제된 메시지 제외
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스 조회 시 예외 발생")
        void getMessages_WorkspaceNotFound() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> messageService.getMessages(999999L, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("최근 메시지 조회")
    class GetRecentMessages {

        @Test
        @DisplayName("최근 메시지 N개 조회 성공")
        void getRecentMessages_Success() {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            // when
            List<MessageResponse> result = messageService.getRecentMessages(workspace.getId(), 3);

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("메시지 검색")
    class SearchMessages {

        @Test
        @DisplayName("키워드로 메시지 검색 성공")
        void searchMessages_Success() {
            // given
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Java 공부"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Spring 공부"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Python 학습"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            MessagePageResponse response = messageService.searchMessages(
                    workspace.getId(), "공부", pageable);

            // then
            assertThat(response.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("메시지 타입별 조회")
    class GetMessagesByType {

        @Test
        @DisplayName("이미지 타입 메시지만 조회")
        void getMessagesByType_Image() {
            // given
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지1", "https://example.com/1.png"));
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지2", "https://example.com/2.png"));
            messageRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            MessagePageResponse response = messageService.getMessagesByType(
                    workspace.getId(), MessageType.IMAGE, pageable);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getContent())
                    .allMatch(m -> m.getMessageType() == MessageType.IMAGE);
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class UpdateMessage {

        @Test
        @DisplayName("메시지 수정 성공")
        void updateMessage_Success() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정된 메시지")
                    .build();

            // when
            MessageResponse response = messageService.updateMessage(
                    message.getId(), request, user.getId());

            // then
            assertThat(response.getContent()).isEqualTo("수정된 메시지");
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 수정 시도 시 예외 발생")
        void updateMessage_NotAuthor() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    message.getId(), request, user2.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한 메시지만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("삭제된 메시지 수정 시도 시 예외 발생")
        void updateMessage_Deleted() {
            // given
            message.delete();
            messageRepository.flush();

            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    message.getId(), request, user.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제된 메시지는 수정할 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 메시지 수정 시도 시 예외 발생")
        void updateMessage_NotFound() {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> messageService.updateMessage(
                    999999L, request, user.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("메시지 삭제 성공")
        void deleteMessage_Success() {
            // when
            messageService.deleteMessage(message.getId(), user.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 삭제 시도 시 예외 발생")
        void deleteMessage_NotAuthor() {
            // when & then
            assertThatThrownBy(() -> messageService.deleteMessage(message.getId(), user2.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한 메시지만 삭제할 수 있습니다");
        }

        @Test
        @DisplayName("관리자 삭제 성공 (권한 체크 없음)")
        void deleteMessageByAdmin_Success() {
            // when
            messageService.deleteMessageByAdmin(message.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Message found = messageRepository.findById(message.getId()).orElseThrow();
            assertThat(found.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 수 조회")
    class GetMessageCount {

        @Test
        @DisplayName("워크스페이스 내 메시지 수 조회")
        void getMessageCount_Success() {
            // given
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지1"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지2"));
            messageRepository.flush();

            // when
            long count = messageService.getMessageCount(workspace.getId());

            // then
            assertThat(count).isEqualTo(3); // setUp에서 1개 + 여기서 2개
        }
    }
}