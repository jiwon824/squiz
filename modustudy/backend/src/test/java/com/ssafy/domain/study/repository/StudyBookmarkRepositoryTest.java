package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * StudyBookmarkRepository 테스트
 */
 @SpringBootTest
 @Transactional
 class StudyBookmarkRepositoryTest {

    @Autowired
    private StudyBookmarkRepository bookmarkRepository;

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
    private Study study1;
    private Study study2;
    private Study study3;
    private StudyBookmark bookmark1;
    private StudyBookmark bookmark2;
    private StudyBookmark bookmark3;
    private Topic topic1;
    private Topic topic2;
    private Topic topic3;
    private Format format;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic1 = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topic2 = topicRepository.save(Topic.builder()
                .name("CS")
                .sortOrder(2)
                .build());
        topic3 = topicRepository.save(Topic.builder()
                .name("백엔드")
                .sortOrder(3)
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
        study1 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("알고리즘 스터디")
                .description("백준 문제 풀이")
                .topic(topic1)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        study2 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("CS 스터디")
                .description("운영체제 학습")
                .topic(topic2)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(5)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 4, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        study3 = studyRepository.save(Study.builder()
                .leaderId(user2.getId())
                .name("스프링 스터디")
                .description("스프링 부트 학습")
                .topic(topic3)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(8)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // 5. Bookmark 생성
        // User1이 Study1, Study2를 북마크
        bookmark1 = bookmarkRepository.save(StudyBookmark.create(user1.getId(), study1.getId()));
        bookmark2 = bookmarkRepository.save(StudyBookmark.create(user1.getId(), study2.getId()));

        // User2가 Study1을 북마크
        bookmark3 = bookmarkRepository.save(StudyBookmark.create(user2.getId(), study1.getId()));

        bookmarkRepository.flush();
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자 + 스터디로 북마크 조회 성공")
    void findByUserIdAndStudyId_Success() {
        // when
        Optional<StudyBookmark> result = bookmarkRepository.findByUserIdAndStudyId(
                user1.getId(), study1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(user1.getId());
        assertThat(result.get().getStudyId()).isEqualTo(study1.getId());
    }

    @Test
    @DisplayName("존재하지 않는 북마크 조회 시 빈 Optional 반환")
    void findByUserIdAndStudyId_NotFound() {
        // when
        Optional<StudyBookmark> result = bookmarkRepository.findByUserIdAndStudyId(
                user1.getId(), study3.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자의 북마크 목록 조회 (페이징)")
    void findByUserId_Paging() {
        // when
        Page<StudyBookmark> result = bookmarkRepository.findByUserId(
                user1.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자의 북마크 목록 조회 (리스트)")
    void findByUserId_List() {
        // when
        List<StudyBookmark> result = bookmarkRepository.findByUserId(user1.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("studyId")
                .containsExactlyInAnyOrder(study1.getId(), study2.getId());
    }

    @Test
    @DisplayName("특정 스터디를 북마크한 목록 조회")
    void findByStudyId() {
        // when
        List<StudyBookmark> result = bookmarkRepository.findByStudyId(study1.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("userId")
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());
    }

    // ============================================================
    // 존재 여부 확인 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 존재 여부 확인 - 존재함")
    void existsByUserIdAndStudyId_True() {
        // when
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(
                user1.getId(), study1.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("북마크 존재 여부 확인 - 존재하지 않음")
    void existsByUserIdAndStudyId_False() {
        // when
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(
                user1.getId(), study3.getId());

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // 통계 테스트
    // ============================================================

    @Test
    @DisplayName("사용자의 북마크 개수 조회")
    void countByUserId() {
        // when
        Long count = bookmarkRepository.countByUserId(user1.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 스터디의 북마크 개수 조회")
    void countByStudyId() {
        // when
        Long count = bookmarkRepository.countByStudyId(study1.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 스터디의 북마크 개수 조회 (N+1 방지)")
    void countByStudyIds() {
        // when
        List<Object[]> results = bookmarkRepository.countByStudyIds(
                List.of(study1.getId(), study2.getId(), study3.getId()));

        // then
        assertThat(results).hasSize(2);  // Study3은 북마크 없음

        for (Object[] row : results) {
            Long studyId = (Long) row[0];
            Long count = (Long) row[1];

            if (studyId.equals(study1.getId())) {
                assertThat(count).isEqualTo(2);
            } else if (studyId.equals(study2.getId())) {
                assertThat(count).isEqualTo(1);
            }
        }
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("사용자 + 스터디로 북마크 삭제")
    void deleteByUserIdAndStudyId() {
        // when
        bookmarkRepository.deleteByUserIdAndStudyId(user1.getId(), study1.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(
                user1.getId(), study1.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("특정 스터디의 모든 북마크 삭제")
    void deleteByStudyId() {
        // when
        bookmarkRepository.deleteByStudyId(study1.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        List<StudyBookmark> remaining = bookmarkRepository.findByStudyId(study1.getId());
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자의 모든 북마크 삭제")
    void deleteByUserId() {
        // when
        bookmarkRepository.deleteByUserId(user1.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        List<StudyBookmark> remaining = bookmarkRepository.findByUserId(user1.getId());
        assertThat(remaining).isEmpty();
    }

    // ============================================================
    // 엔티티 메서드 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 생성 - 정적 팩토리 메서드")
    void create() {
        // when
        StudyBookmark bookmark = StudyBookmark.create(user2.getId(), study2.getId());
        StudyBookmark saved = bookmarkRepository.save(bookmark);
        bookmarkRepository.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(user2.getId());
        assertThat(saved.getStudyId()).isEqualTo(study2.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 북마크 확인")
    void isOwner() {
        // when & then
        assertThat(bookmark1.isOwner(user1.getId())).isTrue();
        assertThat(bookmark1.isOwner(user2.getId())).isFalse();
    }
}
