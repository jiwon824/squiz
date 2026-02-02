package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Study 기본 Repository
 */
@Repository
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryCustom {

    /**
     * 공개된 스터디 조회 (DRAFT 제외, is_public = true)
     */
    @Query("SELECT s FROM Study s " +
            "WHERE s.status != :excludeStatus " +
            "AND s.isPublic = true " +
            "ORDER BY s.createdAt DESC")
    Page<Study> findAllPublicStudies(@Param("excludeStatus") Status excludeStatus, Pageable pageable);

    /**
     * 모집중인 스터디만 조회
     */
    @Query("SELECT s FROM Study s " +
            "WHERE s.status = 'RECRUITING' " +
            "AND s.isPublic = true " +
            "ORDER BY s.createdAt DESC")
    Page<Study> findRecruitingStudies(Pageable pageable);

    /**
     * 특정 스터디장의 스터디 목록 조회
     */
    Page<Study> findByLeaderId(Long leaderId, Pageable pageable);

    /**
     * 특정 상태의 스터디 개수 조회
     */
    Long countByStatus(Status status);

    /**
     * 특정 스터디장의 특정 상태 스터디 목록
     */
    Page<Study> findByLeaderIdAndStatus(Long leaderId, Status status, Pageable pageable);

    // ========== 스케줄러용 쿼리 메서드 ==========

    /**
     * 특정 상태의 스터디 목록 조회
     */
    List<Study> findByStatus(Status status);

    /**
     * 모집 시작일이 도래한 SCHEDULED 스터디
     */
    List<Study> findByStatusAndRecruitStartDateLessThanEqual(Status status, LocalDate date);

    /**
     * 모집 종료일이 경과한 RECRUITING 스터디
     */
    List<Study> findByStatusAndRecruitEndDateLessThan(Status status, LocalDate date);

    /**
     * 시작일이 도래한 RECRUIT_CLOSED 스터디
     */
    List<Study> findByStatusAndStartDateLessThanEqual(Status status, LocalDate date);

    /**
     * 종료일이 경과한 IN_PROGRESS 스터디
     */
    List<Study> findByStatusAndEndDateLessThan(Status status, LocalDate date);
}