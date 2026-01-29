import { useState, useCallback } from 'react';
import { Send } from 'lucide-react';

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
    <div className="message-input">
      <div className="message-input__wrapper">
        {/* 텍스트 입력 영역 */}
        <input
          type="text"
          className="message-input__field"
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
          className="message-input__send-btn"
          title="전송"
        >
          <Send size={18} />
        </button>
      </div>
    </div>
  );
};
