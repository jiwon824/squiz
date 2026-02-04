// 내가 개설한 스터디 위젯
// 대시보드에서 내가 리더로 있는 스터디 목록을 표시

import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Crown, Users, ChevronRight, RefreshCw, Megaphone, Maximize2, AlertCircle } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { studyApi } from '@/api/endpoints/studyApi';
import { useAuthStore } from '@/store/authStore';

// 상태별 뱃지 스타일
const STATUS_BADGE: Record<string, { label: string; className: string; dot: string }> = {
  RECRUITING: { label: '모집', className: 'bg-blue-50 text-blue-600', dot: 'bg-blue-500' },
  RECRUIT_CLOSED: { label: '마감', className: 'bg-gray-50 text-gray-500', dot: 'bg-gray-400' },
  IN_PROGRESS: { label: '진행', className: 'bg-emerald-50 text-emerald-600', dot: 'bg-emerald-500' },
  COMPLETED: { label: '완료', className: 'bg-gray-50 text-gray-400', dot: 'bg-gray-300' },
  SCHEDULED: { label: '예정', className: 'bg-violet-50 text-violet-600', dot: 'bg-violet-500' },
  CANCELLED: { label: '취소', className: 'bg-red-50 text-red-400', dot: 'bg-red-300' },
  PENDING: { label: '대기', className: 'bg-amber-50 text-amber-600', dot: 'bg-amber-500' },
  DRAFT: { label: '임시', className: 'bg-gray-50 text-gray-400', dot: 'bg-gray-300' },
};

const getStatusBadge = (status: string) => {
  return STATUS_BADGE[status] || { label: status, className: 'bg-gray-50 text-gray-500 ring-1 ring-gray-200', dot: 'bg-gray-400' };
};

interface StudyItem {
  id: number;
  name: string;
  status: string;
  studyType?: string;
  maxMembers?: number;
  topic?: { id: number; name: string; icon?: string | null };
  leader?: { id: number; nickname?: string; profileImage?: string | null };
}

// 대시보드에 표시할 최대 개수
const MAX_DISPLAY_COUNT = 3;

