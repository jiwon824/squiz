package com.ssafy.domain.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseDecisionRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceManualUpdateRequest;
import com.ssafy.domain.attendance.dto.response.AttendanceCalendarResponse;
import com.ssafy.domain.attendance.dto.response.AttendanceResponse;
import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import com.ssafy.domain.attendance.service.AttendanceService;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AttendanceControllerTest.TestConfig.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceService attendanceService;

    @Test
    @DisplayName("BLE 출석 시작")
    void startBleAttendance() throws Exception {
        when(attendanceService.startBleAttendance(1L, 10L, 1L))
                .thenReturn(new com.ssafy.common.response.MessageResponse("BLE 출석이 시작되었습니다."));

        mockMvc.perform(post("/api/v1/studies/1/sessions/10/attendance/ble/start")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("BLE 출석이 시작되었습니다."));
    }

    @Test
    @DisplayName("BLE 출석 체크")
    void checkBleAttendance() throws Exception {
        AttendanceResponse response = attendanceResponse(
                1L,
                10L,
                2L,
                AttendanceCheckType.BLE,
                AttendanceStatus.PRESENT,
                1L
        );
        when(attendanceService.checkAttendanceBle(1L, 10L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/studies/1/sessions/10/attendance/ble/check")
                        .with(authentication(authUser(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkType").value("BLE"))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    @DisplayName("셀프 출석 체크")
    void checkSelfAttendance() throws Exception {
        AttendanceResponse response = attendanceResponse(
                2L,
                10L,
                2L,
                AttendanceCheckType.SELF,
                AttendanceStatus.PRESENT,
                null
        );
        when(attendanceService.checkAttendanceSelf(1L, 10L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/studies/1/sessions/10/attendance/self")
                        .with(authentication(authUser(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkType").value("SELF"))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    @DisplayName("온라인 자동 출석 체크")
    void checkAutoAttendance() throws Exception {
        AttendanceResponse response = attendanceResponse(
                3L,
                10L,
                2L,
                AttendanceCheckType.AUTO,
                AttendanceStatus.PRESENT,
                null
        );
        when(attendanceService.checkAttendanceAutoOnline(1L, 10L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/studies/1/sessions/10/attendance/online/auto")
                        .with(authentication(authUser(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkType").value("AUTO"))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    @DisplayName("출석 상태 수동 변경")
    void updateAttendanceStatus() throws Exception {
        AttendanceManualUpdateRequest request = new AttendanceManualUpdateRequest(
                AttendanceStatus.PRESENT,
                "BLE 인식 오류"
        );
        AttendanceResponse response = attendanceResponse(
                4L,
                10L,
                2L,
                AttendanceCheckType.SELF,
                AttendanceStatus.PRESENT,
                1L
        );
        when(attendanceService.updateAttendanceStatus(eq(1L), eq(10L), eq(1L), eq(2L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/studies/1/sessions/10/attendance/2")
                        .with(authentication(authUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRESENT"))
                .andExpect(jsonPath("$.data.checkedBy").value(1L));
    }

    @Test
    @DisplayName("세션 출석 현황 조회")
    void getSessionAttendance() throws Exception {
        AttendanceResponse response = attendanceResponse(
                5L,
                10L,
                2L,
                AttendanceCheckType.BLE,
                AttendanceStatus.PRESENT,
                1L
        );
        when(attendanceService.getSessionAttendance(1L, 10L, 1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/studies/1/sessions/10/attendance")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PRESENT"));
    }

    @Test
    @DisplayName("월별 출석 캘린더 조회")
    void getMonthlyCalendar() throws Exception {
        AttendanceCalendarResponse response = new AttendanceCalendarResponse(
                2026,
                1,
                List.of(new AttendanceCalendarResponse.AttendanceCalendarItem(
                        LocalDate.of(2026, 1, 29),
                        10L,
                        AttendanceStatus.PRESENT,
                        AttendanceCheckType.AUTO,
                        LocalDateTime.of(2026, 1, 29, 10, 0)
                ))
        );
        when(attendanceService.getMonthlyCalendar(1L, 2L, 2026, 1)).thenReturn(response);

        mockMvc.perform(get("/api/v1/studies/1/attendance/calendar")
                        .with(authentication(authUser(2L)))
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.items[0].status").value("PRESENT"));
    }

    @Test
    @DisplayName("결석 소명 제출")
    void submitExcuse() throws Exception {
        AttendanceExcuseRequest request = new AttendanceExcuseRequest("지각 사유");
        AttendanceResponse response = attendanceResponse(
                6L,
                10L,
                2L,
                AttendanceCheckType.SELF,
                AttendanceStatus.ABSENT,
                null
        );
        when(attendanceService.submitExcuse(eq(1L), eq(10L), eq(2L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/studies/1/sessions/10/attendance/excuse")
                        .with(authentication(authUser(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABSENT"));
    }

    @Test
    @DisplayName("결석 소명 승인")
    void decideExcuse() throws Exception {
        AttendanceExcuseDecisionRequest request = new AttendanceExcuseDecisionRequest(
                AttendanceExcuseStatus.APPROVED
        );
        AttendanceResponse response = new AttendanceResponse(
                7L,
                10L,
                2L,
                AttendanceCheckType.SELF,
                AttendanceStatus.EXCUSED,
                LocalDateTime.now(),
                1L,
                null,
                AttendanceExcuseStatus.APPROVED
        );
        when(attendanceService.decideExcuse(eq(1L), eq(10L), eq(1L), eq(2L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/studies/1/sessions/10/attendance/2/excuse")
                        .with(authentication(authUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXCUSED"))
                .andExpect(jsonPath("$.data.excuseStatus").value("APPROVED"));
    }

    private AttendanceResponse attendanceResponse(
            Long id,
            Long sessionId,
            Long userId,
            AttendanceCheckType checkType,
            AttendanceStatus status,
            Long checkedBy
    ) {
        return new AttendanceResponse(
                id,
                sessionId,
                userId,
                checkType,
                status,
                LocalDateTime.now(),
                checkedBy,
                null,
                null
        );
    }

    private UsernamePasswordAuthenticationToken authUser(Long userId) {
        User user = User.builder()
                .userId("user-" + userId)
                .email("user-" + userId + "@test.local")
                .role(Role.USER)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return new UsernamePasswordAuthenticationToken(new SsafyUserDetails(user), null, List.of());
    }

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new TestAuthenticationPrincipalResolver());
        }
    }

    static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && SsafyUserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SsafyUserDetails details) {
                return details;
            }
            User user = User.builder()
                    .userId("user-1")
                    .email("user-1@test.local")
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);
            return new SsafyUserDetails(user);
        }
    }
}
