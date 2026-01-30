import { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';
import { WorkspaceHeader } from './WorkspaceHeader';
import { WorkspaceSidebar } from './WorkspaceSidebar';
import { ChatArea } from './ChatArea';
import { MessageInput } from './MessageInput';
import { MemberList, type WorkspaceMember } from './MemberList';
import { WorkspaceCalendarArea } from './WorkspaceCalendarArea';
import { MaterialArea } from '@/features/material';
import MeetingHistoryPanel from '@/features/meeting/components/MeetingHistoryPanel';
import { workspaceApi } from '@/api/endpoints/workspaceApi';
import { studyApi, type StudyMemberResponse } from '@/api/endpoints/studyApi';
import { sessionApi, type StudySessionResponse } from '@/api/endpoints/sessionApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { workspaceWebSocket } from '@/api/websocket/workspaceWebSocketService';
import type { WorkspaceWebSocketEvent } from '@/api/websocket/workspaceWebSocketTypes';
import type { MessageResponse, WorkspaceResponse } from '../types';
import '../styles/workspace.css';

/**
 * 현재 시간 기준으로 세션이 진행 중인지 확인
 */
const isSessionInProgress = (session: StudySessionResponse): boolean => {
  if (session.status === 'CANCELLED') return false;

  const now = new Date();
  const startTime = new Date(session.scheduledAt);
  const durationMs = (session.durationMinutes || 60) * 60 * 1000;
  const endTime = new Date(startTime.getTime() + durationMs);

  return now >= startTime && now < endTime;
};

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

  // studyId 파싱 (테스트 모드용 기본값?)
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
  const [sessions, setSessions] = useState<StudySessionResponse[]>([]);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [isWebSocketConnected, setIsWebSocketConnected] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const [activeMeetingId, setActiveMeetingId] = useState<number | null>(null);
  const [activeMeetingEnded, setActiveMeetingEnded] = useState<boolean | null>(null);

  // 미팅에서 진입 애니메이션 플래그(최초 렌더 한 번만 확인)
  const isEnteringFromMeeting = useMemo(() => {
    const flag = sessionStorage.getItem('fromMeeting') === 'true';
    if (flag) {
      sessionStorage.removeItem('fromMeeting');
    }
    return flag;
  }, []);

  // 대시보드에서 진입 애니메이션 플래그(최초 렌더 한 번만 확인)
  const isEnteringFromDashboard = useMemo(() => {
    const flag = sessionStorage.getItem('fromDashboard') === 'true';
    if (flag) {
      sessionStorage.removeItem('fromDashboard');
    }
    return flag;
  }, []);

  // WebSocket 핸들러를 위한 ref (상태 변경 시 최신 값 참조)
  const messagesRef = useRef<MessageResponse[]>([]);
  messagesRef.current = messages;

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

        // 스터디 시작일 체크: 시작일 이전이면 상세 페이지로 리다이렉트
        if (studyData?.startDate) {
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          const startDate = new Date(studyData.startDate);
          startDate.setHours(0, 0, 0, 0);

          if (today < startDate) {
            const formattedDate = startDate.toLocaleDateString('ko-KR', {
              year: 'numeric',
              month: 'long',
              day: 'numeric'
            });
            showToast?.(`워크스페이스는 스터디 시작일(${formattedDate})부터 이용할 수 있습니다.`, 'info');
            navigate(`/study/v3/${studyId}`);
            return;
          }
        }

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
          '워크스페이스를 불러오는 데 실패했습니다.';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    loadInitialData();
  }, [studyId, navigate, showToast]);

  // WebSocket 연결 관리
  useEffect(() => {
    if (!workspace || !currentUser?.id || !currentUser?.nickname) return;

    const userId = typeof currentUser.id === 'string' ? parseInt(currentUser.id, 10) : currentUser.id;
    const nickname = currentUser.nickname;

    // WebSocket 핸들러 설정
    workspaceWebSocket.connect(workspace.id, userId, nickname, {
      onMessage: (event: WorkspaceWebSocketEvent) => {
        // 서버에서 받은 메시지를 채팅에 추가
        if (event.message) {
          const newMessage: MessageResponse = {
            id: event.message.id,
            workspaceId: workspace.id,
            content: event.message.content,
            messageType: event.message.messageType,
            createdAt: event.message.createdAt,
            updatedAt: null,
            author: {
              id: event.message.userId,
              nickname: event.message.nickname,
              profileImageUrl: event.message.profileImageUrl || null,
            },
          };

          // 중복 메시지 방지 (같은 ID 메시지가 이미 있는지 확인)
          setMessages((prev) => {
            const exists = prev.some((m) => m.id === newMessage.id);
            if (exists) return prev;
            return [...prev, newMessage];
          });
        }
      },
      onJoin: (event: WorkspaceWebSocketEvent) => {
        // 사용자 입장 시 멤버 온라인 상태 업데이트
        if (event.senderId) {
          setMembers((prev) =>
            prev.map((m) =>
              m.id === event.senderId ? { ...m, isOnline: true } : m
            )
          );
        }
      },
      onLeave: (event: WorkspaceWebSocketEvent) => {
        // 사용자 퇴장 시 멤버 오프라인 상태 업데이트
        if (event.senderId) {
          setMembers((prev) =>
            prev.map((m) =>
              m.id === event.senderId ? { ...m, isOnline: false } : m
            )
          );
        }
      },
      onDelete: (event: WorkspaceWebSocketEvent) => {
        // 메시지 삭제 시
        if (event.messageId) {
          setMessages((prev) => prev.filter((m) => m.id !== event.messageId));
        }
      },
      onUpdate: (event: WorkspaceWebSocketEvent) => {
        // 메시지 수정 시
        if (event.message && event.messageId) {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === event.messageId
                ? { ...m, content: event.message!.content }
                : m
            )
          );
        }
      },
      onConnectionChange: (status) => {
        setIsWebSocketConnected(status === 'CONNECTED');
        if (status === 'CONNECTED') {
          console.log('[Workspace] WebSocket 연결 완료');
        }
      },
      onError: (errorMessage) => {
        console.error('[Workspace] WebSocket 에러:', errorMessage);
      },
    });

    // 컴포넌트 언마운트 시 연결 해제
    return () => {
      workspaceWebSocket.disconnect();
      setIsWebSocketConnected(false);
    };
  }, [workspace?.id, currentUser?.id, currentUser?.nickname]);

  // 세션 목록 로드
  const loadSessions = useCallback(async () => {
    if (!studyId) return;
    try {
      const data = await sessionApi.getSessions(studyId);
      setSessions(data);
    } catch {
      setSessions([]);
    }
  }, [studyId]);

  // 초기 세션 로드
  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  // 10초마다 현재 시간 갱신 (진행 중인 세션 재확인)
  useEffect(() => {
    // 초기 체크
    setCurrentTime(new Date());

    const interval = setInterval(() => {
      setCurrentTime(new Date());
    }, 10000); // 10초마다 체크
    return () => clearInterval(interval);
  }, []);

  // 5분마다 세션 목록 갱신 (신규 세션 반영)
  useEffect(() => {
    const interval = setInterval(() => {
      loadSessions();
    }, 300000); // 5분마다 세션 목록 갱신
    return () => clearInterval(interval);
  }, [loadSessions]);

  // 현재 진행 중인 세션 찾기
  const activeSession = useMemo(() => {
    // currentTime은 종속성 갱신을 위한 더미 참조
    void currentTime;
    const inProgress = sessions.filter(isSessionInProgress);
    if (inProgress.length === 0) return null;
    return inProgress.reduce((latest, session) => {
      const latestTime = new Date(latest.scheduledAt).getTime();
      const sessionTime = new Date(session.scheduledAt).getTime();
      return sessionTime > latestTime ? session : latest;
    }, inProgress[0]);
  }, [sessions, currentTime]);

  useEffect(() => {
    if (!studyId || !activeSession) {
      setActiveMeetingId(null);
      setActiveMeetingEnded(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const meetings = await meetingApi.listMeetings(studyId, { page: 0, size: 20 });
        const match = meetings.content.find((meeting) => meeting.session?.id === activeSession.id);
        if (!cancelled) {
          if (match) {
            const ended = Boolean(match.endedAt);
            setActiveMeetingEnded(ended);
            setActiveMeetingId(ended ? null : match.id);
          } else {
            setActiveMeetingEnded(null);
            setActiveMeetingId(null);
          }
        }
      } catch (error) {
        if (!cancelled) {
          setActiveMeetingId(null);
          setActiveMeetingEnded(null);
        }
        console.warn('Failed to resolve active meeting id', error);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeSession, studyId]);

  // 다크모드 토글
  const handleToggleDarkMode = useCallback(() => {
    setIsDarkMode((prev) => !prev);
  }, []);

  // 메시지 전송 핸들러 (WebSocket 사용)
  const handleSendMessage = useCallback(
    (content: string) => {
      if (!workspace) {
        return;
      }

      // WebSocket 연결 상태 확인
      if (isWebSocketConnected) {
        // WebSocket으로 메시지 전송
        workspaceWebSocket.sendMessage(content, 'TEXT');
      } else {
        // WebSocket 미연결 시 REST API 대체
        workspaceApi
          .sendMessage(workspace.id, { content, messageType: 'TEXT' })
          .then((newMessage) => {
            setMessages((prev) => [...prev, newMessage]);
          })
          .catch(() => {
            showToast?.('메시지 전송에 실패했습니다.', 'error');
          });
      }
    },
    [workspace, isWebSocketConnected, showToast]
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
      // 이전 메시지를 위에 추가
      setMessages((prev) => [...messagesData.content.reverse(), ...prev]);
      setHasMoreMessages(!messagesData.last);
      setCurrentPage(nextPage);
    } catch (err) {
      // 이전 메시지 로드 실패는 무시
    } finally {
      setIsMessagesLoading(false);
    }
  }, [workspace, isMessagesLoading, hasMoreMessages, currentPage]);

  // 뒤로가기 (대시보드로 이동, 전환 애니메이션 포함)
  const handleGoBack = useCallback(() => {
    // 퇴장 애니메이션 시작
    setIsExiting(true);

    // 애니메이션 완료 후 네비게이션 (500ms)
    setTimeout(() => {
      navigate('/dashboard');
    }, 500);
  }, [navigate]);

  // 미팅으로 이동 (전환 애니메이션 포함)
  const handleNavigateToMeeting = useCallback(() => {
    if (!studyId) return;

    // 퇴장 애니메이션 시작
    setIsExiting(true);

    // 미팅 페이지에서 진입 애니메이션을 위한 플래그 설정
    sessionStorage.setItem('fromWorkspace', 'true');

    // 애니메이션 완료 후 네비게이션 (500ms)
    setTimeout(() => {
      if (activeMeetingId) {
        navigate(`/study/${studyId}/meetings/${activeMeetingId}/room`);
      } else {
        navigate(`/study/${studyId}/meetings`);
      }
    }, 500);
  }, [navigate, studyId, activeMeetingId]);

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
          <span className="workspace-error__icon">!</span>
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
    <div className={cn(
      'workspace-container',
      isDarkMode && 'workspace-container--dark',
      isExiting && 'workspace-container--exiting',
      isEnteringFromMeeting && 'workspace-container--entering',
      isEnteringFromDashboard && 'workspace-container--entering-from-dashboard'
    )}>
      {/* 메인 콘텐츠 영역 */}
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
            activeSession={activeSession && activeMeetingEnded !== true ? activeSession : null}
            onNavigateToMeeting={handleNavigateToMeeting}
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
                  currentUserId={currentUser?.id}
                />
                <MessageInput
                  onSend={handleSendMessage}
                  placeholder={`${studyName}에 메시지 보내기`}
                />
              </>
            )}

            {activeMenu === 'materials' && studyId && <MaterialArea studyId={studyId} />}

            {activeMenu === 'calendar' && studyId && (
              <WorkspaceCalendarArea studyId={studyId} isLeader={isLeader} onSessionChange={loadSessions} />
            )}

            {activeMenu === 'meeting' && studyId && (
              <MeetingHistoryPanel studyId={studyId} />
            )}
          </div>
        </div>
      </div>

      {/* 멤버 목록 사이드바 */}
      <MemberList
        members={members}
        isVisible={isMembersVisible}
        onMemberClick={() => {
          // TODO: 멤버 클릭 프로필 보기 기능 추가
        }}
      />
    </div>
  );
};







