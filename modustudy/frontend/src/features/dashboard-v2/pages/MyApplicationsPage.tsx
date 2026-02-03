// 신청한 스터디 전체 페이지
// 대시보드 위젯의 풀사이즈 버전

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search, ChevronRight, RefreshCw,
  Clock, CheckCircle2, XCircle, Compass, Calendar
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
import { studyApi } from '@/api/endpoints/studyApi';

// 신청 상태별 스타일
const APPLICATION_STATUS: Record<string, { label: string; className: string; dot: string; icon: React.ElementType; cardBorder: string }> = {
  PENDING: {
    label: '대기중',
    className: 'bg-amber-50 text-amber-600 ring-1 ring-amber-200',
    dot: 'bg-amber-500 animate-pulse',
    icon: Clock,
    cardBorder: 'border-amber-100',
  },
  APPROVED: {
    label: '승인',
    className: 'bg-emerald-50 text-emerald-600 ring-1 ring-emerald-200',
    dot: 'bg-emerald-500',
    icon: CheckCircle2,
    cardBorder: 'border-emerald-100',
  },
  REJECTED: {
    label: '거절',
    className: 'bg-red-50 text-red-400 ring-1 ring-red-200',
    dot: 'bg-red-400',
    icon: XCircle,
    cardBorder: 'border-red-100',
  },
};

const getApplicationBadge = (status: string) => {
  return APPLICATION_STATUS[status] || {
    label: status,
    className: 'bg-gray-50 text-gray-500 ring-1 ring-gray-200',
    dot: 'bg-gray-400',
    icon: Clock,
    cardBorder: 'border-gray-100',
  };
};

interface ApplicationItem {
  applicationId: number;
  studyId: number;
  studyName?: string;
  status: string;
  message?: string;
  createdAt: string;
  processedAt?: string;
  rejectedReason?: string;
}

type FilterStatus = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';

