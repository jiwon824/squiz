package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.workspace.dto.response.WorkspaceResponse;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.repository.MessageRepository;
import com.ssafy.domain.study.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final MessageRepository messageRepository;

    /**
     * 워크스페이스 생성 (스터디 시작 시 호출)
     */
    @Transactional
    public WorkspaceResponse createWorkspace(Long studyId) {
        log.info("워크스페이스 생성 요청 - studyId: {}", studyId);

        if (workspaceRepository.existsByStudyId(studyId)) {
            log.warn("이미 워크스페이스가 존재합니다 - studyId: {}", studyId);
            throw new IllegalStateException("이미 해당 스터디의 워크스페이스가 존재합니다.");
        }

        Workspace workspace = workspaceRepository.save(Workspace.create(studyId));
        log.info("워크스페이스 생성 완료 - workspaceId: {}, studyId: {}", workspace.getId(), studyId);

        return WorkspaceResponse.from(workspace);
    }

    /**
     * 워크스페이스 ID로 조회
     */
    public WorkspaceResponse getWorkspace(Long workspaceId) {
        log.info("워크스페이스 조회 요청 - workspaceId: {}", workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("워크스페이스를 찾을 수 없습니다 - workspaceId: {}", workspaceId);
                    return new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
                });

        return WorkspaceResponse.from(workspace);
    }

    /**
     * 스터디 ID로 워크스페이스 조회
     */
    public WorkspaceResponse getWorkspaceByStudyId(Long studyId) {
        log.info("스터디 ID로 워크스페이스 조회 요청 - studyId: {}", studyId);

        Workspace workspace = workspaceRepository.findByStudyId(studyId)
                .orElseThrow(() -> {
                    log.warn("워크스페이스를 찾을 수 없습니다 - studyId: {}", studyId);
                    return new IllegalArgumentException("해당 스터디의 워크스페이스를 찾을 수 없습니다.");
                });

        return WorkspaceResponse.from(workspace);
    }

    /**
     * 워크스페이스 존재 여부 확인
     */
    public boolean existsWorkspace(Long studyId) {
        return workspaceRepository.existsByStudyId(studyId);
    }

    /**
     * 워크스페이스 삭제 (스터디 삭제 시 호출)
     */
    @Transactional
    public void deleteWorkspace(Long workspaceId) {
        log.info("워크스페이스 삭제 요청 - workspaceId: {}", workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.warn("워크스페이스를 찾을 수 없습니다 - workspaceId: {}", workspaceId);
                    return new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
                });

        messageRepository.deleteAllByWorkspaceId(workspaceId);
        log.info("워크스페이스 내 메시지 삭제 완료 - workspaceId: {}", workspaceId);

        workspaceRepository.delete(workspace);
        log.info("워크스페이스 삭제 완료 - workspaceId: {}", workspaceId);
    }

    /**
     * 스터디 ID로 워크스페이스 삭제
     */
    @Transactional
    public void deleteWorkspaceByStudyId(Long studyId) {
        log.info("스터디 ID로 워크스페이스 삭제 요청 - studyId: {}", studyId);

        Workspace workspace = workspaceRepository.findByStudyId(studyId)
                .orElseThrow(() -> {
                    log.warn("워크스페이스를 찾을 수 없습니다 - studyId: {}", studyId);
                    return new IllegalArgumentException("해당 스터디의 워크스페이스를 찾을 수 없습니다.");
                });

        messageRepository.deleteAllByWorkspaceId(workspace.getId());
        log.info("워크스페이스 내 메시지 삭제 완료 - workspaceId: {}", workspace.getId());

        workspaceRepository.delete(workspace);
        log.info("워크스페이스 삭제 완료 - studyId: {}", studyId);
    }
}