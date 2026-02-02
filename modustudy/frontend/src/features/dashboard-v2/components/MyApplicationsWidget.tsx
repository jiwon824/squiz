// 내가 신청한 스터디 위젯
// 대시보드에서 내 스터디 신청 내역을 표시

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Send, Search, ChevronRight, RefreshCw, Clock, CheckCircle2, XCircle, Compass, Loader2, Maximize2, Play, AlertCircle } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { studyApi } from '@/api/endpoints/studyApi';

// 신청 상태별 뱃지 스타일 및 아이콘
const APPLICATION_STATUS: Record<string, { label: string; className: string; dot: string; icon: React.ElementType }> = {
  PENDING: {
    label: '대기중',
    className: 'bg-amber-50 text-amber-600 ring-1 ring-amber-200',
    dot: 'bg-amber-500 animate-pulse',
    icon: Clock,
  },
  APPROVED: {
    label: '승인',
    className: 'bg-emerald-50 text-emerald-600 ring-1 ring-emerald-200',
    dot: 'bg-emerald-500',
    icon: CheckCircle2,
  },
  REJECTED: {
    label: '거절',
    className: 'bg-red-50 text-red-400 ring-1 ring-red-200',
    dot: 'bg-red-400',
    icon: XCircle,
  },
};

const getApplicationBadge = (status: string) => {
  return APPLICATION_STATUS[status] || {
    label: status,
    className: 'bg-gray-50 text-gray-500 ring-1 ring-gray-200',
    dot: 'bg-gray-400',
    icon: Clock,
  };
};

// 대시보드에 표시할 최대 개수
const MAX_DISPLAY_COUNT = 2;

interface ApplicationItem {
  applicationId: number;
  studyId: number;
  studyName?: string;
  status: string;
  message?: string;
  createdAt: string;
}

export const MyApplicationsWidget: React.FC = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<ApplicationItem[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchApplications = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await studyApi.getMyApplications(undefined, 0, 20);
      // studyApi.getMyApplications는 response.data를 반환 (Spring Page 객체)
      const page = (response as any)?.content ? response : (response as any)?.data || response;
      const content: ApplicationItem[] = page?.content || [];

      setTotalCount(content.length);
      // 최신순 2개만 표시
      setApplications(content.slice(0, MAX_DISPLAY_COUNT));
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

  return (
    <div className="bg-gradient-to-br from-white to-violet-50/30 rounded-2xl p-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] relative overflow-hidden">
      {/* 배경 장식 */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-violet-100/20 rounded-full -translate-y-8 translate-x-8" />
      <div className="absolute bottom-0 left-0 w-16 h-16 bg-violet-100/15 rounded-full translate-y-6 -translate-x-6" />

      {/* 헤더 - WidgetHeader 스타일 통일 */}
      <div className="flex items-center justify-between mb-5 relative">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-violet-100 flex items-center justify-center flex-shrink-0">
            <Send size={20} className="text-violet-600" />
          </div>
          <div className="h-10 flex flex-col justify-center">
            <h3 className="text-lg font-bold text-text-primary leading-6 mb-0">신청한 스터디</h3>
            <p className="text-xs text-text-tertiary leading-4 mb-0">신청 현황 및 승인 상태</p>
          </div>
        </div>
        <button
          onClick={() => navigate('/my-studies/applications')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          title="전체 화면으로 보기"
        >
          <Maximize2 size={18} className="text-text-secondary" />
        </button>
      </div>

      {/* 로딩 상태 */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-8">
          <Loader2 size={24} className="text-violet-400 animate-spin mb-2" />
          <p className="text-xs text-gray-400">불러오는 중...</p>
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
        <div className="text-center py-12">
          <Compass className="mx-auto text-gray-300 mb-4" size={48} />
          <p className="text-text-secondary">신청한 스터디가 없어요</p>
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

      {/* 신청 목록 */}
      {!loading && !error && applications.length > 0 && (
        <div className="space-y-2 relative">
          <ul className="space-y-2">
            <AnimatePresence>
              {applications.map((app, idx) => {
                const badge = getApplicationBadge(app.status);
                const StatusIcon = badge.icon;
                return (
                  <motion.li
                    key={app.applicationId}
                    initial={{ opacity: 0, x: -8 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -8 }}
                    transition={{ delay: idx * 0.05 }}
                  >
                    <div
                      className={cn(
                        'w-full flex items-center gap-3 text-left group',
                        'rounded-xl p-3 transition-all duration-200',
                        'hover:bg-white hover:shadow-sm border border-transparent hover:border-violet-100'
                      )}
                    >
                      {/* 상태 아이콘 */}
                      <button
                        onClick={() => navigate(`/study/${app.studyId}`)}
                        className={cn(
                          'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 transition-colors',
                          app.status === 'PENDING' && 'bg-amber-50 group-hover:bg-amber-100',
                          app.status === 'APPROVED' && 'bg-emerald-50 group-hover:bg-emerald-100',
                          app.status === 'REJECTED' && 'bg-red-50 group-hover:bg-red-100',
                          !['PENDING', 'APPROVED', 'REJECTED'].includes(app.status) && 'bg-gray-50 group-hover:bg-gray-100',
                        )}
                        title="스터디 상세 보기"
                      >
                        <StatusIcon size={14} className={cn(
                          app.status === 'PENDING' && 'text-amber-500',
                          app.status === 'APPROVED' && 'text-emerald-500',
                          app.status === 'REJECTED' && 'text-red-400',
                          !['PENDING', 'APPROVED', 'REJECTED'].includes(app.status) && 'text-gray-400',
                        )} />
                      </button>

                      {/* 신청 정보 */}
                      <button
                        onClick={() => navigate(`/study/${app.studyId}`)}
                        className="flex-1 min-w-0 text-left"
                        title="스터디 상세 보기"
                      >
                        <p className="text-sm font-semibold text-gray-800 truncate group-hover:text-violet-700 transition-colors">
                          {app.studyName || `스터디 #${app.studyId}`}
                        </p>
                        <p className="text-[11px] text-gray-400 mt-0.5">
                          {formatDate(app.createdAt)} 신청
                        </p>
                      </button>

                      {/* 상태 뱃지 */}
                      <span className={cn(
                        'text-[10px] font-bold px-2.5 py-1 rounded-full flex-shrink-0 flex items-center gap-1',
                        badge.className
                      )}>
                        <span className={cn('w-1.5 h-1.5 rounded-full', badge.dot)} />
                        {badge.label}
                      </span>

                      {/* 워크스페이스 이동 버튼 (승인된 경우에만) */}
                      {app.status === 'APPROVED' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/study/${app.studyId}/workspace`);
                          }}
                          className={cn(
                            'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0',
                            'bg-emerald-500 hover:bg-emerald-600 transition-colors',
                            'shadow-sm shadow-emerald-200'
                          )}
                          title="워크스페이스로 이동"
                        >
                          <Play size={14} className="text-white ml-0.5" fill="white" />
                        </button>
                      )}
                    </div>
                  </motion.li>
                );
              })}
            </AnimatePresence>
          </ul>

          {/* 더보기 버튼 (2개 초과 시) */}
          {totalCount > MAX_DISPLAY_COUNT && (
            <button
              onClick={() => navigate('/my-studies/applications')}
              className="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 rounded-xl bg-violet-50 hover:bg-violet-100 transition-colors text-violet-600 text-sm font-medium"
            >
              <span>외 {totalCount - MAX_DISPLAY_COUNT}개 더보기</span>
              <ChevronRight size={16} />
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default MyApplicationsWidget;
