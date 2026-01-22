package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyBookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

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

    private StudyBookmark bookmark1;
    private StudyBookmark bookmark2;
    private StudyBookmark bookmark3;

    @BeforeEach
    void setUp() {
        // User 1이 Study 1, 2를 북마크
        bookmark1 = StudyBookmark.create(1L, 1L);
        bookmark1 = bookmarkRepository.save(bookmark1);

        bookmark2 = StudyBookmark.create(1L, 2L);
        bookmark2 = bookmarkRepository.save(bookmark2);

        // User 2가 Study 1을 북마크
        bookmark3 = StudyBookmark.create(2L, 1L);
        bookmark3 = bookmarkRepository.save(bookmark3);
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자 + 스터디로 북마크 조회 성공")
    void findByUserIdAndStudyId_Success() {
        // when
        Optional<StudyBookmark> result = bookmarkRepository.findByUserIdAndStudyId(1L, 1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getStudyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 북마크 조회 시 빈 Optional 반환")
    void findByUserIdAndStudyId_NotFound() {
        // when
        Optional<StudyBookmark> result = bookmarkRepository.findByUserIdAndStudyId(1L, 99L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자의 북마크 목록 조회 (페이징)")
    void findByUserId_Paging() {
        // when
        Page<StudyBookmark> result = bookmarkRepository.findByUserId(
                1L,
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
        List<StudyBookmark> result = bookmarkRepository.findByUserId(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("studyId").containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("특정 스터디를 북마크한 목록 조회")
    void findByStudyId() {
        // when
        List<StudyBookmark> result = bookmarkRepository.findByStudyId(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("userId").containsExactlyInAnyOrder(1L, 2L);
    }

    // ============================================================
    // 존재 여부 확인 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 존재 여부 확인 - 존재함")
    void existsByUserIdAndStudyId_True() {
        // when
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(1L, 1L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("북마크 존재 여부 확인 - 존재하지 않음")
    void existsByUserIdAndStudyId_False() {
        // when
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(1L, 99L);

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
        Long count = bookmarkRepository.countByUserId(1L);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 스터디의 북마크 개수 조회")
    void countByStudyId() {
        // when
        Long count = bookmarkRepository.countByStudyId(1L);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 스터디의 북마크 개수 조회 (N+1 방지)")
    void countByStudyIds() {
        // when
        List<Object[]> results = bookmarkRepository.countByStudyIds(List.of(1L, 2L, 3L));

        // then
        assertThat(results).hasSize(2);  // Study 3은 북마크 없음

        // Study 1: 2개, Study 2: 1개
        for (Object[] row : results) {
            Long studyId = (Long) row[0];
            Long count = (Long) row[1];

            if (studyId.equals(1L)) {
                assertThat(count).isEqualTo(2);
            } else if (studyId.equals(2L)) {
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
        bookmarkRepository.deleteByUserIdAndStudyId(1L, 1L);

        // then
        boolean exists = bookmarkRepository.existsByUserIdAndStudyId(1L, 1L);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("특정 스터디의 모든 북마크 삭제")
    void deleteByStudyId() {
        // when
        bookmarkRepository.deleteByStudyId(1L);

        // then
        List<StudyBookmark> remaining = bookmarkRepository.findByStudyId(1L);
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자의 모든 북마크 삭제")
    void deleteByUserId() {
        // when
        bookmarkRepository.deleteByUserId(1L);

        // then
        List<StudyBookmark> remaining = bookmarkRepository.findByUserId(1L);
        assertThat(remaining).isEmpty();
    }

    // ============================================================
    // 엔티티 메서드 테스트
    // ============================================================

    @Test
    @DisplayName("북마크 생성 - 정적 팩토리 메서드")
    void create() {
        // when
        StudyBookmark bookmark = StudyBookmark.create(3L, 3L);
        StudyBookmark saved = bookmarkRepository.save(bookmark);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(3L);
        assertThat(saved.getStudyId()).isEqualTo(3L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 북마크 확인")
    void isOwner() {
        // when & then
        assertThat(bookmark1.isOwner(1L)).isTrue();
        assertThat(bookmark1.isOwner(2L)).isFalse();
    }
}