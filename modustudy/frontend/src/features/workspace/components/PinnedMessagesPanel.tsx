import { useCallback } from 'react';
import { Pin, X } from 'lucide-react';
import { getProfileImageUrl } from '@/shared/utils/profileImage';
import type { MessageResponse } from '../types';

interface PinnedMessagesPanelProps {
  isVisible: boolean;
  pinnedMessages: MessageResponse[];
  onUnpin: (messageId: number) => void;
  onMessageClick?: (messageId: number) => void;
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

export const PinnedMessagesPanel: React.FC<PinnedMessagesPanelProps> = ({
  isVisible,
  pinnedMessages,
  onUnpin,
  onMessageClick,
}) => {
  // 메시지 클릭 핸들러
  const handleMessageClick = useCallback((messageId: number) => {
    onMessageClick?.(messageId);
  }, [onMessageClick]);

  // 고정 해제 핸들러
  const handleUnpin = useCallback((e: React.MouseEvent, messageId: number) => {
    e.stopPropagation();
    onUnpin(messageId);
  }, [onUnpin]);

  // 숨김 상태면 렌더링하지 않음
  if (!isVisible) return null;

  return (
    <div className="pinned-sidebar">
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
                onClick={() => handleMessageClick(message.id)}
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
                  onClick={(e) => handleUnpin(e, message.id)}
                  title="고정 해제"
                >
                  <X size={14} />
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
