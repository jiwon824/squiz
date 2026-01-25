package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.PenaltyPolicy;
import com.ssafy.domain.study.entity.StudyTemplate;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * StudyTemplateRepository 테스트
 */
@SpringBootTest
@Transactional
class StudyTemplateRepositoryTest {

    @Autowired
    private StudyTemplateRepository studyTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        // 사용자 1 생성
        User user1 = User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
                .name("김테스트")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        user1 = userRepository.save(user1);
        userId1 = user1.getId();

        // 사용자 2 생성
        User user2 = User.builder()
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .name("이테스트")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        user2 = userRepository.save(user2);
        userId2 = user2.getId();

        // 사용자 1의 템플릿들
        StudyTemplate userTemplate1 = StudyTemplate.builder()
                .userId(userId1)
                .name("알고리즘 스터디 템플릿")
                .isSystem(false)
                .templateType("ALGORITHM")
                .topic("백준 골드 달성")
                .format("문제 풀이 + 코드 리뷰")
                .meetingType(MeetingType.ONLINE)
                .description("백준 골드 티어를 목표로 하는 알고리즘 스터디")
                .textbook("백준 온라인 저지")
                .goal("3개월 내 골드 티어 달성")
                .difficulty(Difficulty.INTERMEDIATE)
                .prerequisites("실버 티어 이상")
                .processDetail("매주 3문제 풀이 후 코드 리뷰")
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .build();
        studyTemplateRepository.save(userTemplate1);

        StudyTemplate userTemplate2 = StudyTemplate.builder()
                .userId(userId1)
                .name("CS 스터디 템플릿")
                .isSystem(false)
                .templateType("CS")
                .topic("운영체제 + 네트워크")
                .format("발표 + 질의응답")
                .meetingType(MeetingType.HYBRID)
                .description("CS 기초 다지기")
                .textbook("운영체제와 정보기술의 원리")
                .goal("CS 기본 개념 숙지")
                .difficulty(Difficulty.ELEMENTARY)
                .prerequisites("없음")
                .processDetail("매주 1챕터씩 발표")
                .penaltyPolicy(PenaltyPolicy.LENIENT)
                .build();
        studyTemplateRepository.save(userTemplate2);

        // 사용자 2의 템플릿
        StudyTemplate userTemplate3 = StudyTemplate.builder()
                .userId(userId2)
                .name("프로젝트 템플릿")
                .isSystem(false)
                .templateType("PROJECT")
                .topic("웹 개발 프로젝트")
                .format("실습 중심")
                .meetingType(MeetingType.OFFLINE)
                .description("실전 프로젝트 진행")
                .textbook("없음")
                .goal("포트폴리오 완성")
                .difficulty(Difficulty.ADVANCED)
                .prerequisites("Spring Boot 기초")
                .processDetail("2주 스프린트로 진행")
                .penaltyPolicy(PenaltyPolicy.STRICT)
                .build();
        studyTemplateRepository.save(userTemplate3);

        // 시스템 템플릿들 (userId = null)
        StudyTemplate systemTemplate1 = StudyTemplate.builder()
                .userId(null)
                .name("기본 알고리즘 템플릿")
                .isSystem(true)
                .templateType("ALGORITHM")
                .topic("알고리즘 기초")
                .format("문제 풀이")
                .meetingType(MeetingType.ONLINE)
                .description("알고리즘 기초를 다지는 스터디")
                .textbook("프로그래머스")
                .goal("알고리즘 기초 습득")
                .difficulty(Difficulty.BEGINNER)
                .prerequisites("없음")
                .processDetail("매주 5문제 풀이")
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .build();
        studyTemplateRepository.save(systemTemplate1);

