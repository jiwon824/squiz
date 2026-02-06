package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyRecommendLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StudyRecommendLogRepository extends JpaRepository<StudyRecommendLog, Long> {

    /**
     * 최근 추천 세션 중 특정 스터디가 포함된 가장 최근 로그 찾기
     * (사용자가 추천에서 클릭/지원했는지 자동 감지용)
     */
    @Query("SELECT l FROM StudyRecommendLog l " +
            "JOIN StudyRecommendItem i ON i.logId = l.id " +
            "WHERE l.userId = :userId AND i.studyId = :studyId " +
            "AND l.createdAt >= :since " +
            "ORDER BY l.createdAt DESC " +
            "LIMIT 1")
    Optional<StudyRecommendLog> findRecentLogContainingStudy(
            @Param("userId") Long userId,
            @Param("studyId") Long studyId,
            @Param("since") LocalDateTime since
    );
}
