package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.request.CreateTemplateRequest;
import com.ssafy.domain.study.dto.response.StudyTemplateResponse;
import com.ssafy.domain.study.dto.request.UpdateTemplateRequest;
import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.PenaltyPolicy;
import com.ssafy.domain.study.entity.StudyTemplate;
import com.ssafy.domain.study.repository.StudyTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * StudyTemplateService 테스트
 */
 @ExtendWith(MockitoExtension.class)
 class StudyTemplateServiceTest {

    @Mock
    private StudyTemplateRepository studyTemplateRepository;

    @InjectMocks
    private StudyTemplateService studyTemplateService;

    private Long userId;
    private StudyTemplate template1;
    private StudyTemplate template2;
    private StudyTemplate systemTemplate;

    @BeforeEach
    void setUp() {
        userId = 1L;

        template1 = StudyTemplate.builder()
                .id(1L)
                .userId(userId)
                .name("알고리즘 템플릿")
                .isSystem(false)
                .templateType("ALGORITHM")
                .topic("백준 골드")
                .format("문제풀이")
                .meetingType(MeetingType.ONLINE)
                .description("알고리즘 스터디")
                .difficulty(Difficulty.INTERMEDIATE)
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        template2 = StudyTemplate.builder()
                .id(2L)
                .userId(userId)
                .name("CS 템플릿")
                .isSystem(false)
                .templateType("CS")
                .topic("운영체제")
                .format("발표")
                .meetingType(MeetingType.HYBRID)
                .description("CS 스터디")
                .difficulty(Difficulty.ELEMENTARY)
                .penaltyPolicy(PenaltyPolicy.LENIENT)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        systemTemplate = StudyTemplate.builder()
                .id(3L)
                .userId(null)
                .name("기본 알고리즘 템플릿")
                .isSystem(true)
                .templateType("ALGORITHM")
                .topic("알고리즘 기초")
                .format("문제풀이")
                .meetingType(MeetingType.ONLINE)
                .description("시스템 제공 템플릿")
                .difficulty(Difficulty.BEGINNER)
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();
    }

    // ============================================================
    // 템플릿 생성 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 생성 성공")
    void createTemplate_Success() {
        // given
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("새 템플릿")
                .templateType("PROJECT")
                .topic("웹 개발")
                .format("프로젝트")
                .meetingType(MeetingType.OFFLINE)
                .description("프로젝트 스터디")
                .difficulty(Difficulty.ADVANCED)
                .penaltyPolicy(PenaltyPolicy.STRICT)
                .build();

        StudyTemplate savedTemplate = StudyTemplate.builder()
                .id(10L)
                .userId(userId)
                .name(request.getName())
                .isSystem(false)
                .templateType(request.getTemplateType())
                .topic(request.getTopic())
                .format(request.getFormat())
                .meetingType(request.getMeetingType())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .penaltyPolicy(request.getPenaltyPolicy())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(studyTemplateRepository.existsByUserIdAndName(userId, request.getName()))
                .willReturn(false);
        given(studyTemplateRepository.save(any(StudyTemplate.class)))
                .willReturn(savedTemplate);

        // when
        StudyTemplateResponse response = studyTemplateService.createTemplate(request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("새 템플릿");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.isSystem()).isFalse();

        verify(studyTemplateRepository).existsByUserIdAndName(userId, request.getName());
        verify(studyTemplateRepository).save(any(StudyTemplate.class));
    }

    @Test
    @DisplayName("템플릿 생성 실패 - 이름 중복")
    void createTemplate_DuplicateName_ThrowsException() {
        // given
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("알고리즘 템플릿")
                .build();

        given(studyTemplateRepository.existsByUserIdAndName(userId, request.getName()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyTemplateService.createTemplate(request, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 같은 이름의 템플릿이 존재합니다");

        verify(studyTemplateRepository).existsByUserIdAndName(userId, request.getName());
        verify(studyTemplateRepository, never()).save(any());
    }

    // ============================================================
    // 내 템플릿 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("내 템플릿 목록 조회 성공 - 최신순 정렬")
    void getMyTemplates_Success() {
        // given
        given(studyTemplateRepository.findByUserId(userId))
                .willReturn(Arrays.asList(template1, template2));

        // when
        List<StudyTemplateResponse> responses = studyTemplateService.getMyTemplates(userId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("CS 템플릿"); // 최신순
        assertThat(responses.get(1).getName()).isEqualTo("알고리즘 템플릿");

        verify(studyTemplateRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("내 템플릿 목록 조회 - 템플릿 없음")
    void getMyTemplates_Empty() {
        // given
        given(studyTemplateRepository.findByUserId(userId))
                .willReturn(List.of());

        // when
        List<StudyTemplateResponse> responses = studyTemplateService.getMyTemplates(userId);

        // then
        assertThat(responses).isEmpty();
        verify(studyTemplateRepository).findByUserId(userId);
    }

    // ============================================================
    // 시스템 템플릿 조회 테스트
    // ============================================================

    @Test
    @DisplayName("시스템 템플릿 전체 조회")
    void getSystemTemplates_All_Success() {
        // given
        given(studyTemplateRepository.findByIsSystemTrue())
                .willReturn(List.of(systemTemplate));

        // when
        List<StudyTemplateResponse> responses = studyTemplateService.getSystemTemplates(null);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isSystem()).isTrue();
        assertThat(responses.get(0).getUserId()).isNull();

        verify(studyTemplateRepository).findByIsSystemTrue();
    }

    @Test
    @DisplayName("시스템 템플릿 타입별 조회")
    void getSystemTemplates_ByType_Success() {
        // given
        String templateType = "ALGORITHM";
        given(studyTemplateRepository.findByIsSystemTrueAndTemplateType(templateType))
                .willReturn(List.of(systemTemplate));

        // when
        List<StudyTemplateResponse> responses = studyTemplateService.getSystemTemplates(templateType);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTemplateType()).isEqualTo("ALGORITHM");

        verify(studyTemplateRepository).findByIsSystemTrueAndTemplateType(templateType);
    }

    // ============================================================
    // 템플릿 상세 조회 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 상세 조회 성공 - 본인 템플릿")
    void getTemplate_OwnTemplate_Success() {
        // given
        given(studyTemplateRepository.findById(1L))
                .willReturn(Optional.of(template1));

        // when
        StudyTemplateResponse response = studyTemplateService.getTemplate(1L, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("알고리즘 템플릿");

        verify(studyTemplateRepository).findById(1L);
    }

    @Test
    @DisplayName("템플릿 상세 조회 성공 - 시스템 템플릿")
    void getTemplate_SystemTemplate_Success() {
        // given
        given(studyTemplateRepository.findById(3L))
                .willReturn(Optional.of(systemTemplate));

        // when
        StudyTemplateResponse response = studyTemplateService.getTemplate(3L, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.isSystem()).isTrue();

        verify(studyTemplateRepository).findById(3L);
    }

    @Test
    @DisplayName("템플릿 상세 조회 실패 - 존재하지 않는 템플릿")
    void getTemplate_NotFound_ThrowsException() {
        // given
        given(studyTemplateRepository.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyTemplateService.getTemplate(999L, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 템플릿입니다");

        verify(studyTemplateRepository).findById(999L);
    }

    @Test
    @DisplayName("템플릿 상세 조회 실패 - 권한 없음")
    void getTemplate_Forbidden_ThrowsException() {
        // given
        Long otherUserId = 999L;
        given(studyTemplateRepository.findById(1L))
                .willReturn(Optional.of(template1));

        // when & then
        assertThatThrownBy(() -> studyTemplateService.getTemplate(1L, otherUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("템플릿 조회 권한이 없습니다");

        verify(studyTemplateRepository).findById(1L);
    }

    // ============================================================
    // 템플릿 수정 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 수정 성공")
    void updateTemplate_Success() {
        // given
        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .name("수정된 템플릿")
                .description("수정된 설명")
                .difficulty(Difficulty.ADVANCED)
                .build();

        given(studyTemplateRepository.findByIdAndUserId(1L, userId))
                .willReturn(Optional.of(template1));
        given(studyTemplateRepository.existsByUserIdAndName(userId, "수정된 템플릿"))
                .willReturn(false);
        given(studyTemplateRepository.save(any(StudyTemplate.class)))
                .willReturn(template1);

        // when
        StudyTemplateResponse response = studyTemplateService.updateTemplate(1L, request, userId);

        // then
        assertThat(response).isNotNull();
        verify(studyTemplateRepository).findByIdAndUserId(1L, userId);
        verify(studyTemplateRepository).existsByUserIdAndName(userId, "수정된 템플릿");
        verify(studyTemplateRepository).save(any(StudyTemplate.class));
    }

    @Test
    @DisplayName("템플릿 수정 실패 - 권한 없음")
    void updateTemplate_NotFound_ThrowsException() {
        // given
        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .name("수정된 템플릿")
                .build();

        given(studyTemplateRepository.findByIdAndUserId(1L, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyTemplateService.updateTemplate(1L, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("템플릿을 찾을 수 없거나 수정 권한이 없습니다");

        verify(studyTemplateRepository).findByIdAndUserId(1L, userId);
        verify(studyTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("템플릿 수정 실패 - 이름 중복")
    void updateTemplate_DuplicateName_ThrowsException() {
        // given
        UpdateTemplateRequest request = UpdateTemplateRequest.builder()
                .name("CS 템플릿") // 이미 존재하는 이름
                .build();

        given(studyTemplateRepository.findByIdAndUserId(1L, userId))
                .willReturn(Optional.of(template1));
        given(studyTemplateRepository.existsByUserIdAndName(userId, "CS 템플릿"))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> studyTemplateService.updateTemplate(1L, request, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 같은 이름의 템플릿이 존재합니다");

        verify(studyTemplateRepository).findByIdAndUserId(1L, userId);
        verify(studyTemplateRepository).existsByUserIdAndName(userId, "CS 템플릿");
        verify(studyTemplateRepository, never()).save(any());
    }

    // ============================================================
    // 템플릿 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 삭제 성공")
    void deleteTemplate_Success() {
        // given
        given(studyTemplateRepository.findByIdAndUserId(1L, userId))
                .willReturn(Optional.of(template1));
        willDoNothing().given(studyTemplateRepository).delete(template1);

        // when
        studyTemplateService.deleteTemplate(1L, userId);

        // then
        verify(studyTemplateRepository).findByIdAndUserId(1L, userId);
        verify(studyTemplateRepository).delete(template1);
    }

    @Test
    @DisplayName("템플릿 삭제 실패 - 권한 없음")
    void deleteTemplate_NotFound_ThrowsException() {
        // given
        given(studyTemplateRepository.findByIdAndUserId(1L, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> studyTemplateService.deleteTemplate(1L, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("템플릿을 찾을 수 없거나 삭제 권한이 없습니다");

        verify(studyTemplateRepository).findByIdAndUserId(1L, userId);
        verify(studyTemplateRepository, never()).delete(any());
    }
}
