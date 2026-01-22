package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.StudyMemberResponse;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyMemberService {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;

    /**
     * 스터디 멤버 목록 조회
     * - 승인된 멤버만 조회 (APPROVED)
     */
    public Page<StudyMemberResponse> getStudyMembers(Long studyId, Pageable pageable) {
        log.info("스터디 멤버 목록 조회 - studyId: {}", studyId);

        // 1. 스터디 존재 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // 2. 승인된 멤버 조회
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

        // 3. User 정보 조회 (한 번에 가져오기)
        List<Long> userIds = members.stream()
                .map(StudyMember::getUserId)
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. Response 변환
        List<StudyMemberResponse> responses = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    return StudyMemberResponse.from(member, user);
                })
                .toList();

        log.info("스터디 멤버 목록 조회 완료 - studyId: {}, count: {}", studyId, responses.size());

        // 5. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());

        List<StudyMemberResponse> pagedList = responses.subList(
                Math.min(start, responses.size()),
                Math.min(end, responses.size())
        );

        return new PageImpl<>(pagedList, pageable, responses.size());
    }

    /**
     * 스터디 멤버 수 조회
     */
    public int countStudyMembers(Long studyId) {
        log.info("스터디 멤버 수 조회 - studyId: {}", studyId);

        // 스터디 존재 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        return studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
    }

    /**
     * 특정 사용자가 스터디 멤버인지 확인
     */
    public boolean isMember(Long studyId, Long userId) {
        log.info("멤버 여부 확인 - studyId: {}, userId: {}", studyId, userId);

        return studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, MemberStatus.APPROVED);
    }
}