package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    List<StudyMember> findByStudyId(Long studyId);

    List<StudyMember> findByUserId(Long userId);

    /**
     * 특정 상태의 멤버십 조회 (사용자별)
     */
    List<StudyMember> findByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByStudyIdAndUserId(Long studyId, Long userId);

    // ============================================================
    // ⭐ 추가된 메서드들 (멤버 목록 조회용)
    // ============================================================

    /**
     * 스터디별 특정 상태의 멤버 목록 조회
     */
    List<StudyMember> findByStudyIdAndStatus(Long studyId, MemberStatus status);

    /**
     * 스터디별 특정 상태의 멤버 수 조회
     */
    int countByStudyIdAndStatus(Long studyId, MemberStatus status);

    /**
     * 특정 사용자가 스터디의 특정 상태 멤버인지 확인
     */
    boolean existsByStudyIdAndUserIdAndStatus(Long studyId, Long userId, MemberStatus status);
}