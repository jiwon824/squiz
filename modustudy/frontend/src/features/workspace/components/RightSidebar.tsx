import { motion } from 'framer-motion';
import { Pin, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';
import type { MessageResponse } from '../types';

// 사이드바 콘텐츠 타입
export type SidebarContent = 'none' | 'members' | 'pinned';

// 멤버 타입 정의
export interface WorkspaceMember {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: 'LEADER' | 'MEMBER';
  isOnline?: boolean;
  isIdle?: boolean;
}

interface RightSidebarProps {
  // 현재 표시할 콘텐츠 ('none'이면 닫힘)
  activeContent: SidebarContent;
  // 멤버 목록 데이터
  members: WorkspaceMember[];
  onMemberClick?: (member: WorkspaceMember) => void;
  // 고정 메시지 데이터
  pinnedMessages: MessageResponse[];
  onUnpin: (messageId: number) => void;
  onPinnedMessageClick?: (messageId: number) => void;
}

// 시간 포맷
const formatTime = (dateString: string): string => {
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const RightSidebar: React.FC<RightSidebarProps> = ({
  activeContent,
  members,
  onMemberClick,
  pinnedMessages,
  onUnpin,
  onPinnedMessageClick,
}) => {
  const isOpen = activeContent !== 'none';

  // 멤버 역할별 분류
  const leaders = members.filter((m) => m.role === 'LEADER');
  const regularMembers = members.filter((m) => m.role === 'MEMBER');

  // 상태별 정렬 (온라인 > 자리비움 > 오프라인)
  const sortByOnline = (list: WorkspaceMember[]) =>
    [...list].sort((a, b) => {
      const rank = (member: WorkspaceMember) => {
        if (!member.isOnline) return 2;
        if (member.isIdle) return 1;
        return 0;
      };
      return rank(a) - rank(b);
    });

  // 멤버 렌더링
  const renderMember = (member: WorkspaceMember) => (
    <div
      key={member.id}
      className="member-list__item"
      onClick={() => onMemberClick?.(member)}
    >
      <div className="member-list__avatar">
        <img src={getProfileImageUrl(member.profileImageUrl)} alt={member.nickname} />
        <div
          className={cn(
            'member-list__status',
            !member.isOnline
              ? 'member-list__status--offline'
              : member.isIdle
              ? 'member-list__status--idle'
              : 'member-list__status--online'
          )}
        />
      </div>
      <div>
        <div className="member-list__name">{member.nickname}</div>
      </div>
    </div>
  );

  // 멤버 목록 콘텐츠
  const renderMembersContent = () => (
    <div className="member-list__inner">
      {/* 리더 섹션 */}
      {leaders.length > 0 && (
        <div className="member-list__section">
          <div className="member-list__title">
            스터디장 — {leaders.length}
          </div>
          {sortByOnline(leaders).map(renderMember)}
        </div>
      )}

      {/* 일반 멤버 섹션 */}
      {regularMembers.length > 0 && (
        <div className="member-list__section">
          <div className="member-list__title">
            멤버 — {regularMembers.length}
          </div>
          {sortByOnline(regularMembers).map(renderMember)}
        </div>
      )}

      {/* 멤버가 없는 경우 */}
      {members.length === 0 && (
        <div className="member-list__section">
          <div className="member-list__title">멤버 없음</div>
        </div>
      )}
    </div>
  );

  // 고정 메시지 콘텐츠
  const renderPinnedContent = () => (
    <div className="pinned-sidebar__inner">
      {/* 헤더 */}
      <div className="pinned-sidebar__header">
        <Pin size={16} className="pinned-sidebar__icon" />
        <span className="pinned-sidebar__title">고정된 메시지</span>
        <span className="pinned-sidebar__count">{pinnedMessages.length}</span>
      </div>

      {/* 메시지 목록 */}
      <div className="pinned-sidebar__list">
        {pinnedMessages.length === 0 ? (
          <div className="pinned-sidebar__empty">
            <Pin size={24} className="pinned-sidebar__empty-icon" />
            <p className="pinned-sidebar__empty-text">고정된 메시지가 없습니다</p>
          </div>
        ) : (
          pinnedMessages.map((message) => (
            <div
              key={message.id}
              className="pinned-sidebar__item"
              onClick={() => onPinnedMessageClick?.(message.id)}
            >
              <div className="pinned-sidebar__item-header">
                <img
                  src={getProfileImageUrl(message.author.profileImageUrl)}
                  alt={message.author.nickname}
                  className="pinned-sidebar__item-avatar"
                />
                <span className="pinned-sidebar__item-author">
                  {message.author.nickname}
                </span>
                <span className="pinned-sidebar__item-time">
                  {formatTime(message.createdAt)}
                </span>
              </div>
              <p className="pinned-sidebar__item-content">
                {message.content}
              </p>
              <button
                className="pinned-sidebar__unpin"
                onClick={(e) => {
                  e.stopPropagation();
                  onUnpin(message.id);
                }}
                title="고정 해제"
              >
                <X size={14} />
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );

  return (
    <motion.div
      className="right-sidebar"
      initial={{ width: 240 }}
      animate={{ width: isOpen ? 240 : 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
    >
      {/* 콘텐츠 전환 시 애니메이션 없이 바로 변경 */}
      {activeContent === 'members' && renderMembersContent()}
      {activeContent === 'pinned' && renderPinnedContent()}
    </motion.div>
  );
};
