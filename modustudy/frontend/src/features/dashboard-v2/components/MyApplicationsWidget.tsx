// 내 참여 스터디 위젯
// 대시보드에서 참여 중인 스터디 표시 (승인 대기 + 진행중 + 완료)

import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Users, ChevronRight, RefreshCw, Clock, CheckCircle2, Compass, Maximize2, Play, AlertCircle } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { studyApi } from '@/api/endpoints/studyApi';

// 통합 상태 타입: 신청 대기 / 승인됨(스터디 상태로 분기) / 진행중 / 완료
type CombinedStatus = 'PENDING' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED';

// 통합 상태별 뱃지 스타일 및 아이콘
const COMBINED_STATUS_BADGE: Record<CombinedStatus, { label: string; className: string; dot: string; icon: React.ElementType }> = {
  PENDING: {
    label: '대기',
    className: 'bg-amber-50 text-amber-600',
    dot: 'bg-amber-500 animate-pulse',
    icon: Clock,
  },
  APPROVED: {
    label: '승인',
    className: 'bg-emerald-50 text-emerald-600',
    dot: 'bg-emerald-500',
    icon: CheckCircle2,
  },
  IN_PROGRESS: {
    label: '진행',
    className: 'bg-blue-50 text-blue-600',
    dot: 'bg-blue-500',
    icon: Play,
  },
  COMPLETED: {
    label: '완료',
    className: 'bg-gray-50 text-gray-500',
    dot: 'bg-gray-400',
    icon: CheckCircle2,
  },
};

const getCombinedStatusBadge = (status: CombinedStatus) => {
  return COMBINED_STATUS_BADGE[status] || {
    label: status,
    className: 'bg-gray-50 text-gray-500 ring-1 ring-gray-200',
    dot: 'bg-gray-400',
    icon: Clock,
  };
};

// 대시보드에 표시할 최대 개수
const MAX_DISPLAY_COUNT = 3;

interface ApplicationItem {
  applicationId: number;
  studyId: number;
  studyName?: string;
  status: string;
  message?: string;
  createdAt: string;
  // 스터디 상세 정보 (승인된 경우)
  studyStatus?: string;
  topicIcon?: string;
  topicName?: string;
}

// 통합 상태 계산: 신청 상태 + 스터디 상태
const getCombinedStatus = (app: ApplicationItem): CombinedStatus => {
  if (app.status === 'PENDING') return 'PENDING';
  if (app.status === 'APPROVED') {
    // 승인된 경우 스터디 상태에 따라 분기
    if (app.studyStatus === 'IN_PROGRESS') return 'IN_PROGRESS';
    if (app.studyStatus === 'COMPLETED') return 'COMPLETED';
    return 'APPROVED'; // 스터디가 아직 시작 안 됨
  }
  return 'PENDING';
};

