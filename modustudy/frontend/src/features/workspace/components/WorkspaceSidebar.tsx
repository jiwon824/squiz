import { useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';
import { type StudySessionResponse } from '@/api/endpoints/sessionApi';
import {
  MessageSquare,
  FolderOpen,
  Calendar,
  Video,
  Info,
  UserCog,
  Sun,
  Moon,
  LucideIcon,
  Play,
} from 'lucide-react';

interface WorkspaceSidebarProps {
  studyId?: number;
  activeMenu?: 'chat' | 'materials' | 'calendar' | 'meeting';
  onMenuChange?: (menu: 'chat' | 'materials' | 'calendar' | 'meeting') => void;
  isDarkMode?: boolean;
  onToggleDarkMode?: () => void;
  /** 현재 진행 중인 세션 (있으면 미팅 참여 버튼 표시) */
  activeSession?: StudySessionResponse | null;
  /** 미팅으로 이동 콜백 (전환 애니메이션 처리용) */
  onNavigateToMeeting?: () => void;
}

// 메뉴 아이템 타입
interface MenuItem {
  id: 'chat' | 'materials' | 'calendar' | 'meeting';
  label: string;
  icon: LucideIcon;
  description?: string;
}

const menuItems: MenuItem[] = [
  {
    id: 'chat',
    label: '채팅',
    icon: MessageSquare,
    description: '스터디원과 소통하기',
  },
  {
    id: 'materials',
    label: '자료실',
    icon: FolderOpen,
    description: '스터디 자료 공유',
  },
  {
    id: 'calendar',
    label: '일정',
    icon: Calendar,
    description: '스터디 일정 확인',
  },
  {
    id: 'meeting',
    label: '미팅',
    icon: Video,
    description: '화상 미팅 기록',
  },
];

export const WorkspaceSidebar: React.FC<WorkspaceSidebarProps> = ({
  studyId,
  activeMenu = 'chat',
  onMenuChange,
  isDarkMode = true,
  onToggleDarkMode,
  activeSession,
  onNavigateToMeeting,
}) => {
  const navigate = useNavigate();

  // 메뉴 클릭 핸들러
  const handleMenuClick = (menuId: MenuItem['id']) => {
    onMenuChange?.(menuId);
  };

  return (
    <div className="workspace-sidebar">
      {/* 메뉴 리스트 */}
      <div className="workspace-sidebar__menu">
        {menuItems.map((item) => {
          const IconComponent = item.icon;
          return (
            <button
              key={item.id}
              className={cn(
                'workspace-sidebar__menu-item',
                activeMenu === item.id && 'workspace-sidebar__menu-item--active'
              )}
              onClick={() => handleMenuClick(item.id)}
              title={item.description}
            >
              <IconComponent size={20} className="workspace-sidebar__menu-icon" />
              <span className="workspace-sidebar__menu-label">{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* 구분선 */}
      <div className="workspace-sidebar__divider" />

      {/* 테마 토글 */}
      <div className="workspace-sidebar__quick-actions">
        <div className="workspace-sidebar__section-title">테마</div>

        <button
          className="workspace-sidebar__theme-toggle"
          onClick={onToggleDarkMode}
          title={isDarkMode ? '라이트 모드로 전환' : '다크 모드로 전환'}
        >
          <div className={cn('workspace-sidebar__toggle-track', isDarkMode && 'workspace-sidebar__toggle-track--dark')}>
            <div className="workspace-sidebar__toggle-thumb">
              {isDarkMode ? <Moon size={14} /> : <Sun size={14} />}
            </div>
          </div>
          <span>{isDarkMode ? '다크 모드' : '라이트 모드'}</span>
        </button>
      </div>

      {/* 구분선 */}
      <div className="workspace-sidebar__divider" />

      {/* 빠른 액션 */}
      <div className="workspace-sidebar__quick-actions">
        <div className="workspace-sidebar__section-title">빠른 액션</div>

        <button
          className="workspace-sidebar__action-btn"
          onClick={() => studyId && navigate(`/study/${studyId}`)}
        >
          <Info size={18} />
          <span>스터디 정보</span>
        </button>

        <button
          className="workspace-sidebar__action-btn"
          onClick={() => studyId && navigate(`/study/manage/${studyId}`)}
        >
          <UserCog size={18} />
          <span>스터디 관리</span>
        </button>
      </div>

      {/* 하단 미팅 참여 버튼 - 진행 중인 세션이 있을 때만 표시 */}
      {activeSession && (
        <div className="workspace-sidebar__footer">
          <button
            className="workspace-sidebar__meeting-btn workspace-sidebar__meeting-btn--active"
            onClick={() => onNavigateToMeeting ? onNavigateToMeeting() : studyId && navigate(`/study/${studyId}/meetings`)}
          >
            <Play size={20} />
            <span>미팅 참여하기</span>
          </button>
          <div className="workspace-sidebar__session-info">
            <span className="workspace-sidebar__session-title">
              {activeSession.title || '스터디 세션'}
            </span>
            <span className="workspace-sidebar__session-time">
              {new Date(activeSession.scheduledAt).toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
              })}
              {activeSession.durationMinutes && ` (${activeSession.durationMinutes}분)`}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
