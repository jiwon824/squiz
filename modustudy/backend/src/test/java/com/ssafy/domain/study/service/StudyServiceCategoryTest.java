package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StudyServiceCategoryTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User leader;
    private Topic parentTopic;
    private Topic childTopic;
    private Format format;

    @BeforeEach
    void setUp() {
        // User 생성
        leader = userRepository.save(User.builder()
                .userId("leader1")
                .email("leader@test.com")
                .nickname("스터디장")
                .name("홍길동")
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

        // 대분류 Topic 생성
        parentTopic = topicRepository.save(Topic.builder()
                .name("알고리즘/코딩테스트")
                .icon("code")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 소분류 Topic 생성
        childTopic = topicRepository.save(Topic.builder()
                .name("백준")
                .parent(parentTopic)
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .description("알고리즘/자격증 문제 풀고 리뷰")
                .icon("edit")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        entityManager.clear();
    }

    @Nested
    @DisplayName("스터디 생성 - 카테고리")
    class CreateStudyWithCategory {

        @Test
        @DisplayName("성공 - Topic과 Format 모두 지정")
        void createStudy_WithTopicAndFormat_Success() {
            // given
            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("백준 골드 달성 스터디")
                    .description("매일 1문제씩 풀기")
                    .topicId(childTopic.getId())
                    .formatId(format.getId())
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .maxMembers(10)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();

            // when
            StudyResponse response = studyService.createStudy(request, leader.getId());

            // then
            assertThat(response.getId()).isNotNull();
            assertThat(response.getName()).isEqualTo("백준 골드 달성 스터디");

            // Topic 정보 확인
            assertThat(response.getTopic()).isNotNull();
            assertThat(response.getTopic().getId()).isEqualTo(childTopic.getId());
            assertThat(response.getTopic().getName()).isEqualTo("백준");
            assertThat(response.getTopic().getParent()).isNotNull();
            assertThat(response.getTopic().getParent().getName()).isEqualTo("알고리즘/코딩테스트");

            // Format 정보 확인
            assertThat(response.getFormat()).isNotNull();
            assertThat(response.getFormat().getId()).isEqualTo(format.getId());
            assertThat(response.getFormat().getName()).isEqualTo("문제 풀이");
        }

        @Test
        @DisplayName("성공 - Topic만 지정 (Format 없음)")
        void createStudy_WithTopicOnly_Success() {
            // given
            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("알고리즘 스터디")
                    .topicId(childTopic.getId())
                    .formatId(null)  // Format 없음
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();

            // when
            StudyResponse response = studyService.createStudy(request, leader.getId());

            // then
            assertThat(response.getTopic()).isNotNull();
            assertThat(response.getFormat()).isNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 Topic ID")
        void createStudy_InvalidTopicId_Fail() {
            // given
            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("테스트 스터디")
                    .topicId(999L)  // 존재하지 않는 ID
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();

            // when & then
            assertThatThrownBy(() -> studyService.createStudy(request, leader.getId()))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("존재하지 않는 주제");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 Format ID")
        void createStudy_InvalidFormatId_Fail() {
            // given
            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("테스트 스터디")
                    .topicId(childTopic.getId())
                    .formatId(999L)  // 존재하지 않는 ID
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();

            // when & then
            assertThatThrownBy(() -> studyService.createStudy(request, leader.getId()))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("존재하지 않는 형식");
        }

        @Test
        @DisplayName("성공 - 대분류로 스터디 생성")
        void createStudy_WithParentTopic_Success() {
            // given
            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("알고리즘 종합 스터디")
                    .topicId(parentTopic.getId())  // 대분류로 생성
                    .formatId(format.getId())
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();

            // when
            StudyResponse response = studyService.createStudy(request, leader.getId());

            // then
            assertThat(response.getTopic().getName()).isEqualTo("알고리즘/코딩테스트");
            assertThat(response.getTopic().getParent()).isNull();  // 대분류는 부모가 없음
        }
    }

    @Nested
    @DisplayName("스터디 수정 - 카테고리")
    class UpdateStudyCategory {

        private Study study;

        @BeforeEach
        void setUpStudy() {
            study = studyRepository.save(Study.builder()
                    .leaderId(leader.getId())
                    .name("기존 스터디")
                    .topic(childTopic)
                    .format(format)
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .status(Status.DRAFT)
                    .maxMembers(10)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .recruitStartDate(LocalDate.now())
                    .recruitEndDate(LocalDate.now().plusDays(5))
                    .extensionCount(0)
                    .build());
            studyRepository.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("성공 - Topic 변경")
        void updateStudy_ChangeTopic_Success() {
            // given
            Topic newTopic = topicRepository.save(Topic.builder()
                    .name("프로그래머스")
                    .parent(parentTopic)
                    .sortOrder(2)
                    .build());
            topicRepository.flush();

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .topicId(newTopic.getId())
                    .build();

            // when
            StudyResponse response = studyService.updateStudy(study.getId(), request, leader.getId());

            // then
            assertThat(response.getTopic().getId()).isEqualTo(newTopic.getId());
            assertThat(response.getTopic().getName()).isEqualTo("프로그래머스");
        }

        @Test
        @DisplayName("성공 - Format 변경")
        void updateStudy_ChangeFormat_Success() {
            // given
            Format newFormat = formatRepository.save(Format.builder()
                    .name("독서/책 스터디")
                    .description("기술 서적 읽고 토론")
                    .sortOrder(2)
                    .build());
            formatRepository.flush();

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .formatId(newFormat.getId())
                    .build();

            // when
            StudyResponse response = studyService.updateStudy(study.getId(), request, leader.getId());

            // then
            assertThat(response.getFormat().getId()).isEqualTo(newFormat.getId());
            assertThat(response.getFormat().getName()).isEqualTo("독서/책 스터디");
        }

        @Test
        @DisplayName("성공 - Topic과 Format 동시 변경")
        void updateStudy_ChangeTopicAndFormat_Success() {
            // given
            Topic newTopic = topicRepository.save(Topic.builder()
                    .name("CS 기초")
                    .sortOrder(2)
                    .build());
            topicRepository.flush();

            Format newFormat = formatRepository.save(Format.builder()
                    .name("강의 수강")
                    .sortOrder(3)
                    .build());
            formatRepository.flush();

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .topicId(newTopic.getId())
                    .formatId(newFormat.getId())
                    .build();

            // when
            StudyResponse response = studyService.updateStudy(study.getId(), request, leader.getId());

            // then
            assertThat(response.getTopic().getName()).isEqualTo("CS 기초");
            assertThat(response.getFormat().getName()).isEqualTo("강의 수강");
        }

        @Test
        @DisplayName("성공 - 카테고리 외 필드만 변경 (카테고리 유지)")
        void updateStudy_KeepCategory_Success() {
            // given
            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .name("스터디명만 변경")
                    .topicId(null)   // 변경 안 함
                    .formatId(null)  // 변경 안 함
                    .build();

            // when
            StudyResponse response = studyService.updateStudy(study.getId(), request, leader.getId());

            // then
            assertThat(response.getName()).isEqualTo("스터디명만 변경");
            assertThat(response.getTopic().getName()).isEqualTo("백준");  // 유지
            assertThat(response.getFormat().getName()).isEqualTo("문제 풀이");  // 유지
        }
    }

    @Nested
    @DisplayName("스터디 조회 - 카테고리 정보 포함")
    class GetStudyWithCategory {

        @Test
        @DisplayName("성공 - 스터디 상세 조회 시 카테고리 정보 포함")
        void getStudyById_WithCategoryInfo() {
            // given
            Study study = studyRepository.save(Study.builder()
                    .leaderId(leader.getId())
                    .name("테스트 스터디")
                    .topic(childTopic)
                    .format(format)
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .status(Status.DRAFT)
                    .maxMembers(10)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(3))
                    .recruitStartDate(LocalDate.now())
                    .recruitEndDate(LocalDate.now().plusDays(5))
                    .extensionCount(0)
                    .build());
            studyRepository.flush();
            entityManager.clear();

            // when
            StudyResponse found = studyService.getStudyById(study.getId());

            // then
            assertThat(found.getTopic()).isNotNull();
            assertThat(found.getTopic().getName()).isEqualTo("백준");

            assertThat(found.getFormat()).isNotNull();
            assertThat(found.getFormat().getName()).isEqualTo("문제 풀이");
        }
    }
}