import { Pin } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';
import type { MessageResponse } from '../types';

interface MessageItemProps {
  message: MessageResponse;
  isGrouped?: boolean; // 같은 사용자의 연속 메시지인 경우
  isOwnMessage?: boolean; // 내가 보낸 메시지인 경우
  isPinned?: boolean; // 고정된 메시지인 경우
  onPinToggle?: (messageId: number) => void; // 고정/해제 토글
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

// URL을 클릭 가능한 링크로 변환하는 함수
const renderMessageContent = (content: string) => {
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  const parts = content.split(urlRegex);

  return parts.map((part, index) => {
    if (part.match(urlRegex)) {
      return (
        <a
          key={index}
          href={part}
          target="_blank"
          rel="noopener noreferrer"
          className="underline break-all hover:opacity-80"
          onClick={(e) => e.stopPropagation()}
        >
          {part}
        </a>
      );
    }
    return <span key={index}>{part}</span>;
  });
};

export const MessageItem: React.FC<MessageItemProps> = ({
  message,
  isGrouped = false,
  isOwnMessage = false,
  isPinned = false,
  onPinToggle,
}) => {
  const isSystemMessage = message.messageType === 'SYSTEM';

  // 시스템 메시지
  if (isSystemMessage) {
    return (
      <div className="flex justify-center my-2">
        <span className="text-xs text-gray-400 dark:text-gray-500 bg-gray-100 dark:bg-gray-700/50 px-3 py-1 rounded-full">
          {message.content}
        </span>
      </div>
    );
  }

  // 핀 버튼 렌더링
  const renderPinButton = () => {
    if (!onPinToggle) return null;
    return (
      <button
        onClick={() => onPinToggle(message.id)}
        className={cn(
          'message-item__pin-btn',
          'flex items-center justify-center w-6 h-6 rounded',
          'transition-all duration-150',
          isPinned
            ? 'text-yellow-500'
            : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 opacity-0 group-hover:opacity-100'
        )}
        title={isPinned ? '고정 해제' : '메시지 고정'}
      >
        <Pin size={14} className={isPinned ? 'fill-current' : ''} />
      </button>
    );
  };

  // 그룹화된 메시지 (같은 사용자 연속 메시지) - 아바타 없이 말풍선만
  if (isGrouped) {
    return (
      <div className={cn('group relative flex gap-2 mb-1', isOwnMessage ? 'flex-row-reverse' : 'flex-row')}>
        {/* 아바타 자리 (빈 공간) */}
        <div className="w-10 flex-shrink-0" />

        {/* 말풍선 + 전송됨 */}
        <div className={cn('flex flex-col max-w-[70%]', isOwnMessage ? 'items-end' : 'items-start')}>
          <div className="flex items-center gap-1">
            {isOwnMessage && renderPinButton()}
            <div
              className={cn(
                'px-4 py-2 rounded-2xl text-sm break-words',
                isOwnMessage
                  ? 'bg-blue-500 text-white rounded-tr-sm'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-100 rounded-tl-sm'
              )}
            >
              {renderMessageContent(message.content)}
            </div>
            {!isOwnMessage && renderPinButton()}
          </div>
        </div>
      </div>
    );
  }

  // 일반 메시지 - 아바타 + 이름 + 말풍선
  return (
    <div className={cn('group relative flex gap-2 mb-3', isOwnMessage ? 'flex-row-reverse' : 'flex-row')}>
      {/* 아바타 */}
      <div className="flex-shrink-0">
        <div
          className={cn(
            'w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm overflow-hidden',
            isOwnMessage
              ? 'bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300'
              : 'bg-gray-200 dark:bg-gray-600 text-gray-600 dark:text-gray-300'
          )}
        >
          <img
            src={getProfileImageUrl(message.author.profileImageUrl)}
            alt={message.author.nickname}
            className="w-full h-full rounded-full object-cover"
          />
        </div>
      </div>

      {/* 메시지 콘텐츠 */}
      <div
        className={cn('flex flex-col max-w-[70%]', isOwnMessage ? 'items-end' : 'items-start')}
      >
        {/* 헤더: 이름 + 시간 */}
        <div
          className={cn(
            'flex items-center gap-2 mb-1',
            isOwnMessage ? 'flex-row-reverse' : 'flex-row'
          )}
        >
          <span className="text-xs font-semibold text-gray-700 dark:text-gray-200">
            {isOwnMessage ? '나' : message.author.nickname}
          </span>
          <time className="text-[10px] text-gray-400 dark:text-gray-500">{formatTime(message.createdAt)}</time>
        </div>

        {/* 말풍선 + 핀 버튼 */}
        <div className="flex items-center gap-1">
          {isOwnMessage && renderPinButton()}
          <div
            className={cn(
              'px-4 py-2 rounded-2xl text-sm break-words',
              isOwnMessage
                ? 'bg-blue-500 text-white rounded-tr-sm'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-100 rounded-tl-sm'
            )}
          >
            {renderMessageContent(message.content)}
          </div>
          {!isOwnMessage && renderPinButton()}
        </div>
      </div>
    </div>
  );
};
