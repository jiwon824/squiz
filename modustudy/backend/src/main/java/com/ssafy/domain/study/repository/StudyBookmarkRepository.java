package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyBookmark;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyBookmarkRepository extends JpaRepository<StudyBookmark, Long> {

    // ============================================================
    // 조회
    // ============================================================

    /**
     * 사용자 + 스터디로 북마크 조회
     */
    Optional<StudyBookmark> findByUserIdAndStudyId(Long userId, Long studyId);

    /**
     * 사용자의 북마크 목록 조회 (페이징)
     */
    Page<StudyBookmark> findByUserId(Long userId, Pageable pageable);

    /**
     * 사용자의 북마크 목록 조회 (리스트)
     */
    List<StudyBookmark> findByUserId(Long userId);

    /**
     * 특정 스터디를 북마크한 목록
     */
    List<StudyBookmark> findByStudyId(Long studyId);

    // ============================================================
    // 존재 여부 확인
    // ============================================================

    /**
     * 사용자가 해당 스터디를 북마크했는지 확인
     */
    boolean existsByUserIdAndStudyId(Long userId, Long studyId);

    // ============================================================
    // 통계
    // ============================================================

    /**
     * 사용자의 북마크 개수
     */
    Long countByUserId(Long userId);

    /**
     * 특정 스터디의 북마크 개수
     */
    Long countByStudyId(Long studyId);

    /**
     * 여러 스터디의 북마크 개수 조회 (N+1 방지)
     */
    @Query("SELECT sb.studyId, COUNT(sb) FROM StudyBookmark sb " +
            "WHERE sb.studyId IN :studyIds " +
            "GROUP BY sb.studyId")
    List<Object[]> countByStudyIds(@Param("studyIds") List<Long> studyIds);

    // ============================================================
    // 삭제
    // ============================================================

    /**
     * 사용자 + 스터디로 북마크 삭제
     */
    void deleteByUserIdAndStudyId(Long userId, Long studyId);

    /**
     * 특정 스터디의 모든 북마크 삭제 (스터디 삭제 시)
     */
    void deleteByStudyId(Long studyId);

    /**
     * 특정 사용자의 모든 북마크 삭제
     */
    void deleteByUserId(Long userId);
}
