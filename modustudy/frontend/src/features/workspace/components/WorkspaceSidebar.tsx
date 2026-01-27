import { useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';

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
  icon: string;
  description?: string;
}

const menuItems: MenuItem[] = [
  {
    id: 'chat',
    label: '채팅',
    icon: 'chat',
    description: '스터디원들과 대화하기',
  },
  {
    id: 'materials',
    label: '자료실',
    icon: 'folder',
    description: '스터디 자료 공유',
  },
  {
    id: 'calendar',
    label: '일정',
    icon: 'calendar_today',
    description: '스터디 일정 확인',
  },
  {
    id: 'meeting',
    label: '미팅',
    icon: 'videocam',
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
          <span className="material-icons" style={{ fontSize: '18px' }}>
            settings
          </span>
        </button>
      </div>

      {/* 메뉴 리스트 */}
      <div className="workspace-sidebar__menu">
        {menuItems.map((item) => (
          <button
            key={item.id}
            className={cn(
              'workspace-sidebar__menu-item',
              activeMenu === item.id && 'workspace-sidebar__menu-item--active'
            )}
            onClick={() => handleMenuClick(item.id)}
            title={item.description}
          >
            <span className="material-icons workspace-sidebar__menu-icon">
              {item.icon}
            </span>
            <span className="workspace-sidebar__menu-label">{item.label}</span>
            {item.id === 'meeting' && (
              <span className="workspace-sidebar__menu-badge">
                <span className="material-icons" style={{ fontSize: '14px' }}>
                  open_in_new
                </span>
              </span>
            )}
          </button>
        ))}
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
          <span className="material-icons">info</span>
          <span>스터디 정보</span>
        </button>

        <button
          className="workspace-sidebar__action-btn"
          onClick={() => studyId && navigate(`/study/manage/${studyId}`)}
        >
          <span className="material-icons">manage_accounts</span>
          <span>스터디 관리</span>
        </button>
      </div>

      {/* 하단 미팅 시작 버튼 */}
      <div className="workspace-sidebar__footer">
        <button
          className="workspace-sidebar__meeting-btn"
          onClick={() => studyId && navigate(`/study/${studyId}/meetings`)}
        >
          <span className="material-icons">videocam</span>
          <span>미팅 시작하기</span>
        </button>
      </div>
    </div>
  );
};