export const MyCreatedStudiesWidget: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [studies, setStudies] = useState<StudyItem[]>([]);
  const [allStudies, setAllStudies] = useState<StudyItem[]>([]); // 전체 목록 (카운트용)
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

  const fetchStudies = async () => {
    if (!user?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(false);
    try {
      const response = await studyApi.getMyStudies(0, 20);
      // studyApi.getMyStudies는 response.data를 반환 (Spring Page 객체)
      const page = (response as any)?.content ? response : (response as any)?.data || response;
      const content: StudyItem[] = page?.content || [];

      // 내가 리더인 스터디만 필터 (타입 안전 비교)
      const myCreated = content.filter((s) => {
        const leaderId = s.leader?.id;
        return leaderId != null && Number(leaderId) === Number(user.id);
      });

      setTotalCount(myCreated.length);
      setAllStudies(myCreated); // 전체 목록 저장 (카운트용)

      // 정렬: 진행중 > 모집중 > 대기 > 모집마감 > 완료 > 기타
      const statusOrder: Record<string, number> = {
        IN_PROGRESS: 0,
        RECRUITING: 1,
        PENDING: 2,
        RECRUIT_CLOSED: 3,
        COMPLETED: 4,
      };
      const sorted = [...myCreated].sort((a, b) => {
        const orderA = statusOrder[a.status] ?? 99;
        const orderB = statusOrder[b.status] ?? 99;
        return orderA - orderB;
      });

      // 정렬된 상위 3개만 표시
      setStudies(sorted.slice(0, MAX_DISPLAY_COUNT));
    } catch (err) {
      console.error('[MyCreatedStudiesWidget] 조회 실패:', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  // 상태별 카운트
  const statusCounts = {
    PENDING: allStudies.filter((s) => s.status === 'PENDING').length,
    RECRUITING: allStudies.filter((s) => s.status === 'RECRUITING').length,
    IN_PROGRESS: allStudies.filter((s) => s.status === 'IN_PROGRESS').length,
    COMPLETED: allStudies.filter((s) => s.status === 'COMPLETED').length,
  };

  useEffect(() => {
    fetchStudies();
  }, [user?.id]);

  // 위젯 내용 컴포넌트
  const WidgetContent = () => (
    <>
      {/* 배경 장식 */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-blue-100/20 rounded-full -translate-y-8 translate-x-8" />
      <div className="absolute bottom-0 left-0 w-16 h-16 bg-blue-100/15 rounded-full translate-y-6 -translate-x-6" />

      {/* 헤더 - WidgetHeader 스타일 통일 */}
      <div className="flex items-center justify-between mb-5 relative">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center flex-shrink-0">
            <Crown size={20} className="text-blue-600" />
          </div>
          <div className="h-10 flex flex-col justify-center">
            <h3 className="text-lg font-bold text-text-primary leading-6 mb-0">개설한 스터디</h3>
            <p className="text-xs text-text-tertiary leading-4 mb-0">리더로 운영 중인 스터디</p>
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
      {!loading && !error && allStudies.length > 0 && (
        <div className="flex items-center gap-2 mb-4 flex-wrap">
          {statusCounts.PENDING > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-amber-50 text-amber-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
              대기 {statusCounts.PENDING}
            </span>
          )}
          {statusCounts.RECRUITING > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-blue-50 text-blue-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse" />
              모집 {statusCounts.RECRUITING}
            </span>
          )}
          {statusCounts.IN_PROGRESS > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-emerald-50 text-emerald-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
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
            onClick={fetchStudies}
            className="inline-flex items-center gap-1.5 mt-3 px-4 py-2 text-sm font-medium text-text-tertiary hover:text-text-secondary transition-colors"
          >
            <RefreshCw size={14} />
            다시 시도
          </button>
        </div>
      )}

      {/* 빈 상태 */}
      {!loading && !error && studies.length === 0 && (
        <div className="text-center py-4">
          <Megaphone className="mx-auto text-gray-300 mb-4" size={48} />
          <p className="text-text-secondary">개설한 스터디가 없어요</p>
          <p className="text-sm text-text-tertiary mt-1">스터디를 만들고 함께 성장할 멤버를 모아보세요</p>
          <button
            onClick={() => navigate('/study')}
            className="inline-flex items-center gap-1 mt-4 text-sm font-medium text-text-tertiary hover:text-text-secondary transition-colors"
          >
            스터디 탐색하기
            <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* 스터디 목록 */}
      {!loading && !error && studies.length > 0 && (
        <div className="space-y-2 relative">
          <ul className="space-y-2 min-h-[120px]">
            <AnimatePresence>
              {studies.map((study, idx) => {
                const badge = getStatusBadge(study.status);
                // 진행중/완료 상태면 워크스페이스로, 그 외는 상세 페이지로
                const canGoWorkspace = study.status === 'IN_PROGRESS' || study.status === 'COMPLETED';
                const handleClick = () => {
                  if (canGoWorkspace) {
                    navigate(`/study/${study.id}/workspace`);
                  } else {
                    navigate(`/study/${study.id}`);
                  }
                };

                // 상태별 아이콘 배경색
                const getIconStyle = () => {
                  switch (study.status) {
                    case 'IN_PROGRESS':
                      return { bg: 'bg-emerald-50 group-hover:bg-emerald-100', icon: 'text-emerald-500' };
                    case 'RECRUITING':
                      return { bg: 'bg-blue-50 group-hover:bg-blue-100', icon: 'text-blue-500' };
                    case 'PENDING':
                      return { bg: 'bg-amber-50 group-hover:bg-amber-100', icon: 'text-amber-500' };
                    case 'COMPLETED':
                      return { bg: 'bg-gray-50 group-hover:bg-gray-100', icon: 'text-gray-400' };
                    default:
                      return { bg: 'bg-gray-50 group-hover:bg-gray-100', icon: 'text-gray-400' };
                  }
                };
                const iconStyle = getIconStyle();

                return (
                  <motion.li
                    key={study.id}
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
                        'hover:bg-white hover:shadow-sm border border-transparent hover:border-blue-100'
                      )}
                      title={canGoWorkspace ? '워크스페이스로 이동' : '스터디 상세 보기'}
                    >
                      {/* 주제 아이콘 또는 인덱스 */}
                      <div className={cn('w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 transition-colors', iconStyle.bg)}>
                        {study.topic?.icon ? (
                          <span className="text-sm">{study.topic.icon}</span>
                        ) : (
                          <Users size={14} className={iconStyle.icon} />
                        )}
                      </div>

                      {/* 스터디 정보 */}
                      <div className="flex-1 min-w-0 text-left flex flex-col justify-center">
                        <p className="text-sm font-semibold text-gray-800 truncate group-hover:text-blue-700 transition-colors leading-none mb-0">
                          {study.name}
                        </p>
                        {study.topic && (
                          <p className="text-[11px] text-gray-400 truncate leading-none mt-0.5 mb-0">{study.topic.name}</p>
                        )}
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
              className="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 rounded-xl bg-blue-50 hover:bg-blue-100 transition-colors text-blue-600 text-sm font-medium"
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
          'bg-gradient-to-br from-white to-blue-50/30 rounded-2xl p-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] relative overflow-hidden h-full',
          isExpanding && 'invisible'
        )}
      >
        <WidgetContent />
      </div>

      {/* 확대 애니메이션 오버레이 */}
      <AnimatePresence>
        {isExpanding && initialRect && targetRect && (
          <motion.div
            className="fixed bg-gradient-to-br from-white to-blue-50/30 p-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] overflow-hidden"
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
              navigate('/my-studies/created');
            }}
          >
            <WidgetContent />
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default MyCreatedStudiesWidget;
