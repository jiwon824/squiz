import { cn } from '@/shared/utils/cn';
import type { MessageResponse } from '../types';

interface MessageItemProps {
  message: MessageResponse;
  isGrouped?: boolean; // 같은 사용자의 연속 메시지인 경우
}

// 시간 포맷 (오늘이면 시간만, 아니면 날짜+시간)
const formatTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();

  if (isToday) {
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const MessageItem: React.FC<MessageItemProps> = ({
  message,
  isGrouped = false,
}) => {
  const isSystemMessage = message.messageType === 'SYSTEM';

  // 시스템 메시지
  if (isSystemMessage) {
    return (
      <div className={cn('message-item', 'message-item--system')}>
        <div className="message-item__content">
          <p className="message-item__text">{message.content}</p>
        </div>
      </div>
    );
  }

  // 그룹화된 메시지 (같은 사용자 연속 메시지)
  if (isGrouped) {
    return (
      <div className={cn('message-item', 'message-item--grouped')}>
        <div className="message-item__content">
          <p className="message-item__text">{message.content}</p>
        </div>
      </div>
    );
  }

  // 일반 메시지
  return (
    <div className="message-item">
      <div className="message-item__avatar">
        {message.author.profileImageUrl ? (
          <img
            src={message.author.profileImageUrl}
            alt={message.author.nickname}
          />
        ) : (
          message.author.nickname.charAt(0).toUpperCase()
        )}
      </div>
      <div className="message-item__content">
        <div className="message-item__header">
          <span className="message-item__author">{message.author.nickname}</span>
          <span className="message-item__timestamp">
            {formatTime(message.createdAt)}
          </span>
        </div>
        <p className="message-item__text">{message.content}</p>
      </div>
    </div>
  );
};
