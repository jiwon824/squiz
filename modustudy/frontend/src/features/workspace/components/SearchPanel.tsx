import { useState, useCallback, useRef, useEffect } from 'react';
import { Search, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Spinner } from '@/shared/components/Spinner';
import { workspaceApi } from '@/api/endpoints/workspaceApi';
import type { MessageResponse } from '../types';

interface SearchPanelProps {
  workspaceId: number;
  isOpen: boolean;
  onClose: () => void;
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

// 검색어 하이라이트 함수
const highlightText = (text: string, keyword: string) => {
  if (!keyword.trim()) return text;

  const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  const parts = text.split(regex);

  return parts.map((part, index) =>
    regex.test(part) ? (
      <mark key={index} className="bg-yellow-200 dark:bg-yellow-600 px-0.5 rounded">
        {part}
      </mark>
    ) : (
      <span key={index}>{part}</span>
    )
  );
};

export const SearchPanel: React.FC<SearchPanelProps> = ({
  workspaceId,
  isOpen,
  onClose,
  onMessageClick,
}) => {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<MessageResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  // 패널 열릴 때 입력창에 포커스
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // 패널 닫힐 때 상태 초기화
  useEffect(() => {
    if (!isOpen) {
      setKeyword('');
      setResults([]);
      setHasSearched(false);
    }
  }, [isOpen]);

  // 검색 실행
  const handleSearch = useCallback(async (searchKeyword: string) => {
    if (!searchKeyword.trim()) {
      setResults([]);
      setHasSearched(false);
      return;
    }

    setIsLoading(true);
    setHasSearched(true);

    try {
      const response = await workspaceApi.searchMessages(workspaceId, searchKeyword.trim());
      setResults(response.content);
    } catch (error) {
      console.error('검색 실패:', error);
      setResults([]);
    } finally {
      setIsLoading(false);
    }
  }, [workspaceId]);

  // 디바운스 검색
  const handleKeywordChange = useCallback((value: string) => {
    setKeyword(value);

    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
      handleSearch(value);
    }, 300);
  }, [handleSearch]);

  // 검색 결과 클릭
  const handleResultClick = useCallback((messageId: number) => {
    onMessageClick?.(messageId);
    onClose();
  }, [onMessageClick, onClose]);

  if (!isOpen) return null;

  return (
    <div className="search-panel">
      {/* 헤더 */}
      <div className="search-panel__header">
        <h3 className="search-panel__title">메시지 검색</h3>
        <button
          className="search-panel__close"
          onClick={onClose}
          title="닫기"
        >
          <X size={18} />
        </button>
      </div>

      {/* 검색 입력창 */}
      <div className="search-panel__input-wrapper">
        <Search size={16} className="search-panel__input-icon" />
        <input
          ref={inputRef}
          type="text"
          value={keyword}
          onChange={(e) => handleKeywordChange(e.target.value)}
          placeholder="메시지 내용을 검색하세요..."
          className="search-panel__input"
        />
        {keyword && (
          <button
            className="search-panel__clear"
            onClick={() => handleKeywordChange('')}
          >
            <X size={14} />
          </button>
        )}
      </div>

      {/* 검색 결과 */}
      <div className="search-panel__results">
        {isLoading ? (
          <div className="search-panel__loading">
            <Spinner size="sm" label="검색 중..." />
          </div>
        ) : hasSearched && results.length === 0 ? (
          <div className="search-panel__empty">
            <span className="search-panel__empty-icon">🔍</span>
            <p>검색 결과가 없습니다</p>
          </div>
        ) : (
          results.map((message) => (
            <button
              key={message.id}
              className="search-panel__result-item"
              onClick={() => handleResultClick(message.id)}
            >
              <div className="search-panel__result-header">
                <span className="search-panel__result-author">
                  {message.author.nickname}
                </span>
                <span className="search-panel__result-time">
                  {formatTime(message.createdAt)}
                </span>
              </div>
              <p className="search-panel__result-content">
                {highlightText(message.content, keyword)}
              </p>
            </button>
          ))
        )}
      </div>
    </div>
  );
};
