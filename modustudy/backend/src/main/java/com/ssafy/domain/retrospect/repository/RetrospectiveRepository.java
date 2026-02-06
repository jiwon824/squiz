package com.ssafy.domain.retrospect.repository;

import com.ssafy.domain.retrospect.entity.Retrospective;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    // 스터디별 회고 목록 조회
    List<Retrospective> findByStudyId(Long studyId);

    // 스터디별 회고 목록 조회 (페이징)
    Page<Retrospective> findByStudyId(Long studyId, Pageable pageable);

    // 스터디 + 회고 ID로 조회 (권한 체크용)
    Optional<Retrospective> findByIdAndStudyId(Long id, Long studyId);

    // 스터디 + 세션 ID로 조회
    Optional<Retrospective> findByStudyIdAndSessionId(Long studyId, Long sessionId);

    // 스터디별 회고 개수 조회
    Long countByStudyId(Long studyId);

    // 스터디별 회고 전체 삭제
    void deleteByStudyId(Long studyId);
}
