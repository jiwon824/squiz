import { cn } from '@/shared/utils/cn';
import { Users, Search, Pin, ChevronLeft } from 'lucide-react';

interface WorkspaceHeaderProps {
  studyName: string;
  memberCount?: number;
  onToggleMembers?: () => void;
  isMembersVisible?: boolean;
  onGoBack?: () => void;
  // 검색 관련
  onToggleSearch?: () => void;
  isSearchOpen?: boolean;
  // 고정 메시지 관련
  onTogglePinned?: () => void;
  isPinnedOpen?: boolean;
  pinnedCount?: number;
}

export const WorkspaceHeader: React.FC<WorkspaceHeaderProps> = ({
  studyName,
  memberCount: _memberCount = 0,
  onToggleMembers,
  isMembersVisible = true,
  onGoBack,
  onToggleSearch,
  isSearchOpen = false,
  onTogglePinned,
  isPinnedOpen = false,
  pinnedCount = 0,
}) => {
  return (
    <div className="workspace-header">
      <div className="workspace-header__title">
        <button
          className="workspace-header__back-btn"
          onClick={onGoBack}
          title="돌아가기"
        >
          <ChevronLeft size={20} />
        </button>
        <span className="workspace-header__study-name">{studyName}</span>
      </div>

      <div className="workspace-header__actions">
        {/* 멤버 목록 토글 */}
        <button
          className={cn(
            'workspace-header__action-btn',
            isMembersVisible && 'workspace-header__action-btn--active'
          )}
          onClick={onToggleMembers}
          title={isMembersVisible ? '멤버 목록 숨기기' : '멤버 목록 보기'}
        >
          <Users size={22} />
        </button>

        {/* 검색 버튼 */}
        <button
          className={cn(
            'workspace-header__action-btn',
            isSearchOpen && 'workspace-header__action-btn--active'
          )}
          onClick={onToggleSearch}
          title="검색"
        >
          <Search size={22} />
        </button>

        {/* 핀 메시지 */}
        <button
          className={cn(
            'workspace-header__action-btn',
            isPinnedOpen && 'workspace-header__action-btn--active'
          )}
          onClick={onTogglePinned}
          title="고정된 메시지"
        >
          <Pin size={22} />
          {pinnedCount > 0 && (
            <span className="workspace-header__badge">{pinnedCount}</span>
          )}
        </button>
      </div>
    </div>
  );
};
