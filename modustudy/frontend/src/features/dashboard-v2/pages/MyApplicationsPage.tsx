// 내 참여 스터디 전체 페이지
// 대시보드 위젯의 풀사이즈 버전 - 카드 형식

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search, RefreshCw, Users, MapPin,
  Clock, CheckCircle2, XCircle, Compass, Calendar, Play, Loader2
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
import { studyApi, type StudyDetailResponse } from '@/api/endpoints/studyApi';

// 통합 상태 타입 (신청 상태 + 스터디 상태)
type CombinedStatus = 'PENDING' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

// 상태별 스타일
const STATUS_STYLES: Record<CombinedStatus, { label: string; className: string; dot: string; icon: React.ElementType; cardBorder: string }> = {
  PENDING: {
    label: '대기',
    className: 'bg-amber-50 text-amber-600',
    dot: 'bg-amber-500 animate-pulse',
    icon: Clock,
    cardBorder: 'hover:border-amber-100',
  },
  APPROVED: {
    label: '승인',
    className: 'bg-emerald-50 text-emerald-600',
    dot: 'bg-emerald-500',
    icon: CheckCircle2,
    cardBorder: 'hover:border-emerald-100',
  },
  IN_PROGRESS: {
    label: '진행',
    className: 'bg-blue-50 text-blue-600',
    dot: 'bg-blue-500',
    icon: Play,
    cardBorder: 'hover:border-blue-100',
  },
  COMPLETED: {
    label: '완료',
    className: 'bg-gray-50 text-gray-500',
    dot: 'bg-gray-400',
    icon: CheckCircle2,
    cardBorder: 'hover:border-gray-200',
  },
  REJECTED: {
    label: '거절',
    className: 'bg-red-50 text-red-400',
    dot: 'bg-red-400',
    icon: XCircle,
    cardBorder: 'hover:border-red-100',
  },
};

const getStatusBadge = (status: CombinedStatus) => {
  return STATUS_STYLES[status] || STATUS_STYLES.PENDING;
};

// 미팅 타입 텍스트
const getMeetingTypeText = (type?: string) => {
  switch (type) {
    case 'ONLINE': return '온라인';
    case 'OFFLINE': return '오프라인';
    case 'HYBRID': return '혼합';
    default: return type || '-';
  }
};

interface ParticipationItem {
  applicationId: number;
  studyId: number;
  studyName?: string;
  description?: string;
  applicationStatus: string;
  studyStatus?: string;
  combinedStatus: CombinedStatus;
  message?: string;
  createdAt: string;
  processedAt?: string;
  rejectedReason?: string;
  topic?: { name: string; icon?: string };
  maxMembers?: number;
  meetingType?: string;
  scheduleTime?: string;
}

type FilterStatus = 'ALL' | 'PENDING' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED';

