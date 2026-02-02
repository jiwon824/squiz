package com.ssafy.domain.study.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.workspace.dto.request.MessageCreateRequest;
import com.ssafy.domain.study.workspace.dto.request.MessageUpdateRequest;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MessageController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        void createTextMessage_Success() throws Exception {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(
                    workspace.getId(), "새 메시지입니다");

            // when & then
            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.workspaceId").value(workspace.getId()))
                    .andExpect(jsonPath("$.userId").value(user1.getId()))
                    .andExpect(jsonPath("$.nickname").value("테스트유저1"))
                    .andExpect(jsonPath("$.content").value("새 메시지입니다"))
                    .andExpect(jsonPath("$.messageType").value("TEXT"));
        }

        @Test
        @DisplayName("성공 - 이미지 메시지 생성")
        void createImageMessage_Success() throws Exception {
            // given
            MessageCreateRequest request = MessageCreateRequest.image(
                    workspace.getId(), "이미지입니다", "https://example.com/image.jpg");

            // when & then
            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageType").value("IMAGE"))
                    .andExpect(jsonPath("$.fileUrl").value("https://example.com/image.jpg"));
        }

        @Test
        @DisplayName("실패 - workspaceId 불일치")
        void createMessage_WorkspaceIdMismatch() throws Exception {
            // given
            MessageCreateRequest request = MessageCreateRequest.text(999L, "메시지");

            // when & then
            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("일치하지 않습니다")));
        }
    }

    @Nested
    @DisplayName("메시지 목록 조회")
    class GetMessages {

        @Test
        @DisplayName("성공 - 페이징 조회")
        void getMessages_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("성공 - 최근 메시지 조회")
        void getRecentMessages_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/recent", workspace.getId())
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("성공 - 특정 시간 이후 메시지 조회")
        void getMessagesAfter_Success() throws Exception {
            // given
            String beforeTime = LocalDateTime.now().minusHours(1)
                    .format(DateTimeFormatter.ISO_DATE_TIME);

            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/after", workspace.getId())
                            .param("after", beforeTime))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("메시지 검색")
    class SearchMessages {

        @Test
        @DisplayName("성공 - 키워드 검색")
        void searchMessages_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/search", workspace.getId())
                            .param("keyword", "안녕"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].content").value(containsString("안녕")));
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void searchMessages_NoResult() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/search", workspace.getId())
                            .param("keyword", "존재하지않는키워드"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("메시지 타입별 조회")
    class GetMessagesByType {

        @Test
        @DisplayName("성공 - TEXT 타입 조회")
        void getMessagesByType_Text() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/type/{messageType}",
                            workspace.getId(), "TEXT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].messageType").value("TEXT"));
        }

        @Test
        @DisplayName("성공 - IMAGE 타입 조회 (결과 없음)")
        void getMessagesByType_Image_Empty() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/type/{messageType}",
                            workspace.getId(), "IMAGE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("메시지 상세 조회")
    class GetMessage {

        @Test
        @DisplayName("성공 - 메시지 상세 조회")
        void getMessage_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(message1.getId()))
                    .andExpect(jsonPath("$.content").value("안녕하세요!"))
                    .andExpect(jsonPath("$.nickname").value("테스트유저1"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 메시지")
        void getMessage_NotFound() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), 99999L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class UpdateMessage {

        @Test
        @DisplayName("성공 - 메시지 수정")
        void updateMessage_Success() throws Exception {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정된 메시지")
                    .build();

            // when & then
            mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("수정된 메시지"));
        }

        @Test
        @DisplayName("실패 - 작성자가 아닌 사용자")
        void updateMessage_NotAuthor() throws Exception {
            // given
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            // when & then
            mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("본인이 작성한")));
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("성공 - 작성자가 삭제")
        void deleteMessage_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user1.getId()))
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();

            Message found = messageRepository.findById(message1.getId()).orElseThrow();
            assert found.isDeleted();
        }

        @Test
        @DisplayName("실패 - 작성자가 아닌 사용자")
        void deleteMessage_NotAuthor() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user2.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("본인이 작성한")));
        }

        @Test
        @DisplayName("성공 - 관리자 삭제")
        void deleteMessageByAdmin_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}/admin",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user1.getId()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("메시지 통계")
    class MessageStatistics {

        @Test
        @DisplayName("성공 - 메시지 개수 조회")
        void getMessageCount_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/count", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }
    }

    @Nested
    @DisplayName("고정 메시지")
    class PinnedMessage {

        @Test
        @DisplayName("성공 - 고정 메시지 목록 조회")
        void getPinnedMessages_Success() throws Exception {
            // given
            message1.pin();
            messageRepository.flush();

            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/pinned", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(message1.getId()))
                    .andExpect(jsonPath("$[0].isPinned").value(true));
        }

        @Test
        @DisplayName("성공 - 고정 메시지 없음")
        void getPinnedMessages_Empty() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/pinned", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("성공 - 메시지 고정 토글")
        void togglePinMessage_Success() throws Exception {
            // when & then - 고정
            mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/messages/{messageId}/pin",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(message1.getId()))
                    .andExpect(jsonPath("$.isPinned").value(true));

            // when & then - 해제
            mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/messages/{messageId}/pin",
                            workspace.getId(), message1.getId())
                            .header("User-Id", user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(message1.getId()))
                    .andExpect(jsonPath("$.isPinned").value(false));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 메시지 고정 시도")
        void togglePinMessage_NotFound() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}/messages/{messageId}/pin",
                            workspace.getId(), 99999L)
                            .header("User-Id", user1.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }

        @Test
        @DisplayName("성공 - 고정 메시지 수 조회")
        void getPinnedMessageCount_Success() throws Exception {
            // given
            message1.pin();
            messageRepository.flush();

            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/pinned/count", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }

        @Test
        @DisplayName("성공 - 고정 메시지 수 0")
        void getPinnedMessageCount_Zero() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/pinned/count", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }
    }
}