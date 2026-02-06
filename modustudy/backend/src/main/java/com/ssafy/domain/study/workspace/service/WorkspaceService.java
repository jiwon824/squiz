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
        if (workspaceRepository.existsByStudyId(studyId)) {
            throw new IllegalStateException("이미 해당 스터디의 워크스페이스가 존재합니다.");
        }

        Workspace workspace = workspaceRepository.save(Workspace.create(studyId));
        return WorkspaceResponse.from(workspace);
    }

    /**
     * 워크스페이스 ID로 조회
     */
    public WorkspaceResponse getWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
                });

        return WorkspaceResponse.from(workspace);
    }

    /**
     * 스터디 ID로 워크스페이스 조회
     */
    public WorkspaceResponse getWorkspaceByStudyId(Long studyId) {
        Workspace workspace = workspaceRepository.findByStudyId(studyId)
                .orElseThrow(() -> {
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
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
                });

        messageRepository.deleteAllByWorkspaceId(workspaceId);
        workspaceRepository.delete(workspace);
}

    /**
     * 스터디 ID로 워크스페이스 삭제
     */
    @Transactional
    public void deleteWorkspaceByStudyId(Long studyId) {
        Workspace workspace = workspaceRepository.findByStudyId(studyId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("해당 스터디의 워크스페이스를 찾을 수 없습니다.");
                });

        messageRepository.deleteAllByWorkspaceId(workspace.getId());
        workspaceRepository.delete(workspace);
}
}
