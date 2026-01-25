package com.ssafy.domain.study.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("MessageController 테스트")
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
    @DisplayName("메시지 생성 API")
    class CreateMessage {

        @Test
        @DisplayName("텍스트 메시지 생성 성공 - 201 Created")
        void createMessage_Success() throws Exception {
            MessageCreateRequest request = MessageCreateRequest.text(
                    workspace.getId(), "안녕하세요!");

            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.workspaceId").value(workspace.getId()))
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.content").value("안녕하세요!"))
                    .andExpect(jsonPath("$.messageType").value("TEXT"))
                    .andExpect(jsonPath("$.nickname").value("테스트유저1"));
        }

        @Test
        @DisplayName("이미지 메시지 생성 성공")
        void createImageMessage_Success() throws Exception {
            MessageCreateRequest request = MessageCreateRequest.image(
                    workspace.getId(), "이미지 설명", "https://example.com/img.png");

            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageType").value("IMAGE"))
                    .andExpect(jsonPath("$.fileUrl").value("https://example.com/img.png"));
        }

        @Test
        @DisplayName("내용 없이 메시지 생성 시 400 에러")
        void createMessage_NoContent() throws Exception {
            MessageCreateRequest request = MessageCreateRequest.builder()
                    .workspaceId(workspace.getId())
                    .content("")
                    .build();

            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("workspaceId 불일치 시 400 에러")
        void createMessage_WorkspaceIdMismatch() throws Exception {
            MessageCreateRequest request = MessageCreateRequest.text(999999L, "메시지");

            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("메시지 목록 조회 API")
    class GetMessages {

        @Test
        @DisplayName("메시지 목록 조회 성공 - 200 OK")
        void getMessages_Success() throws Exception {
            // given - 추가 메시지 생성
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지1"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user2.getId(), "메시지2"));
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.pageNumber").value(0));
        }

        @Test
        @DisplayName("페이징 파라미터 적용")
        void getMessages_WithPaging() throws Exception {
            // given - 10개 메시지 생성
            for (int i = 0; i < 10; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages", workspace.getId())
                            .param("page", "0")
                            .param("size", "5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11)) // setUp 1개 + 10개
                    .andExpect(jsonPath("$.pageSize").value(5));
        }
    }

    @Nested
    @DisplayName("최근 메시지 조회 API")
    class GetRecentMessages {

        @Test
        @DisplayName("최근 메시지 조회 성공")
        void getRecentMessages_Success() throws Exception {
            // given
            for (int i = 0; i < 5; i++) {
                messageRepository.save(Message.createTextMessage(
                        workspace.getId(), user.getId(), "메시지 " + i));
            }
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/recent", workspace.getId())
                            .param("limit", "3")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }
    }

    @Nested
    @DisplayName("메시지 검색 API")
    class SearchMessages {

        @Test
        @DisplayName("키워드로 메시지 검색 성공")
        void searchMessages_Success() throws Exception {
            // given
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Java 공부"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Spring 공부"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "Python 학습"));
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/search", workspace.getId())
                            .param("keyword", "공부")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("메시지 타입별 조회 API")
    class GetMessagesByType {

        @Test
        @DisplayName("이미지 타입 메시지만 조회")
        void getMessagesByType_Success() throws Exception {
            // given
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지1", "https://example.com/1.png"));
            messageRepository.save(Message.createImageMessage(
                    workspace.getId(), user.getId(), "이미지2", "https://example.com/2.png"));
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/type/{messageType}",
                            workspace.getId(), MessageType.IMAGE)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("메시지 상세 조회 API")
    class GetMessage {

        @Test
        @DisplayName("메시지 상세 조회 성공")
        void getMessage_Success() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(message.getId()))
                    .andExpect(jsonPath("$.content").value("테스트 메시지입니다."))
                    .andExpect(jsonPath("$.nickname").value("테스트유저1"));
        }

        @Test
        @DisplayName("존재하지 않는 메시지 조회 시 400 에러")
        void getMessage_NotFound() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), 999999L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("삭제된 메시지 조회 시 내용 숨김")
        void getMessage_Deleted() throws Exception {
            // given
            message.delete();
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("삭제된 메시지입니다."))
                    .andExpect(jsonPath("$.isDeleted").value(true));
        }
    }

    @Nested
    @DisplayName("메시지 수정 API")
    class UpdateMessage {

        @Test
        @DisplayName("메시지 수정 성공 - 200 OK")
        void updateMessage_Success() throws Exception {
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정된 메시지")
                    .build();

            mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("수정된 메시지"));
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 수정 시도 시 400 에러")
        void updateMessage_NotAuthor() throws Exception {
            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("삭제된 메시지 수정 시도 시 400 에러")
        void updateMessage_Deleted() throws Exception {
            // given
            message.delete();
            messageRepository.flush();

            MessageUpdateRequest request = MessageUpdateRequest.builder()
                    .content("수정 시도")
                    .build();

            mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("메시지 삭제 API")
    class DeleteMessage {

        @Test
        @DisplayName("메시지 삭제 성공 - 204 No Content")
        void deleteMessage_Success() throws Exception {
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 삭제 시도 시 400 에러")
        void deleteMessage_NotAuthor() throws Exception {
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}",
                            workspace.getId(), message.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("관리자 메시지 삭제 성공 - 204 No Content")
        void deleteMessageByAdmin_Success() throws Exception {
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/messages/{messageId}/admin",
                            workspace.getId(), message.getId())
                            .header("User-Id", user2.getId())  // 다른 사용자여도 가능
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("메시지 수 조회 API")
    class GetMessageCount {

        @Test
        @DisplayName("메시지 수 조회 성공")
        void getMessageCount_Success() throws Exception {
            // given
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지1"));
            messageRepository.save(Message.createTextMessage(workspace.getId(), user.getId(), "메시지2"));
            messageRepository.flush();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/messages/count", workspace.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("3")); // setUp 1개 + 2개
        }
    }
}