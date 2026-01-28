import { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';
import { WorkspaceHeader } from './WorkspaceHeader';
import { WorkspaceSidebar } from './WorkspaceSidebar';
import { ChatArea } from './ChatArea';
import { MessageInput } from './MessageInput';
import { MemberList, type WorkspaceMember } from './MemberList';
import { WorkspaceCalendarArea } from './WorkspaceCalendarArea';
import { MaterialArea } from '@/features/material';
import { workspaceApi } from '@/api/endpoints/workspaceApi';
import { studyApi, type StudyMemberResponse } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import type { MessageResponse, WorkspaceResponse } from '../types';
import '../styles/workspace.css';

// 스터디 멤버를 워크스페이스 멤버 형식으로 변환
const toWorkspaceMember = (member: StudyMemberResponse): WorkspaceMember => ({
  id: member.userId,
  nickname: member.userNickname || member.userName,
  profileImageUrl: member.userProfileImage || null,
  role: member.role,
  isOnline: false, // 온라인 상태는 별도 WebSocket으로 관리 필요
});

export const WorkspacePage: React.FC = () => {
  const { studyId: studyIdParam } = useParams<{ studyId: string }>();
  const navigate = useNavigate();
  const showToast = useUIStore((state) => state.showToast);
  const currentUser = useAuthStore((state) => state.user);

  // studyId 파싱 (테스트 모드용 기본값)
  const studyId = studyIdParam ? Number(studyIdParam) : undefined;

  // 상태
  const [studyName, setStudyName] = useState('스터디 워크스페이스');
  const [workspace, setWorkspace] = useState<WorkspaceResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [isMembersVisible, setIsMembersVisible] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [isMessagesLoading, setIsMessagesLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeMenu, setActiveMenu] = useState<'chat' | 'materials' | 'calendar' | 'meeting'>('chat');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [hasMoreMessages, setHasMoreMessages] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLeader, setIsLeader] = useState(false);

  // 초기 데이터 로드
  useEffect(() => {
    const loadInitialData = async () => {
      if (!studyId) {
        setIsLoading(false);
        setError('스터디 ID가 필요합니다.');
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        // 1. 스터디 정보 + 멤버 목록 병렬 조회
        const [studyData, membersData] = await Promise.all([
          studyApi.getStudyDetail(studyId),
          studyApi.getStudyMembers(studyId),
        ]);

        setStudyName(studyData?.name || '스터디');

        // 멤버 목록 설정
        const approvedMembers = membersData.content.filter((m) => m.status === 'APPROVED');
        const workspaceMembers = approvedMembers.map(toWorkspaceMember);
        setMembers(workspaceMembers);

        // 현재 사용자가 리더인지 확인
        if (currentUser?.id) {
          const currentMember = approvedMembers.find(
            (m) => String(m.userId) === String(currentUser.id)
          );
          setIsLeader(currentMember?.role === 'LEADER');
        }

        // 2. 워크스페이스 조회 (없으면 생성)
        let workspaceData: WorkspaceResponse;
        try {
          workspaceData = await workspaceApi.getWorkspaceByStudyId(studyId);
        } catch (wsError: any) {
          // 워크스페이스가 없으면 생성
          if (wsError?.response?.status === 400 || wsError?.response?.status === 404) {
            workspaceData = await workspaceApi.createWorkspace(studyId);
          } else {
            throw wsError;
          }
        }
        setWorkspace(workspaceData);

        // 3. 메시지 목록 조회
        const messagesData = await workspaceApi.getMessages(workspaceData.id);
        // 최신 메시지가 아래로 가도록 역순 정렬
        setMessages(messagesData.content.reverse());
        setHasMoreMessages(!messagesData.last);
        setCurrentPage(0);
      } catch (err: any) {
        const errorMessage =
          err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          '워크스페이스를 불러오는데 실패했습니다.';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    loadInitialData();
  }, [studyId]);

  // 다크모드 토글
  const handleToggleDarkMode = useCallback(() => {
    setIsDarkMode((prev) => !prev);
  }, []);

  // 메시지 전송 핸들러
  const handleSendMessage = useCallback(
    async (content: string) => {
      if (!workspace) {
        return;
      }

      try {
        const newMessage = await workspaceApi.sendMessage(workspace.id, {
          content,
          messageType: 'TEXT',
        });
        setMessages((prev) => [...prev, newMessage]);
      } catch (err: any) {
        showToast?.('메시지 전송에 실패했습니다.', 'error');
      }
    },
    [workspace, showToast]
  );

  // 멤버 목록 토글
  const handleToggleMembers = useCallback(() => {
    setIsMembersVisible((prev) => !prev);
  }, []);

  // 이전 메시지 로드 (무한 스크롤)
  const handleLoadMore = useCallback(async () => {
    if (!workspace || isMessagesLoading || !hasMoreMessages) return;

    setIsMessagesLoading(true);
    try {
      const nextPage = currentPage + 1;
      const messagesData = await workspaceApi.getMessages(workspace.id, nextPage);
      // 이전 메시지는 앞에 추가
      setMessages((prev) => [...messagesData.content.reverse(), ...prev]);
      setHasMoreMessages(!messagesData.last);
      setCurrentPage(nextPage);
    } catch (err) {
      // 이전 메시지 로드 실패 시 무시
    } finally {
      setIsMessagesLoading(false);
    }
  }, [workspace, isMessagesLoading, hasMoreMessages, currentPage]);

  // 뒤로가기
  const handleGoBack = useCallback(() => {
    if (studyId) {
      navigate(`/study/${studyId}`);
    } else {
      navigate('/study');
    }
  }, [navigate, studyId]);

  // 로딩 상태
  if (isLoading) {
    return (
      <div className={cn('workspace-container', isDarkMode && 'workspace-container--dark')}>
        <div className="workspace-loading">
          <div className="loading-spinner" />
          <span>워크스페이스 불러오는 중...</span>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className={cn('workspace-container', isDarkMode && 'workspace-container--dark')}>
        <div className="workspace-error">
          <span className="workspace-error__icon">⚠️</span>
          <h2>오류가 발생했습니다</h2>
          <p>{error}</p>
          <button onClick={handleGoBack} className="workspace-error__btn">
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={cn('workspace-container', isDarkMode && 'workspace-container--dark')}>
      {/* 메인 컨텐츠 영역 */}
      <div className="workspace-content">
        {/* 상단 헤더 */}
        <WorkspaceHeader
          studyName={studyName}
          memberCount={members.length}
          onToggleMembers={handleToggleMembers}
          isMembersVisible={isMembersVisible}
          onGoBack={handleGoBack}
        />

        {/* 본문 영역 */}
        <div className="workspace-body">
          {/* 왼쪽 사이드바 */}
          <WorkspaceSidebar
            studyId={studyId || 0}
            activeMenu={activeMenu}
            onMenuChange={setActiveMenu}
            isDarkMode={isDarkMode}
            onToggleDarkMode={handleToggleDarkMode}
          />

          {/* 메인 콘텐츠 영역 */}
          <div className="workspace-main">
            {activeMenu === 'chat' && (
              <>
                <ChatArea
                  messages={messages}
                  isLoading={isMessagesLoading}
                  onLoadMore={handleLoadMore}
                  hasMore={hasMoreMessages}
                />
                <MessageInput
                  onSend={handleSendMessage}
                  placeholder={`${studyName}에 메시지 보내기`}
                />
              </>
            )}

            {activeMenu === 'materials' && studyId && <MaterialArea studyId={studyId} />}

            {activeMenu === 'calendar' && studyId && (
              <WorkspaceCalendarArea studyId={studyId} isLeader={isLeader} />
            )}
          </div>
        </div>
      </div>

      {/* 멤버 목록 사이드바 */}
      <MemberList
        members={members}
        isVisible={isMembersVisible}
        onMemberClick={() => {
          // TODO: 멤버 클릭 시 프로필 보기 등 기능 추가
        }}
      />
    </div>
  );
};