export const MyApplicationsPage: React.FC = () => {
  const navigate = useNavigate();
  const [participations, setParticipations] = useState<ParticipationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [filter, setFilter] = useState<FilterStatus>('ALL');

  const fetchParticipations = async () => {
    setLoading(true);
    setError(false);
    try {
      // 신청 목록 조회
      const response = await studyApi.getMyApplications(undefined, 0, 100);
      const page = (response as Record<string, unknown>)?.content ? response : (response as Record<string, unknown>)?.data || response;
      const applications = page?.content || [];

      // 승인된 신청들에 대해 스터디 상태 조회
      const participationItems: ParticipationItem[] = await Promise.all(
        applications.map(async (app: { applicationId: number; studyId: number; studyName?: string; status: string; message?: string; createdAt: string; processedAt?: string; rejectedReason?: string }) => {
          let studyStatus: string | undefined;
          let topic: { name: string; icon?: string } | undefined;
          let maxMembers: number | undefined;
          let meetingType: string | undefined;
          let scheduleTime: string | undefined;
          let description: string | undefined;

          // 승인된 경우 스터디 상세 정보 조회
          if (app.status === 'APPROVED') {
            try {
              const studyDetail = await studyApi.getStudyDetail(app.studyId);
              // 백엔드 응답이 래퍼({ data: ... })를 사용할 수도 있음
              const wrapped = studyDetail as unknown as { data?: StudyDetailResponse };
              const study = wrapped?.data || studyDetail;
              studyStatus = study?.status;
              topic = study?.topic ? { name: study.topic.name, icon: study.topic.icon ?? undefined } : undefined;
              maxMembers = study?.maxMembers;
              meetingType = study?.meetingType;
              scheduleTime = study?.scheduleTime ?? undefined;
              description = study?.description ?? undefined;
            } catch {
              // 스터디 조회 실패 시 무시
            }
          }

          // 통합 상태 결정
          let combinedStatus: CombinedStatus = 'PENDING';
          if (app.status === 'REJECTED') {
            combinedStatus = 'REJECTED';
          } else if (app.status === 'APPROVED') {
            if (studyStatus === 'IN_PROGRESS') {
              combinedStatus = 'IN_PROGRESS';
            } else if (studyStatus === 'COMPLETED') {
              combinedStatus = 'COMPLETED';
            } else {
              combinedStatus = 'APPROVED';
            }
          }

          return {
            applicationId: app.applicationId,
            studyId: app.studyId,
            studyName: app.studyName,
            description,
            applicationStatus: app.status,
            studyStatus,
            combinedStatus,
            message: app.message,
            createdAt: app.createdAt,
            processedAt: app.processedAt,
            rejectedReason: app.rejectedReason,
            topic,
            maxMembers,
            meetingType,
            scheduleTime,
          };
        })
      );

      // 거절된 항목은 제외하고 정렬 (진행중 > 대기중 > 승인됨 > 완료)
      const filtered = participationItems.filter(p => p.combinedStatus !== 'REJECTED');
      const statusOrder: Record<CombinedStatus, number> = {
        IN_PROGRESS: 0,
        PENDING: 1,
        APPROVED: 2,
        COMPLETED: 3,
        REJECTED: 4,
      };
      filtered.sort((a, b) => statusOrder[a.combinedStatus] - statusOrder[b.combinedStatus]);

      setParticipations(filtered);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchParticipations();
  }, []);

  // 필터링 로직
  const filteredItems = filter === 'ALL'
    ? participations
    : participations.filter((p) => {
        if (filter === 'PENDING') return p.combinedStatus === 'PENDING';
        if (filter === 'APPROVED') return p.combinedStatus === 'APPROVED';
        if (filter === 'IN_PROGRESS') return p.combinedStatus === 'IN_PROGRESS';
        if (filter === 'COMPLETED') return p.combinedStatus === 'COMPLETED';
        return true;
      });

  // 필터별 카운트
  const filterCounts = {
    ALL: participations.length,
    PENDING: participations.filter((p) => p.combinedStatus === 'PENDING').length,
    APPROVED: participations.filter((p) => p.combinedStatus === 'APPROVED').length,
    IN_PROGRESS: participations.filter((p) => p.combinedStatus === 'IN_PROGRESS').length,
    COMPLETED: participations.filter((p) => p.combinedStatus === 'COMPLETED').length,
  };

  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
    } catch {
      return '';
    }
  };

  // 페이지 전환 애니메이션 (줌인 효과)
  const pageTransition = {
    initial: { opacity: 0, scale: 0.95 },
    animate: { opacity: 1, scale: 1 },
    transition: { duration: 0.3, ease: 'easeOut' as const },
  };

  return (
    <motion.div
      className="min-h-[calc(100vh-64px)] py-6 sm:py-8"
      initial={pageTransition.initial}
      animate={pageTransition.animate}
      transition={pageTransition.transition}
    >
      <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8">
        {/* 브레드크럼 + 헤더 */}
        <PageNavHeader
          title="내 참여 스터디"
          breadcrumbs={[
            { label: '대시보드', path: '/dashboard' },
            { label: '내 참여 스터디' },
          ]}
          badge={{ text: `${participations.length}개`, className: 'bg-violet-50 text-violet-600' }}
        />

        {/* 필터 탭 */}
        <div className="flex flex-wrap items-center gap-2 mb-6">
          {([
            { key: 'ALL' as FilterStatus, label: '전체' },
            { key: 'PENDING' as FilterStatus, label: '대기' },
            { key: 'APPROVED' as FilterStatus, label: '승인' },
            { key: 'IN_PROGRESS' as FilterStatus, label: '진행' },
            { key: 'COMPLETED' as FilterStatus, label: '완료' },
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
              onClick={fetchParticipations}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-red-50 text-red-500 font-medium hover:bg-red-100 transition-colors"
            >
              <RefreshCw size={16} />
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!loading && !error && participations.length === 0 && (
          <div className="text-center py-20">
            <div className="w-20 h-20 mx-auto mb-5 bg-violet-50 rounded-2xl flex items-center justify-center">
              <Compass size={36} className="text-violet-300" />
            </div>
            <p className="text-gray-800 text-lg font-semibold mb-2">참여 중인 스터디가 없어요</p>
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
        {!loading && !error && participations.length > 0 && filteredItems.length === 0 && (
          <div className="text-center py-16">
            <p className="text-gray-400">해당 상태의 스터디가 없습니다</p>
          </div>
        )}

        {/* 참여 스터디 목록 - 카드 그리드 */}
        {!loading && !error && filteredItems.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <AnimatePresence>
              {filteredItems.map((item, idx) => {
                const badge = getStatusBadge(item.combinedStatus);
                const StatusIcon = badge.icon;
                const canAccessWorkspace = item.combinedStatus === 'IN_PROGRESS' || item.combinedStatus === 'COMPLETED';

                return (
                  <motion.div
                    key={item.applicationId}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -12 }}
                    transition={{ delay: idx * 0.03 }}
                    onClick={() => navigate(`/study/${item.studyId}`)}
                    className={cn(
                      'bg-white rounded-2xl p-5 border border-gray-100 cursor-pointer',
                      'hover:shadow-lg hover:-translate-y-0.5',
                      'transition-all duration-200 group',
                      badge.cardBorder
                    )}
                  >
                    {/* 상단: 주제 + 상태 */}
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-2">
                        <div className={cn(
                          'w-8 h-8 rounded-lg flex items-center justify-center',
                          item.combinedStatus === 'PENDING' && 'bg-amber-50',
                          item.combinedStatus === 'APPROVED' && 'bg-emerald-50',
                          item.combinedStatus === 'IN_PROGRESS' && 'bg-blue-50',
                          item.combinedStatus === 'COMPLETED' && 'bg-gray-50',
                        )}>
                          {item.topic?.icon ? (
                            <span className="text-sm">{item.topic.icon}</span>
                          ) : (
                            <StatusIcon size={14} className={cn(
                              item.combinedStatus === 'PENDING' && 'text-amber-500',
                              item.combinedStatus === 'APPROVED' && 'text-emerald-500',
                              item.combinedStatus === 'IN_PROGRESS' && 'text-blue-500',
                              item.combinedStatus === 'COMPLETED' && 'text-gray-400',
                            )} />
                          )}
                        </div>
                        {item.topic && (
                          <span className="text-xs font-medium text-violet-600">{item.topic.name}</span>
                        )}
                      </div>
                      <span className={cn(
                        'text-[10px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1',
                        badge.className
                      )}>
                        <span className={cn('w-1.5 h-1.5 rounded-full', badge.dot)} />
                        {badge.label}
                      </span>
                    </div>

                    {/* 타이틀 */}
                    <h3 className="font-bold text-gray-900 mb-2 group-hover:text-violet-700 transition-colors line-clamp-1">
                      {item.studyName || `스터디 #${item.studyId}`}
                    </h3>

                    {/* 설명 또는 대기 안내 */}
                    {item.combinedStatus === 'PENDING' ? (
                      <p className="text-sm text-amber-500 mb-3 flex items-center gap-1.5">
                        <Loader2 size={14} className="animate-spin" />
                        승인 대기 중입니다
                      </p>
                    ) : item.description ? (
                      <p className="text-sm text-gray-500 mb-3 line-clamp-2">{item.description}</p>
                    ) : (
                      <p className="text-sm text-gray-400 mb-3">{formatDate(item.createdAt)} 신청</p>
                    )}

                    {/* 메타 정보 */}
                    <div className="flex items-center gap-3 text-xs text-gray-400 mt-auto pt-3 border-t border-gray-50">
                      {item.meetingType && (
                        <span className="flex items-center gap-1">
                          <MapPin size={12} />
                          {getMeetingTypeText(item.meetingType)}
                        </span>
                      )}
                      {item.maxMembers && (
                        <span className="flex items-center gap-1">
                          <Users size={12} />
                          최대 {item.maxMembers}명
                        </span>
                      )}
                      {item.scheduleTime && (
                        <span className="flex items-center gap-1">
                          <Clock size={12} />
                          {item.scheduleTime.substring(0, 5)}
                        </span>
                      )}
                      {!item.meetingType && !item.maxMembers && !item.scheduleTime && (
                        <span className="flex items-center gap-1">
                          <Calendar size={12} />
                          {formatDate(item.createdAt)}
                        </span>
                      )}
                    </div>

                    {/* 하단 버튼 영역 */}
                    <div className="mt-3 pt-3 border-t border-gray-50 flex gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/study/${item.studyId}`);
                        }}
                        className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg bg-violet-50 text-violet-600 text-xs font-semibold hover:bg-violet-100 transition-colors"
                      >
                        <Search size={12} />
                        스터디 상세
                      </button>
                      {/* 진행중/완료 상태일 때만 워크스페이스 버튼 표시 */}
                      {canAccessWorkspace && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/study/${item.studyId}/workspace`);
                          }}
                          className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 bg-violet-500 hover:bg-violet-600 transition-colors shadow-sm shadow-violet-200"
                          title="워크스페이스로 이동"
                        >
                          <Play size={14} fill="white" className="text-white ml-0.5" />
                        </button>
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>
          </div>
        )}
      </div>
    </motion.div>
  );
};

export default MyApplicationsPage;