export const MyApplicationsWidget: React.FC = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<ApplicationItem[]>([]);
  const [allApplications, setAllApplications] = useState<ApplicationItem[]>([]); // 전체 목록 (카운트용)
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  // 확대 애니메이션 상태
  const [isExpanding, setIsExpanding] = useState(false);
  const [initialRect, setInitialRect] = useState<DOMRect | null>(null);
  const [targetRect, setTargetRect] = useState<DOMRect | null>(null);
  const widgetRef = useRef<HTMLDivElement>(null);

  // main content 영역 내에서만 확대 후 페이지 이동
  const handleExpandToPage = () => {
    if (widgetRef.current) {
      const rect = widgetRef.current.getBoundingClientRect();
      setInitialRect(rect);

      // UserLayout의 main content 영역 bounds 가져오기
      const mainContent = document.getElementById('main-content-scroll');
      if (mainContent) {
        const mainRect = mainContent.getBoundingClientRect();
        setTargetRect(mainRect);
      }

      setIsExpanding(true);
    }
  };

  const fetchApplications = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await studyApi.getMyApplications(undefined, 0, 50);
      const page = (response as any)?.content ? response : (response as any)?.data || response;
      const content: ApplicationItem[] = page?.content || [];

      // REJECTED 제외
      const filtered = content.filter((app) => app.status !== 'REJECTED');

      // 승인된 스터디들의 상세 정보 조회
      const approvedApps = filtered.filter((app) => app.status === 'APPROVED');
      const studyDetailsPromises = approvedApps.map(async (app) => {
        try {
          const studyDetail = await studyApi.getStudyDetail(app.studyId);
          const study = (studyDetail as any)?.data || studyDetail;
          return {
            studyId: app.studyId,
            studyStatus: study?.status,
            topicIcon: study?.topic?.icon,
            topicName: study?.topic?.name,
          };
        } catch {
          return { studyId: app.studyId };
        }
      });

      const studyDetails = await Promise.all(studyDetailsPromises);
      const studyDetailMap = new Map(studyDetails.map((d) => [d.studyId, d]));

      // 스터디 상세 정보 병합
      const enrichedApps = filtered.map((app) => {
        if (app.status === 'APPROVED') {
          const detail = studyDetailMap.get(app.studyId);
          return {
            ...app,
            studyStatus: detail?.studyStatus,
            topicIcon: detail?.topicIcon,
            topicName: detail?.topicName,
          };
        }
        return app;
      });

      // 정렬: 진행중 > 승인 대기 > 승인됨 > 완료
      const statusOrder: Record<CombinedStatus, number> = {
        IN_PROGRESS: 0,
        PENDING: 1,
        APPROVED: 2,
        COMPLETED: 3,
      };
      enrichedApps.sort((a, b) => {
        const statusA = getCombinedStatus(a);
        const statusB = getCombinedStatus(b);
        return statusOrder[statusA] - statusOrder[statusB];
      });

      setTotalCount(enrichedApps.length);
      setAllApplications(enrichedApps); // 전체 목록 저장 (카운트용)
      setApplications(enrichedApps.slice(0, MAX_DISPLAY_COUNT));
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  // 날짜 포맷: "1월 30일" 형태
  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return `${date.getMonth() + 1}월 ${date.getDate()}일`;
    } catch {
      return '';
    }
  };

  // 상태별 카운트
  const statusCounts = {
    PENDING: allApplications.filter((app) => getCombinedStatus(app) === 'PENDING').length,
    APPROVED: allApplications.filter((app) => getCombinedStatus(app) === 'APPROVED').length,
    IN_PROGRESS: allApplications.filter((app) => getCombinedStatus(app) === 'IN_PROGRESS').length,
    COMPLETED: allApplications.filter((app) => getCombinedStatus(app) === 'COMPLETED').length,
  };

  // 위젯 내용 컴포넌트
  const WidgetContent = () => (
    <>
      {/* 배경 장식 */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-violet-100/20 rounded-full -translate-y-8 translate-x-8" />
      <div className="absolute bottom-0 left-0 w-16 h-16 bg-violet-100/15 rounded-full translate-y-6 -translate-x-6" />

      {/* 헤더 - WidgetHeader 스타일 통일 */}
      <div className="flex items-center justify-between mb-5 relative">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-violet-100 flex items-center justify-center flex-shrink-0">
            <Users size={20} className="text-violet-600" />
          </div>
          <div className="h-10 flex flex-col justify-center">
            <h3 className="text-lg font-bold text-text-primary leading-6 mb-0">내 참여 스터디</h3>
            <p className="text-xs text-text-tertiary leading-4 mb-0">참여 현황 및 진행 상태</p>
          </div>
        </div>
        <button
          onClick={handleExpandToPage}
          className="p-2 hover:bg-gray-100 rounded-lg transition-all active:scale-125 hover:scale-110"
          title="전체 화면으로 보기"
        >
          <Maximize2 size={18} className="text-text-secondary" />
        </button>
      </div>

      {/* 상태별 카운트 요약 */}
      {!loading && !error && allApplications.length > 0 && (
        <div className="flex items-center gap-2 mb-4 flex-wrap">
          {statusCounts.PENDING > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-amber-50 text-amber-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
              대기 {statusCounts.PENDING}
            </span>
          )}
          {statusCounts.APPROVED > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-emerald-50 text-emerald-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              승인 {statusCounts.APPROVED}
            </span>
          )}
          {statusCounts.IN_PROGRESS > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-blue-50 text-blue-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />
              진행 {statusCounts.IN_PROGRESS}
            </span>
          )}
          {statusCounts.COMPLETED > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-gray-50 text-gray-500 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-gray-400" />
              완료 {statusCounts.COMPLETED}
            </span>
          )}
        </div>
      )}

      {/* 로딩 상태 */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-8">
          <Spinner variant="center" size="md" label="불러오는 중..." />
        </div>
      )}

      {/* 에러 상태 */}
      {!loading && error && (
        <div className="text-center py-12">
          <AlertCircle className="mx-auto text-gray-300 mb-4" size={48} />
          <p className="text-text-secondary">불러오지 못했어요</p>
          <p className="text-sm text-text-tertiary mt-1">네트워크 상태를 확인해주세요</p>
          <button
            onClick={fetchApplications}
            className="inline-flex items-center gap-1.5 mt-3 px-4 py-2 text-sm font-medium text-text-tertiary hover:text-text-secondary transition-colors"
          >
            <RefreshCw size={14} />
            다시 시도
          </button>
        </div>
      )}

      {/* 빈 상태 */}
      {!loading && !error && applications.length === 0 && (
        <div className="text-center py-4">
          <Compass className="mx-auto text-gray-300 mb-4" size={48} />
          <p className="text-text-secondary">참여 중인 스터디가 없어요</p>
          <p className="text-sm text-text-tertiary mt-1">관심 있는 스터디를 찾아 참여해보세요</p>
          <button
            onClick={() => navigate('/study')}
            className="inline-flex items-center gap-1 mt-4 text-sm font-medium text-text-tertiary hover:text-text-secondary transition-colors"
          >
            스터디 탐색하기
            <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* 참여 스터디 목록 */}
      {!loading && !error && applications.length > 0 && (
        <div className="space-y-2 relative">
          <ul className="space-y-2 min-h-[120px]">
            <AnimatePresence>
              {applications.map((app, idx) => {
                const combinedStatus = getCombinedStatus(app);
                const badge = getCombinedStatusBadge(combinedStatus);
                const StatusIcon = badge.icon;
                const canGoWorkspace = combinedStatus === 'IN_PROGRESS' || combinedStatus === 'COMPLETED';

                // 진행중/완료 상태면 워크스페이스로, 그 외는 상세 페이지로
                const handleClick = () => {
                  if (canGoWorkspace) {
                    navigate(`/study/${app.studyId}/workspace`);
                  } else {
                    navigate(`/study/${app.studyId}`);
                  }
                };

                return (
                  <motion.li
                    key={app.applicationId}
                    initial={{ opacity: 0, x: -8 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -8 }}
                    transition={{ delay: idx * 0.05 }}
                  >
                    <button
                      onClick={handleClick}
                      className={cn(
                        'w-full flex items-center gap-3 text-left group',
                        'rounded-xl p-3 transition-all duration-200',
                        'hover:bg-white hover:shadow-sm border border-transparent hover:border-violet-100'
                      )}
                      title={canGoWorkspace ? '워크스페이스로 이동' : '스터디 상세 보기'}
                    >
                      {/* 주제 아이콘 또는 상태 아이콘 */}
                      <div
                        className={cn(
                          'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 transition-colors',
                          combinedStatus === 'PENDING' && 'bg-amber-50 group-hover:bg-amber-100',
                          combinedStatus === 'APPROVED' && 'bg-emerald-50 group-hover:bg-emerald-100',
                          combinedStatus === 'IN_PROGRESS' && 'bg-blue-50 group-hover:bg-blue-100',
                          combinedStatus === 'COMPLETED' && 'bg-gray-50 group-hover:bg-gray-100',
                        )}
                      >
                        {app.topicIcon ? (
                          <span className="text-sm">{app.topicIcon}</span>
                        ) : (
                          <StatusIcon size={14} className={cn(
                            combinedStatus === 'PENDING' && 'text-amber-500',
                            combinedStatus === 'APPROVED' && 'text-emerald-500',
                            combinedStatus === 'IN_PROGRESS' && 'text-blue-500',
                            combinedStatus === 'COMPLETED' && 'text-gray-400',
                          )} />
                        )}
                      </div>

                      {/* 스터디 정보 */}
                      <div className="flex-1 min-w-0 text-left flex flex-col justify-center">
                        <p className="text-sm font-semibold text-gray-800 truncate group-hover:text-violet-700 transition-colors leading-none mb-0">
                          {app.studyName || `스터디 #${app.studyId}`}
                        </p>
                        <p className="text-[11px] text-gray-400 leading-none mt-0.5 mb-0">
                          {combinedStatus === 'PENDING' ? '승인 대기 중입니다' : (app.topicName || formatDate(app.createdAt))}
                        </p>
                      </div>

                      {/* 상태 뱃지 */}
                      <span className={cn(
                        'text-[10px] font-bold px-2.5 py-1 rounded-full flex-shrink-0 flex items-center gap-1',
                        badge.className
                      )}>
                        <span className={cn('w-1.5 h-1.5 rounded-full', badge.dot)} />
                        {badge.label}
                      </span>

                    </button>
                  </motion.li>
                );
              })}
            </AnimatePresence>
          </ul>

          {/* 더보기 버튼 (3개 초과 시) */}
          {totalCount > MAX_DISPLAY_COUNT && (
            <button
              onClick={handleExpandToPage}
              className="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 rounded-xl bg-violet-50 hover:bg-violet-100 transition-colors text-violet-600 text-sm font-medium"
            >
              <span>외 {totalCount - MAX_DISPLAY_COUNT}개 더보기</span>
              <ChevronRight size={16} />
            </button>
          )}
        </div>
      )}
    </>
  );

  return (
    <>
      {/* 기본 위젯 */}
      <div
        ref={widgetRef}
        className={cn(
          'bg-gradient-to-br from-white to-violet-50/30 rounded-2xl p-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] relative overflow-hidden h-full',
          isExpanding && 'invisible'
        )}
      >
        <WidgetContent />
      </div>

      {/* 확대 애니메이션 오버레이 */}
      <AnimatePresence>
        {isExpanding && initialRect && targetRect && (
          <motion.div
            className="fixed bg-gradient-to-br from-white to-violet-50/30 p-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] overflow-hidden"
            style={{ zIndex: 9999 }}
            initial={{
              top: initialRect.top,
              left: initialRect.left,
              width: initialRect.width,
              height: initialRect.height,
              borderRadius: 16,
            }}
            animate={{
              top: targetRect.top,
              left: targetRect.left,
              width: targetRect.width,
              height: targetRect.height,
              borderRadius: 24, // main content의 rounded-3xl과 일치
            }}
            transition={{
              duration: 0.4,
              ease: [0.4, 0, 0.2, 1],
            }}
            onAnimationComplete={() => {
              navigate('/my-studies/applications');
            }}
          >
            <WidgetContent />
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default MyApplicationsWidget;
