package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.StudyBookmarkResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyBookmarkRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyBookmarkServiceTest {

    @InjectMocks
    private StudyBookmarkService bookmarkService;

    @Mock
    private StudyBookmarkRepository bookmarkRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    private Study testStudy;
    private StudyBookmark testBookmark;

    @BeforeEach
    void setUp() {
        testStudy = Study.builder()
                .id(1L)
                .leaderId(10L)
                .name("알고리즘 스터디")
                .description("백준 골드 문제 풀이")
                .topic("알고리즘")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .difficulty(Difficulty.INTERMEDIATE)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .build();

        testBookmark = StudyBookmark.builder()
                .id(1L)
                .userId(1L)
                .studyId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 북마크 토글 테스트
    // ============================================================

    @Nested
    @DisplayName("북마크 토글 테스트")
    class ToggleBookmarkTest {

        @Test
        @DisplayName("북마크 추가 성공 - 기존 북마크 없음")
        void toggleBookmark_Add_Success() {
            // given
            Long studyId = 1L;
            Long userId = 1L;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));
            given(userRepository.existsById(userId)).willReturn(true);
            given(bookmarkRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.empty());
            given(bookmarkRepository.save(any(StudyBookmark.class))).willReturn(testBookmark);

            // when
            StudyBookmarkResponse response = bookmarkService.toggleBookmark(studyId, userId);

            // then
            assertThat(response.getIsBookmarked()).isTrue();
            assertThat(response.getStudyId()).isEqualTo(studyId);
            verify(bookmarkRepository).save(any(StudyBookmark.class));
            verify(bookmarkRepository, never()).delete(any());
        }

        @Test
        @DisplayName("북마크 삭제 성공 - 기존 북마크 있음")
        void toggleBookmark_Remove_Success() {
            // given
            Long studyId = 1L;
            Long userId = 1L;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));
            given(userRepository.existsById(userId)).willReturn(true);
            given(bookmarkRepository.findByUserIdAndStudyId(userId, studyId)).willReturn(Optional.of(testBookmark));

            // when
            StudyBookmarkResponse response = bookmarkService.toggleBookmark(studyId, userId);

            // then
            assertThat(response.getIsBookmarked()).isFalse();
            assertThat(response.getStudyId()).isEqualTo(studyId);
            verify(bookmarkRepository).delete(testBookmark);
            verify(bookmarkRepository, never()).save(any());
        }

        @Test
        @DisplayName("북마크 토글 실패 - 존재하지 않는 스터디")
        void toggleBookmark_StudyNotFound() {
            // given
            Long studyId = 99L;
            Long userId = 1L;

            given(studyRepository.findById(studyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookmarkService.toggleBookmark(studyId, userId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(bookmarkRepository, never()).save(any());
            verify(bookmarkRepository, never()).delete(any());
        }

        @Test
        @DisplayName("북마크 토글 실패 - 존재하지 않는 사용자")
        void toggleBookmark_UserNotFound() {
            // given
            Long studyId = 1L;
            Long userId = 99L;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));
            given(userRepository.existsById(userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> bookmarkService.toggleBookmark(studyId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사용자");

            verify(bookmarkRepository, never()).save(any());
            verify(bookmarkRepository, never()).delete(any());
        }
    }

    // ============================================================
    // 내 북마크 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("내 북마크 목록 조회 테스트")
    class GetMyBookmarksTest {

        @Test
        @DisplayName("내 북마크 목록 조회 성공")
        void getMyBookmarks_Success() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            List<StudyBookmark> bookmarks = List.of(testBookmark);
            Page<StudyBookmark> bookmarkPage = new PageImpl<>(bookmarks, pageable, 1);

            given(bookmarkRepository.findByUserId(userId, pageable)).willReturn(bookmarkPage);
            given(studyRepository.findAllById(List.of(1L))).willReturn(List.of(testStudy));
            List<Object[]> countResult = new ArrayList<>();
            countResult.add(new Object[]{1L, 5L});
            given(bookmarkRepository.countByStudyIds(anyList())).willReturn(countResult);

            // when
            Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getStudyName()).isEqualTo("알고리즘 스터디");
            assertThat(response.getContent().get(0).getBookmarkCount()).isEqualTo(5L);
            assertThat(response.getContent().get(0).getIsBookmarked()).isTrue();
        }

        @Test
        @DisplayName("내 북마크 목록 조회 - 빈 목록")
        void getMyBookmarks_Empty() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Page<StudyBookmark> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(bookmarkRepository.findByUserId(userId, pageable)).willReturn(emptyPage);

            // when
            Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(userId, pageable);

            // then
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    // ============================================================
    // 북마크 여부 확인 테스트
    // ============================================================

    @Nested
    @DisplayName("북마크 여부 확인 테스트")
    class IsBookmarkedTest {

        @Test
        @DisplayName("북마크 여부 확인 - 북마크함")
        void isBookmarked_True() {
            // given
            Long studyId = 1L;
            Long userId = 1L;

            given(bookmarkRepository.existsByUserIdAndStudyId(userId, studyId)).willReturn(true);

            // when
            boolean result = bookmarkService.isBookmarked(studyId, userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("북마크 여부 확인 - 북마크 안함")
        void isBookmarked_False() {
            // given
            Long studyId = 1L;
            Long userId = 1L;

            given(bookmarkRepository.existsByUserIdAndStudyId(userId, studyId)).willReturn(false);

            // when
            boolean result = bookmarkService.isBookmarked(studyId, userId);

            // then
            assertThat(result).isFalse();
        }
    }

    // ============================================================
    // 통계 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("스터디 북마크 개수 조회")
        void getBookmarkCount() {
            // given
            Long studyId = 1L;

            given(bookmarkRepository.countByStudyId(studyId)).willReturn(10L);

            // when
            Long count = bookmarkService.getBookmarkCount(studyId);

            // then
            assertThat(count).isEqualTo(10L);
        }

        @Test
        @DisplayName("내 북마크 개수 조회")
        void getMyBookmarkCount() {
            // given
            Long userId = 1L;

            given(bookmarkRepository.countByUserId(userId)).willReturn(5L);

            // when
            Long count = bookmarkService.getMyBookmarkCount(userId);

            // then
            assertThat(count).isEqualTo(5L);
        }
    }
}