export const MyApplicationsPage: React.FC = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<ApplicationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [filter, setFilter] = useState<FilterStatus>('ALL');

  const fetchApplications = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await studyApi.getMyApplications(undefined, 0, 100);
      const page = (response as any)?.content ? response : (response as any)?.data || response;
      const content: ApplicationItem[] = page?.content || [];
      setApplications(content);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const filteredApps = filter === 'ALL'
    ? applications
    : applications.filter((a) => a.status === filter);

  const filterCounts = {
    ALL: applications.length,
    PENDING: applications.filter((a) => a.status === 'PENDING').length,
    APPROVED: applications.filter((a) => a.status === 'APPROVED').length,
    REJECTED: applications.filter((a) => a.status === 'REJECTED').length,
  };

  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
    } catch {
      return '';
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] py-6 sm:py-8">
      <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8">
        {/* 브레드크럼 + 헤더 */}
        <PageNavHeader
          title="신청한 스터디"
          breadcrumbs={[
            { label: '대시보드', path: '/dashboard' },
            { label: '신청한 스터디' },
          ]}
          badge={{ text: `${applications.length}개`, className: 'bg-violet-50 text-violet-600' }}
        />

        {/* 필터 탭 */}
        <div className="flex flex-wrap items-center gap-2 mb-6">
          {([
            { key: 'ALL' as FilterStatus, label: '전체' },
            { key: 'PENDING' as FilterStatus, label: '대기중' },
            { key: 'APPROVED' as FilterStatus, label: '승인' },
            { key: 'REJECTED' as FilterStatus, label: '거절' },
          ]).map((tab) => (
            <button
              key={tab.key}
              onClick={() => setFilter(tab.key)}
              className={cn(
                'px-3 sm:px-4 py-2 rounded-xl text-sm font-medium transition-all',
                filter === tab.key
                  ? 'bg-violet-600 text-white shadow-sm'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              )}
            >
              {tab.label}
              <span className={cn(
                'ml-1.5 px-1.5 py-0.5 rounded-full text-xs',
                filter === tab.key ? 'bg-white/20' : 'bg-gray-200'
              )}>
                {filterCounts[tab.key]}
              </span>
            </button>
          ))}

          <div className="flex-1" />

          <button
            onClick={() => navigate('/study')}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-violet-600 text-white font-semibold text-sm hover:bg-violet-700 transition-colors shadow-sm"
          >
            <Search size={16} />
            <span className="hidden sm:inline">스터디 찾아보기</span>
            <span className="sm:hidden">탐색</span>
          </button>
        </div>

        {/* 로딩 */}
        {loading && (
          <div className="flex flex-col items-center justify-center py-20">
            <Spinner variant="center" label="불러오는 중..." />
          </div>
        )}

        {/* 에러 */}
        {!loading && error && (
          <div className="text-center py-20">
            <div className="w-16 h-16 mx-auto mb-4 bg-red-50 rounded-full flex items-center justify-center">
              <RefreshCw size={28} className="text-red-400" />
            </div>
            <p className="text-gray-500 text-lg mb-4">목록을 불러오지 못했습니다</p>
            <button
              onClick={fetchApplications}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-red-50 text-red-500 font-medium hover:bg-red-100 transition-colors"
            >
              <RefreshCw size={16} />
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!loading && !error && applications.length === 0 && (
          <div className="text-center py-20">
            <div className="w-20 h-20 mx-auto mb-5 bg-violet-50 rounded-2xl flex items-center justify-center">
              <Compass size={36} className="text-violet-300" />
            </div>
            <p className="text-gray-800 text-lg font-semibold mb-2">신청한 스터디가 없어요</p>
            <p className="text-gray-400 mb-6">관심 있는 스터디를 찾아 참여해보세요</p>
            <button
              onClick={() => navigate('/study')}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-violet-600 text-white font-semibold hover:bg-violet-700 transition-colors shadow-sm shadow-violet-200"
            >
              <Search size={18} />
              스터디 찾아보기
            </button>
          </div>
        )}

        {/* 필터 결과 없음 */}
        {!loading && !error && applications.length > 0 && filteredApps.length === 0 && (
          <div className="text-center py-16">
            <p className="text-gray-400">해당 상태의 신청 내역이 없습니다</p>
          </div>
        )}

        {/* 신청 목록 */}
        {!loading && !error && filteredApps.length > 0 && (
          <div className="space-y-3">
            <AnimatePresence>
              {filteredApps.map((app, idx) => {
                const badge = getApplicationBadge(app.status);
                const StatusIcon = badge.icon;
                return (
                  <motion.div
                    key={app.applicationId}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -12 }}
                    transition={{ delay: idx * 0.03 }}
                    onClick={() => navigate(`/study/${app.studyId}`)}
                    className={cn(
                      'bg-white rounded-2xl p-4 sm:p-5 border cursor-pointer',
                      'hover:shadow-lg hover:-translate-y-0.5',
                      'transition-all duration-200 group',
                      badge.cardBorder
                    )}
                  >
                    <div className="flex items-center gap-3 sm:gap-4">
                      {/* 상태 아이콘 */}
                      <div className={cn(
                        'w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center flex-shrink-0',
                        app.status === 'PENDING' && 'bg-amber-50',
                        app.status === 'APPROVED' && 'bg-emerald-50',
                        app.status === 'REJECTED' && 'bg-red-50',
                        !['PENDING', 'APPROVED', 'REJECTED'].includes(app.status) && 'bg-gray-50',
                      )}>
                        <StatusIcon size={22} className={cn(
                          app.status === 'PENDING' && 'text-amber-500',
                          app.status === 'APPROVED' && 'text-emerald-500',
                          app.status === 'REJECTED' && 'text-red-400',
                          !['PENDING', 'APPROVED', 'REJECTED'].includes(app.status) && 'text-gray-400',
                        )} />
                      </div>

                      {/* 정보 */}
                      <div className="flex-1 min-w-0">
                        <h3 className="font-bold text-gray-900 group-hover:text-violet-700 transition-colors mb-1">
                          {app.studyName || `스터디 #${app.studyId}`}
                        </h3>
                        <div className="flex items-center gap-4 text-xs text-gray-400">
                          <span className="flex items-center gap-1">
                            <Calendar size={12} />
                            {formatDate(app.createdAt)} 신청
                          </span>
                          {app.processedAt && (
                            <span className="flex items-center gap-1">
                              <CheckCircle2 size={12} />
                              {formatDate(app.processedAt)} 처리
                            </span>
                          )}
                        </div>
                        {app.message && (
                          <p className="text-sm text-gray-500 mt-2 line-clamp-1">
                            "{app.message}"
                          </p>
                        )}
                        {app.rejectedReason && (
                          <p className="text-sm text-red-400 mt-2 line-clamp-1">
                            거절 사유: {app.rejectedReason}
                          </p>
                        )}
                      </div>

                      {/* 상태 뱃지 */}
                      <span className={cn(
                        'text-xs font-bold px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full flex items-center gap-1.5 flex-shrink-0',
                        badge.className
                      )}>
                        <span className={cn('w-2 h-2 rounded-full', badge.dot)} />
                        {badge.label}
                      </span>

                      <ChevronRight size={16} className="text-gray-300 flex-shrink-0 hidden sm:block" />
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>
          </div>
        )}
      </div>
    </div>
  );
};

export default MyApplicationsPage;
