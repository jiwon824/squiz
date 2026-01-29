import { useState, useCallback } from 'react';
import { Send } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface MessageInputProps {
  onSend: (content: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  onSend,
  placeholder = '메시지를 입력하세요',
  disabled = false,
}) => {
  const [content, setContent] = useState('');

  // 메시지 전송
  const handleSend = useCallback(() => {
    const trimmed = content.trim();
    if (!trimmed || disabled) return;

    onSend(trimmed);
    setContent('');
  }, [content, disabled, onSend]);

  // 키보드 이벤트 처리
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // Enter로 전송 (Shift+Enter는 줄바꿈 - input에서는 무시)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
      <div className="flex gap-2">
        {/* 텍스트 입력 영역 */}
        <input
          type="text"
          className={cn(
            'flex-1 px-4 py-2.5 text-sm rounded-lg',
            'border border-gray-200 dark:border-gray-600',
            'bg-white dark:bg-gray-700',
            'text-gray-800 dark:text-gray-100',
            'placeholder-gray-400 dark:placeholder-gray-500',
            'focus:outline-none focus:border-blue-500 dark:focus:border-blue-400',
            'disabled:opacity-50 disabled:cursor-not-allowed'
          )}
          placeholder={placeholder}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />

        {/* 전송 버튼 */}
        <button
          type="button"
          onClick={handleSend}
          disabled={!content.trim() || disabled}
          className={cn(
            'w-11 h-11 flex items-center justify-center rounded-lg transition-colors',
            'bg-blue-500 text-white hover:bg-blue-600',
            'disabled:opacity-50 disabled:cursor-not-allowed'
          )}
          title="전송"
        >
          <Send size={18} />
        </button>
      </div>
    </div>
  );
};
