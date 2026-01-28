import { useState, useRef, useCallback } from 'react';

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
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // 텍스트 영역 높이 자동 조절
  const adjustHeight = useCallback(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 144)}px`;
    }
  }, []);

  // 메시지 전송
  const handleSend = useCallback(() => {
    const trimmed = content.trim();
    if (!trimmed || disabled) return;

    onSend(trimmed);
    setContent('');

    // 높이 초기화
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  }, [content, disabled, onSend]);

  // 키보드 이벤트 처리
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Enter로 전송 (Shift+Enter는 줄바꿈)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 입력 변경 처리
  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    adjustHeight();
  };

  return (
    <div className="message-input">
      <div className="message-input__wrapper">
        {/* 파일 첨부 버튼 */}
        <button
          type="button"
          className="message-input__left-btn"
          title="파일 첨부"
        >
          <span className="material-icons" style={{ fontSize: '24px' }}>
            add_circle
          </span>
        </button>

        {/* 텍스트 입력 영역 */}
        <textarea
          ref={textareaRef}
          className="message-input__field"
          placeholder={placeholder}
          value={content}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          rows={1}
        />

        {/* 전송 버튼 */}
        <button
          type="button"
          className="message-input__right-btn"
          onClick={handleSend}
          disabled={!content.trim() || disabled}
          title="전송"
        >
          <span className="material-icons" style={{ fontSize: '24px' }}>
            send
          </span>
        </button>
      </div>
    </div>
  );
};