        StudyTemplate systemTemplate2 = StudyTemplate.builder()
                .userId(null)
                .name("기본 CS 템플릿")
                .isSystem(true)
                .templateType("CS")
                .topic("자료구조 + 알고리즘")
                .format("이론 학습")
                .meetingType(MeetingType.ONLINE)
                .description("CS 기초 이론 학습")
                .textbook("자료구조와 함께 배우는 알고리즘 입문")
                .goal("CS 기초 완성")
                .difficulty(Difficulty.BEGINNER)
                .prerequisites("없음")
                .processDetail("매주 1개 주제 학습")
                .penaltyPolicy(PenaltyPolicy.LENIENT)
                .build();
        studyTemplateRepository.save(systemTemplate2);
    }

    // ============================================================
    // 사용자 템플릿 조회 테스트
    // ============================================================

    @Test
    @DisplayName("특정 사용자의 템플릿 목록 조회")
    void findByUserId_Success() {
        // when
        List<StudyTemplate> templates = studyTemplateRepository.findByUserId(userId1);

        // then
        assertThat(templates).hasSize(2);
        assertThat(templates).allMatch(t -> t.getUserId().equals(userId1));
        assertThat(templates).allMatch(t -> !t.isSystem());
    }

    @Test
    @DisplayName("템플릿이 없는 사용자 조회 - 빈 리스트 반환")
    void findByUserId_NoTemplates_ReturnsEmpty() {
        // given
        Long userIdWithNoTemplates = 999L;

        // when
        List<StudyTemplate> templates = studyTemplateRepository.findByUserId(userIdWithNoTemplates);

        // then
        assertThat(templates).isEmpty();
    }

    // ============================================================
    // 시스템 템플릿 조회 테스트
    // ============================================================

    @Test
    @DisplayName("시스템 템플릿 목록 조회")
    void findByIsSystemTrue_Success() {
        // when
        List<StudyTemplate> templates = studyTemplateRepository.findByIsSystemTrue();

        // then
        assertThat(templates).hasSize(2);
        assertThat(templates).allMatch(StudyTemplate::isSystem);
        assertThat(templates).allMatch(t -> t.getUserId() == null);
    }

    @Test
    @DisplayName("특정 타입의 시스템 템플릿 조회")
    void findByIsSystemTrueAndTemplateType_Success() {
        // when
        List<StudyTemplate> templates = studyTemplateRepository
                .findByIsSystemTrueAndTemplateType("ALGORITHM");

        // then
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0).getTemplateType()).isEqualTo("ALGORITHM");
        assertThat(templates.get(0).isSystem()).isTrue();
    }

    // ============================================================
    // 권한 체크용 조회 테스트
    // ============================================================

    @Test
    @DisplayName("특정 사용자의 특정 템플릿 조회 - 성공")
    void findByIdAndUserId_Success() {
        // given
        StudyTemplate saved = studyTemplateRepository.findByUserId(userId1).get(0);

        // when
        Optional<StudyTemplate> found = studyTemplateRepository
                .findByIdAndUserId(saved.getId(), userId1);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId()).isEqualTo(userId1);
    }

    @Test
    @DisplayName("다른 사용자의 템플릿 조회 - 실패")
    void findByIdAndUserId_DifferentUser_ReturnsEmpty() {
        // given
        StudyTemplate saved = studyTemplateRepository.findByUserId(userId1).get(0);

        // when
        Optional<StudyTemplate> found = studyTemplateRepository
                .findByIdAndUserId(saved.getId(), userId2);

        // then
        assertThat(found).isEmpty();
    }

    // ============================================================
    // 개수 및 중복 체크 테스트
    // ============================================================

    @Test
    @DisplayName("특정 사용자의 템플릿 개수 조회")
    void countByUserId_Success() {
        // when
        Long count = studyTemplateRepository.countByUserId(userId1);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("템플릿 이름 중복 체크 - 존재함")
    void existsByUserIdAndName_Exists() {
        // when
        boolean exists = studyTemplateRepository
                .existsByUserIdAndName(userId1, "알고리즘 스터디 템플릿");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("템플릿 이름 중복 체크 - 존재하지 않음")
    void existsByUserIdAndName_NotExists() {
        // when
        boolean exists = studyTemplateRepository
                .existsByUserIdAndName(userId1, "없는 템플릿");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 사용자는 같은 이름 사용 가능")
    void existsByUserIdAndName_DifferentUser_NotExists() {
        // when
        boolean exists = studyTemplateRepository
                .existsByUserIdAndName(userId2, "알고리즘 스터디 템플릿");

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("템플릿 생성 테스트")
    void save_Success() {
        // given
        StudyTemplate template = StudyTemplate.builder()
                .userId(userId1)
                .name("새 템플릿")
                .isSystem(false)
                .templateType("INTERVIEW")
                .topic("면접 준비")
                .format("모의 면접")
                .meetingType(MeetingType.ONLINE)
                .description("기술 면접 대비")
                .difficulty(Difficulty.INTERMEDIATE)
                .penaltyPolicy(PenaltyPolicy.NORMAL)
                .build();

        // when
        StudyTemplate saved = studyTemplateRepository.save(template);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("새 템플릿");
        assertThat(saved.getUserId()).isEqualTo(userId1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("템플릿 수정 테스트")
    void update_Success() {
        // given
        StudyTemplate template = studyTemplateRepository.findByUserId(userId1).get(0);
        String originalName = template.getName();

        // when
        template.setName("수정된 템플릿");
        template.setDescription("수정된 설명");
        StudyTemplate updated = studyTemplateRepository.save(template);

        // then
        assertThat(updated.getName()).isEqualTo("수정된 템플릿");
        assertThat(updated.getName()).isNotEqualTo(originalName);
        assertThat(updated.getDescription()).isEqualTo("수정된 설명");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("템플릿 삭제 테스트")
    void delete_Success() {
        // given
        StudyTemplate template = studyTemplateRepository.findByUserId(userId1).get(0);
        Long templateId = template.getId();

        // when
        studyTemplateRepository.delete(template);

        // then
        assertThat(studyTemplateRepository.findById(templateId)).isEmpty();
    }

    @Test
    @DisplayName("템플릿 ID로 조회")
    void findById_Success() {
        // given
        StudyTemplate template = studyTemplateRepository.findByUserId(userId1).get(0);

        // when
        Optional<StudyTemplate> found = studyTemplateRepository.findById(template.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(template.getId());
        assertThat(found.get().getName()).isEqualTo(template.getName());
    }
}