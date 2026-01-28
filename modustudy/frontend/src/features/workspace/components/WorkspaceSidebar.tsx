import { useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';
import {
  MessageSquare,
  FolderOpen,
  Calendar,
  Video,
  Settings,
  Info,
  UserCog,
  ExternalLink,
  LucideIcon,
} from 'lucide-react';

interface WorkspaceSidebarProps {
  studyId?: number;
  studyName?: string;
  activeMenu?: 'chat' | 'materials' | 'calendar' | 'meeting';
  onMenuChange?: (menu: 'chat' | 'materials' | 'calendar' | 'meeting') => void;
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
    description: '스터디원들과 대화하기',
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
    description: '화상 미팅 참여',
  },
];

export const WorkspaceSidebar: React.FC<WorkspaceSidebarProps> = ({
  studyId,
  studyName = '스터디',
  activeMenu = 'chat',
  onMenuChange,
}) => {
  const navigate = useNavigate();

  // 메뉴 클릭 핸들러
  const handleMenuClick = (menuId: MenuItem['id']) => {
    if (menuId === 'meeting' && studyId) {
      // 미팅은 별도 페이지로 이동
      navigate(`/study/${studyId}/meetings`);
      return;
    }

    onMenuChange?.(menuId);
  };

  return (
    <div className="workspace-sidebar">
      {/* 스터디 헤더 */}
      <div className="workspace-sidebar__header">
        <div className="workspace-sidebar__study-name">{studyName}</div>
        <button className="workspace-sidebar__settings-btn" title="스터디 설정">
          <Settings size={18} />
        </button>
      </div>

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
              {item.id === 'meeting' && (
                <span className="workspace-sidebar__menu-badge">
                  <ExternalLink size={14} />
                </span>
              )}
            </button>
          );
        })}
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

      {/* 하단 미팅 시작 버튼 */}
      <div className="workspace-sidebar__footer">
        <button
          className="workspace-sidebar__meeting-btn"
          onClick={() => studyId && navigate(`/study/${studyId}/meetings`)}
        >
          <Video size={20} />
          <span>미팅 시작하기</span>
        </button>
      </div>
    </div>
  );
};
