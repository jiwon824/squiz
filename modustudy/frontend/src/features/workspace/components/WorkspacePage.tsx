import { useState, useCallback } from 'react';
import { cn } from '@/shared/utils/cn';
import { WorkspaceHeader } from './WorkspaceHeader';
import { WorkspaceSidebar } from './WorkspaceSidebar';
import { ChatArea } from './ChatArea';
import { MessageInput } from './MessageInput';
import { MemberList, type WorkspaceMember } from './MemberList';
import type { MessageResponse } from '../types';
import '../styles/workspace.css';

interface WorkspacePageProps {
  studyId?: number;
  studyName?: string;
}

// 임시 목업 데이터
const mockMembers: WorkspaceMember[] = [
  {
    id: 1,
    nickname: '김철수',
    profileImageUrl: null,
    role: 'LEADER',
    isOnline: true,
  },
  {
    id: 2,
    nickname: '이영희',
    profileImageUrl: null,
    role: 'MEMBER',
    isOnline: true,
  },
  {
    id: 3,
    nickname: '박지민',
    profileImageUrl: null,
    role: 'MEMBER',
    isOnline: false,
  },
  {
    id: 4,
    nickname: '정민수',
    profileImageUrl: null,
    role: 'MEMBER',
    isOnline: true,
  },
];

const mockMessages: MessageResponse[] = [
  {
    id: 1,
    workspaceId: 1,
    author: { id: 1, nickname: '김철수', profileImageUrl: null },
    content: '안녕하세요! 오늘 스터디 잘 부탁드립니다 👋',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    updatedAt: null,
  },
  {
    id: 2,
    workspaceId: 1,
    author: { id: 2, nickname: '이영희', profileImageUrl: null },
    content: '네! 저도 잘 부탁드려요~',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 3600000 * 1.5).toISOString(),
    updatedAt: null,
  },
  {
    id: 3,
    workspaceId: 1,
    author: { id: 1, nickname: '김철수', profileImageUrl: null },
    content: '오늘은 React hooks에 대해서 같이 공부해볼까요?',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: null,
  },
  {
    id: 4,
    workspaceId: 1,
    author: { id: 1, nickname: '김철수', profileImageUrl: null },
    content: 'useEffect, useState, useCallback 등을 다뤄보면 좋을 것 같아요',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 3600000 + 30000).toISOString(),
    updatedAt: null,
  },
  {
    id: 5,
    workspaceId: 1,
    author: { id: 3, nickname: '박지민', profileImageUrl: null },
    content: '좋아요! 저는 useCallback이랑 useMemo 차이가 헷갈려서 그 부분 집중적으로 보고 싶어요',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 1800000).toISOString(),
    updatedAt: null,
  },
  {
    id: 6,
    workspaceId: 1,
    author: { id: 4, nickname: '정민수', profileImageUrl: null },
    content: '저도 참여합니다! Custom hooks도 같이 다뤄볼 수 있을까요?',
    messageType: 'TEXT',
    createdAt: new Date(Date.now() - 600000).toISOString(),
    updatedAt: null,
  },
];

export const WorkspacePage: React.FC<WorkspacePageProps> = ({
  studyId = 1, // 테스트용 기본값
  studyName = '스터디 워크스페이스',
}) => {
  const [messages, setMessages] = useState<MessageResponse[]>(mockMessages);
  const [members] = useState<WorkspaceMember[]>(mockMembers);
  const [isMembersVisible, setIsMembersVisible] = useState(true);
  const [isLoading] = useState(false);
  const [activeMenu, setActiveMenu] = useState<'chat' | 'materials' | 'calendar' | 'meeting'>('chat');
  const [isDarkMode, setIsDarkMode] = useState(true); // 기본값: 다크모드

  // 다크모드 토글
  const handleToggleDarkMode = useCallback(() => {
    setIsDarkMode((prev) => !prev);
  }, []);

  // 메시지 전송 핸들러
  const handleSendMessage = useCallback((content: string) => {
    const newMessage: MessageResponse = {
      id: Date.now(),
      workspaceId: studyId || 1,
      author: {
        id: 1, // 현재 사용자 ID (실제로는 authStore에서 가져옴)
        nickname: '김철수',
        profileImageUrl: null,
      },
      content,
      messageType: 'TEXT',
      createdAt: new Date().toISOString(),
      updatedAt: null,
    };

    setMessages((prev) => [...prev, newMessage]);
  }, [studyId]);

  // 멤버 목록 토글
  const handleToggleMembers = useCallback(() => {
    setIsMembersVisible((prev) => !prev);
  }, []);

  // 이전 메시지 로드 (무한 스크롤)
  const handleLoadMore = useCallback(() => {
    // TODO: API 연동 시 구현
    console.log('Load more messages');
  }, []);

  return (
    <div className={cn('workspace-container', isDarkMode && 'workspace-container--dark')}>
      {/* 메인 컨텐츠 영역 (헤더 + 본문) - member-list에 의해 밀림 */}
      <div className="workspace-content">
        {/* 상단 헤더 */}
        <WorkspaceHeader
          studyName={studyName}
          memberCount={members.length}
          onToggleMembers={handleToggleMembers}
          isMembersVisible={isMembersVisible}
        />

        {/* 본문 영역 */}
        <div className="workspace-body">
          {/* 왼쪽 사이드바 */}
          <WorkspaceSidebar
            studyId={studyId}
            activeMenu={activeMenu}
            onMenuChange={setActiveMenu}
            isDarkMode={isDarkMode}
            onToggleDarkMode={handleToggleDarkMode}
          />

          {/* 메인 채팅 영역 */}
          <div className="workspace-main">
            <ChatArea
              messages={messages}
              isLoading={isLoading}
              onLoadMore={handleLoadMore}
              hasMore={false}
            />

            <MessageInput
              onSend={handleSendMessage}
              placeholder={`#${studyName}에 메시지 보내기`}
            />
          </div>
        </div>
      </div>

      {/* 멤버 목록 사이드바 - 오른쪽에서 슬라이드 인 */}
      <MemberList
        members={members}
        isVisible={isMembersVisible}
        onMemberClick={(member) => {
          console.log('Member clicked:', member);
        }}
      />
    </div>
  );
};
