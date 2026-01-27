package com.ssafy.domain.retrospective.repository;

import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.retrospect.repository.RetrospectiveRepository;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RetrospectiveRepositoryTest {

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private Study study;
    private Long studyId;
    private Retrospective retro1;
    private Retrospective retro2;
    private Retrospective retro3;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        Topic topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        Format format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. Study 생성 (부모 엔티티)
        study = studyRepository.save(Study.builder()
                .leaderId(100L)
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        studyId = study.getId();

        // 4. Retrospective 생성
        retro1 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .sessionId(1L)
                .build());

        retro2 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .title("2회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .sessionId(2L)
                .build());

        retro3 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .title("자유 회고")
                .retrospectiveType(RetrospectiveType.FREE)
                .build());

        retrospectiveRepository.flush();
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 회고 목록 조회")
    void findByStudyId_Success() {
        // when
        List<Retrospective> retrospectives = retrospectiveRepository
                .findByStudyId(studyId);

        // then
        assertThat(retrospectives).hasSize(3);
        assertThat(retrospectives).extracting(Retrospective::getStudyId)
                .containsOnly(studyId);
    }

    @Test
    @DisplayName("스터디별 회고 목록 조회 - 페이징")
    void findByStudyId_WithPaging() {
        // when
        Page<Retrospective> page = retrospectiveRepository
                .findByStudyId(studyId, PageRequest.of(0, 2));

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 스터디 ID로 조회 시 빈 목록 반환")
    void findByStudyId_NotFound() {
        // when
        List<Retrospective> retrospectives = retrospectiveRepository
                .findByStudyId(999L);

        // then
        assertThat(retrospectives).isEmpty();
    }

    @Test
    @DisplayName("스터디 + 회고 ID로 조회")
    void findByIdAndStudyId_Success() {
        // when
        Optional<Retrospective> result = retrospectiveRepository
                .findByIdAndStudyId(retro1.getId(), studyId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("1회차 회고");
    }

    @Test
    @DisplayName("다른 스터디의 회고 ID로 조회 시 빈 Optional 반환")
    void findByIdAndStudyId_DifferentStudy() {
        // when
        Optional<Retrospective> result = retrospectiveRepository
                .findByIdAndStudyId(retro1.getId(), 999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디 + 세션 ID로 조회")
    void findByStudyIdAndSessionId_Success() {
        // when
        Optional<Retrospective> result = retrospectiveRepository
                .findByStudyIdAndSessionId(studyId, 1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("1회차 회고");
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 조회 시 빈 Optional 반환")
    void findByStudyIdAndSessionId_NotFound() {
        // when
        Optional<Retrospective> result = retrospectiveRepository
                .findByStudyIdAndSessionId(studyId, 999L);

        // then
        assertThat(result).isEmpty();
    }

    // ============================================================
    // 개수 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 회고 개수 조회")
    void countByStudyId_Success() {
        // when
        Long count = retrospectiveRepository.countByStudyId(studyId);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("회고가 없는 스터디의 개수 조회")
    void countByStudyId_Zero() {
        // when
        Long count = retrospectiveRepository.countByStudyId(999L);

        // then
        assertThat(count).isEqualTo(0);
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("회고 단건 삭제")
    void deleteById_Success() {
        // given
        Long retroId = retro1.getId();

        // when
        retrospectiveRepository.deleteById(retroId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<Retrospective> result = retrospectiveRepository.findById(retroId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디별 회고 전체 삭제")
    void deleteByStudyId_Success() {
        // when
        retrospectiveRepository.deleteByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<Retrospective> retrospectives = retrospectiveRepository
                .findByStudyId(studyId);
        assertThat(retrospectives).isEmpty();
    }

    @Test
    @DisplayName("다른 스터디 회고는 삭제되지 않음")
    void deleteByStudyId_OnlyTargetStudy() {
        // given - 다른 스터디 생성
        Topic topic2 = topicRepository.save(Topic.builder()
                .name("백엔드")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        Study otherStudy = studyRepository.save(Study.builder()
                .leaderId(200L)
                .name("다른 스터디")
                .topic(topic2)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        retrospectiveRepository.save(Retrospective.builder()
                .studyId(otherStudy.getId())
                .title("다른 스터디 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());
        retrospectiveRepository.flush();

        // when
        retrospectiveRepository.deleteByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<Retrospective> remainingRetrospectives = retrospectiveRepository
                .findByStudyId(otherStudy.getId());
        assertThat(remainingRetrospectives).hasSize(1);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("회고 생성")
    void save_Success() {
        // given
        Retrospective retrospective = Retrospective.builder()
                .studyId(studyId)
                .title("새로운 회고")
                .retrospectiveType(RetrospectiveType.FREE)
                .build();

        // when
        Retrospective saved = retrospectiveRepository.save(retrospective);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("새로운 회고");
        assertThat(saved.getRetrospectiveType()).isEqualTo(RetrospectiveType.FREE);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("회고 단건 조회")
    void findById_Success() {
        // when
        Optional<Retrospective> result = retrospectiveRepository.findById(retro1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("1회차 회고");
        assertThat(result.get().getRetrospectiveType()).isEqualTo(RetrospectiveType.KPT);
    }
}