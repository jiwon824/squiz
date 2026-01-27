import { useState } from 'react';
import { cn } from '@/shared/utils/cn';

interface WorkspaceHeaderProps {
  studyName: string;
  memberCount?: number;
  onToggleMembers?: () => void;
  isMembersVisible?: boolean;
}

export const WorkspaceHeader: React.FC<WorkspaceHeaderProps> = ({
  studyName,
  memberCount = 0,
  onToggleMembers,
  isMembersVisible = true,
}) => {
  return (
    <div className="workspace-header">
      <div className="workspace-header__title">
        <span className="workspace-header__hash">#</span>
        <span>{studyName}</span>
      </div>

      <div className="workspace-header__actions">
        {/* 멤버 목록 토글 */}
        <button
          className={cn(
            'workspace-header__action-btn',
            isMembersVisible && 'text-white'
          )}
          onClick={onToggleMembers}
          title={isMembersVisible ? '멤버 목록 숨기기' : '멤버 목록 보기'}
        >
          <span className="material-icons" style={{ fontSize: '24px' }}>
            people
          </span>
        </button>

        {/* 검색 버튼 */}
        <button className="workspace-header__action-btn" title="검색">
          <span className="material-icons" style={{ fontSize: '24px' }}>
            search
          </span>
        </button>

        {/* 핀 메시지 */}
        <button className="workspace-header__action-btn" title="고정된 메시지">
          <span className="material-icons" style={{ fontSize: '24px' }}>
            push_pin
          </span>
        </button>
      </div>
    </div>
  );
};
