package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.StudyBookmarkResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyBookmarkRepository;
import com.ssafy.domain.study.repository.StudyRepository;
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

import static org.assertj.core.api.Assertions.*;

/**
 * StudyBookmarkService 통합 테스트
 */
@SpringBootTest
@Transactional
class StudyBookmarkServiceTest {

    @Autowired
    private StudyBookmarkService bookmarkService;

    @Autowired
    private StudyBookmarkRepository bookmarkRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Study study1;
    private Study study2;
    private Study study3;
    private StudyBookmark bookmark1;

    @BeforeEach
    void setUp() {
        // 1. User 생성
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

        // 2. Study 생성
        study1 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("알고리즘 스터디")
                .description("백준 문제 풀이")
                .topic("알고리즘")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .difficulty(Difficulty.INTERMEDIATE)
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
                .topic("CS")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(5)
                .difficulty(Difficulty.BEGINNER)
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
                .topic("백엔드")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(8)
                .difficulty(Difficulty.INTERMEDIATE)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // 3. Bookmark 생성 - User1이 Study1을 북마크
        bookmark1 = bookmarkRepository.save(StudyBookmark.create(user1.getId(), study1.getId()));
        bookmarkRepository.flush();
    }

    // ============================================================
    // 북마크 토글 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 추가 성공 - 기존 북마크 없음")
    void toggleBookmark_Add_Success() {
        // when - user1이 study2를 북마크 (기존에 없음)
        StudyBookmarkResponse response = bookmarkService.toggleBookmark(
                study2.getId(), user1.getId());

        // then
        assertThat(response.getIsBookmarked()).isTrue();
        assertThat(response.getStudyId()).isEqualTo(study2.getId());
        assertThat(response.getId()).isNotNull();

        // DB 확인
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(
                user1.getId(), study2.getId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("북마크 삭제 성공 - 기존 북마크 있음")
    void toggleBookmark_Remove_Success() {
        // when - user1이 study1을 토글 (이미 북마크됨 → 삭제)
        StudyBookmarkResponse response = bookmarkService.toggleBookmark(
                study1.getId(), user1.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(response.getIsBookmarked()).isFalse();
        assertThat(response.getStudyId()).isEqualTo(study1.getId());

        // DB 확인
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(
                user1.getId(), study1.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("북마크 토글 실패 - 존재하지 않는 스터디")
    void toggleBookmark_StudyNotFound() {
        // when & then
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(99999L, user1.getId()))
                .isInstanceOf(StudyException.StudyNotFoundException.class);
    }

    @Test
    @DisplayName("북마크 토글 실패 - 존재하지 않는 사용자")
    void toggleBookmark_UserNotFound() {
        // when & then
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(study1.getId(), 99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");
    }

    // ============================================================
    // 내 북마크 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("내 북마크 목록 조회 성공")
    void getMyBookmarks_Success() {
        // given - user1이 study2도 북마크 추가
        bookmarkRepository.save(StudyBookmark.create(user1.getId(), study2.getId()));
        bookmarkRepository.flush();

        // when
        Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(
                user1.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).allMatch(r -> r.getIsBookmarked());
        assertThat(response.getContent()).extracting("studyName")
                .containsExactlyInAnyOrder("알고리즘 스터디", "CS 스터디");
    }

    @Test
    @DisplayName("내 북마크 목록 조회 - 빈 목록")
    void getMyBookmarks_Empty() {
        // when - user2는 북마크 없음
        Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(
                user2.getId(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("내 북마크 목록 조회 - 스터디 정보 포함 확인")
    void getMyBookmarks_WithStudyInfo() {
        // when
        Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(
                user1.getId(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(response.getContent()).hasSize(1);

        StudyBookmarkResponse bookmark = response.getContent().get(0);
        assertThat(bookmark.getStudyId()).isEqualTo(study1.getId());
        assertThat(bookmark.getStudyName()).isEqualTo("알고리즘 스터디");
        assertThat(bookmark.getStudyTopic()).isEqualTo("알고리즘");
        assertThat(bookmark.getStudyStatus()).isEqualTo("RECRUITING");
        assertThat(bookmark.getMeetingType()).isEqualTo("ONLINE");
    }

    // ============================================================
    // 북마크 여부 확인 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 여부 확인 - 북마크함")
    void isBookmarked_True() {
        // when
        boolean result = bookmarkService.isBookmarked(study1.getId(), user1.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("북마크 여부 확인 - 북마크 안함")
    void isBookmarked_False() {
        // when
        boolean result = bookmarkService.isBookmarked(study2.getId(), user1.getId());

        // then
        assertThat(result).isFalse();
    }

    // ============================================================
    // 통계 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 북마크 개수 조회")
    void getBookmarkCount() {
        // given - user2도 study1 북마크
        bookmarkRepository.save(StudyBookmark.create(user2.getId(), study1.getId()));
        bookmarkRepository.flush();

        // when
        Long count = bookmarkService.getBookmarkCount(study1.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("스터디 북마크 개수 조회 - 0개")
    void getBookmarkCount_Zero() {
        // when
        Long count = bookmarkService.getBookmarkCount(study3.getId());

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("내 북마크 개수 조회")
    void getMyBookmarkCount() {
        // given - user1이 study2도 북마크
        bookmarkRepository.save(StudyBookmark.create(user1.getId(), study2.getId()));
        bookmarkRepository.flush();

        // when
        Long count = bookmarkService.getMyBookmarkCount(user1.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("내 북마크 개수 조회 - 0개")
    void getMyBookmarkCount_Zero() {
        // when
        Long count = bookmarkService.getMyBookmarkCount(user2.getId());

        // then
        assertThat(count).isZero();
    }

    // ============================================================
    // 토글 연속 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 토글 연속 - 추가 → 삭제 → 추가")
    void toggleBookmark_Consecutive() {
        Long studyId = study2.getId();
        Long userId = user1.getId();

        // 1. 추가 (기존에 없음)
        StudyBookmarkResponse response1 = bookmarkService.toggleBookmark(studyId, userId);
        assertThat(response1.getIsBookmarked()).isTrue();

        // 2. 삭제 (방금 추가됨)
        StudyBookmarkResponse response2 = bookmarkService.toggleBookmark(studyId, userId);
        assertThat(response2.getIsBookmarked()).isFalse();

        // 3. 다시 추가
        StudyBookmarkResponse response3 = bookmarkService.toggleBookmark(studyId, userId);
        assertThat(response3.getIsBookmarked()).isTrue();
    }
}