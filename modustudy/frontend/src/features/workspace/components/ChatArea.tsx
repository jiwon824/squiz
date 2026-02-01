import { useEffect, useRef } from 'react';
import { MessageItem } from './MessageItem';
import type { MessageResponse } from '../types';

interface ChatAreaProps {
  messages: MessageResponse[];
  isLoading?: boolean;
  onLoadMore?: () => void;
  hasMore?: boolean;
  /** 현재 로그인한 사용자 ID (내 메시지 구분용) - string 또는 number */
  currentUserId?: string | number;
  /** 고정된 메시지 ID 목록 */
  pinnedMessageIds?: number[];
  /** 메시지 고정/해제 토글 */
  onPinToggle?: (messageId: number) => void;
  /** 스크롤할 메시지 ID (검색/고정 메시지 클릭 시) */
  scrollToMessageId?: number | null;
  /** 스크롤 완료 후 콜백 */
  onScrollComplete?: () => void;
}

// 날짜 포맷 (구분선용)
const formatDateDivider = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);

  if (date.toDateString() === now.toDateString()) {
    return '오늘';
  }
  if (date.toDateString() === yesterday.toDateString()) {
    return '어제';
  }

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

// 같은 날짜인지 확인
const isSameDay = (date1: string, date2: string): boolean => {
  return new Date(date1).toDateString() === new Date(date2).toDateString();
};

// 같은 사용자의 연속 메시지인지 확인 (5분 이내)
const isGroupedMessage = (
  current: MessageResponse,
  previous: MessageResponse | null
): boolean => {
  if (!previous) return false;
  if (current.author.id !== previous.author.id) return false;
  if (current.messageType === 'SYSTEM' || previous.messageType === 'SYSTEM') return false;

  const timeDiff =
    new Date(current.createdAt).getTime() - new Date(previous.createdAt).getTime();
  return timeDiff < 5 * 60 * 1000; // 5분
};

export const ChatArea: React.FC<ChatAreaProps> = ({
  messages,
  isLoading = false,
  onLoadMore,
  hasMore = false,
  currentUserId,
  pinnedMessageIds = [],
  onPinToggle,
  scrollToMessageId,
  onScrollComplete,
}) => {
  // 고정 메시지 ID Set (빠른 조회용)
  const pinnedSet = new Set(pinnedMessageIds);
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  // 새 메시지 시 스크롤 하단으로
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  // 특정 메시지로 스크롤
  useEffect(() => {
    if (!scrollToMessageId || !scrollRef.current) return;

    // 약간의 딜레이 후 스크롤 (DOM 업데이트 대기)
    const timer = setTimeout(() => {
      const messageElement = scrollRef.current?.querySelector(
        `[data-message-id="${scrollToMessageId}"]`
      );

      if (messageElement) {
        messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' });

        // 하이라이트 효과 적용
        messageElement.classList.add('message-highlight');
        setTimeout(() => {
          messageElement.classList.remove('message-highlight');
        }, 2000);
      }

      onScrollComplete?.();
    }, 100);

    return () => clearTimeout(timer);
  }, [scrollToMessageId, onScrollComplete]);

  // 스크롤 상단 도달 시 이전 메시지 로드
  const handleScroll = () => {
    if (!scrollRef.current || !onLoadMore || !hasMore || isLoading) return;

    if (scrollRef.current.scrollTop === 0) {
      onLoadMore();
    }
  };

  // 빈 상태
  if (messages.length === 0 && !isLoading) {
    return (
      <div className="chat-area">
        <div className="chat-area__empty">
          <span className="chat-area__empty-icon material-icons">forum</span>
          <h3 className="chat-area__empty-title">대화를 시작해보세요!</h3>
          <p className="chat-area__empty-desc">
            이곳에서 스터디 멤버들과 자유롭게 대화할 수 있습니다.
            첫 번째 메시지를 보내보세요.
          </p>
        </div>
      </div>
    );
  }

  // 날짜별로 메시지 그룹화하며 렌더링
  const renderMessages = () => {
    const elements: React.ReactNode[] = [];
    let lastDate: string | null = null;

    messages.forEach((message, index) => {
      const messageDate = message.createdAt;
      const previousMessage = index > 0 ? messages[index - 1] : null;

      // 날짜 구분선 추가
      if (!lastDate || !isSameDay(lastDate, messageDate)) {
        elements.push(
          <div key={`divider-${messageDate}`} className="message-date-divider">
            <div className="message-date-divider__line" />
            <span className="message-date-divider__text">
              {formatDateDivider(messageDate)}
            </span>
            <div className="message-date-divider__line" />
          </div>
        );
        lastDate = messageDate;
      }

      // 메시지 렌더링 (타입이 다를 수 있으므로 문자열로 비교)
      const isOwn = currentUserId !== undefined && String(message.author.id) === String(currentUserId);
      const nextMessage = index < messages.length - 1 ? messages[index + 1] : null;
      // 다음 메시지가 없거나 다음 메시지가 다른 사용자면 그룹의 마지막
      const isLastInGroup = !nextMessage || String(nextMessage.author.id) !== String(message.author.id);

      elements.push(
        <div key={message.id} data-message-id={message.id}>
          <MessageItem
            message={message}
            isGrouped={isGroupedMessage(message, previousMessage)}
            isOwnMessage={isOwn}
            isLastInGroup={isLastInGroup}
            isPinned={pinnedSet.has(message.id)}
            onPinToggle={onPinToggle}
          />
        </div>
      );
    });

    return elements;
  };

  return (
    <div className="chat-area" ref={scrollRef} onScroll={handleScroll}>
      {isLoading && (
        <div className="flex justify-center py-4">
          <span className="material-icons animate-spin text-gray-500">
            refresh
          </span>
        </div>
      )}
      <div className="chat-area__messages">
        {renderMessages()}
        <div ref={bottomRef} />
      </div>
    </div>
  );
};
