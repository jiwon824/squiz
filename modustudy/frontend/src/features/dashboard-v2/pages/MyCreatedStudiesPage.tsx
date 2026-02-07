// 개설한 스터디 전체 페이지
// 대시보드 위젯의 풀사이즈 버전

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Crown, Plus, Users, RefreshCw,
  MapPin, Settings, Clock, Play
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
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

// 미팅 타입 텍스트
const getMeetingTypeText = (type?: string) => {
  switch (type) {
    case 'ONLINE': return '온라인';
    case 'OFFLINE': return '오프라인';
    case 'HYBRID': return '혼합';
    default: return type || '-';
  }
};

interface StudyItem {
  id: number;
  name: string;
  description?: string | null;
  status: string;
  studyType?: string;
  meetingType?: string;
  maxMembers?: number;
  difficulty?: string | null;
  startDate?: string;
  endDate?: string;
  scheduleDays?: string | null;
  scheduleTime?: string | null;
  topic?: { id: number; name: string; icon?: string | null };
  leader?: { id: number; nickname?: string; profileImage?: string | null };
}

type FilterStatus = 'ALL' | 'PENDING' | 'RECRUITING' | 'IN_PROGRESS' | 'COMPLETED';

export const MyCreatedStudiesPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [studies, setStudies] = useState<StudyItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [filter, setFilter] = useState<FilterStatus>('ALL');

  const fetchStudies = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await studyApi.getMyStudies(0, 100);
      // PageResponse 또는 래퍼 응답 처리
      const pageData = response as { content?: StudyItem[]; data?: { content?: StudyItem[] } };
      const content: StudyItem[] = pageData?.content || pageData?.data?.content || [];
      const myCreated = user?.id
        ? content.filter((s) => s.leader?.id === Number(user.id))
        : content;
      setStudies(myCreated);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudies();
  }, []);

  const filteredStudies = filter === 'ALL'
    ? studies
    : studies.filter((s) => s.status === filter);

  const filterCounts = {
    ALL: studies.length,
    PENDING: studies.filter((s) => s.status === 'PENDING').length,
    RECRUITING: studies.filter((s) => s.status === 'RECRUITING').length,
    IN_PROGRESS: studies.filter((s) => s.status === 'IN_PROGRESS').length,
    COMPLETED: studies.filter((s) => s.status === 'COMPLETED').length,
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
          title="개설한 스터디"
          breadcrumbs={[
            { label: '마이홈', path: '/dashboard' },
            { label: '개설한 스터디' },
          ]}
          badge={{ text: `${studies.length}개`, className: 'bg-blue-50 text-blue-600' }}
        />

        {/* 필터 탭 */}
        <div className="flex flex-wrap items-center gap-2 mb-6">
          {([
            { key: 'ALL' as FilterStatus, label: '전체' },
            { key: 'PENDING' as FilterStatus, label: '대기' },
            { key: 'RECRUITING' as FilterStatus, label: '모집' },
            { key: 'IN_PROGRESS' as FilterStatus, label: '진행' },
            { key: 'COMPLETED' as FilterStatus, label: '완료' },
          ]).map((tab) => (
            <button
              key={tab.key}
              onClick={() => setFilter(tab.key)}
              className={cn(
                'px-3 sm:px-4 py-2 rounded-xl text-sm font-medium transition-all',
                filter === tab.key
                  ? 'bg-blue-600 text-white shadow-sm'
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
            onClick={() => navigate('/study/create')}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-600 text-white font-semibold text-sm hover:bg-blue-700 transition-colors shadow-sm"
          >
            <Plus size={16} />
            <span className="hidden sm:inline">새 스터디 개설</span>
            <span className="sm:hidden">개설</span>
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
              onClick={fetchStudies}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-red-50 text-red-500 font-medium hover:bg-red-100 transition-colors"
            >
              <RefreshCw size={16} />
              다시 시도
            </button>
          </div>
        )}

        {/* 빈 상태 */}
        {!loading && !error && studies.length === 0 && (
          <div className="text-center py-20">
            <div className="w-20 h-20 mx-auto mb-5 bg-blue-50 rounded-2xl flex items-center justify-center">
              <Crown size={36} className="text-blue-300" />
            </div>
            <p className="text-gray-800 text-lg font-semibold mb-2">개설한 스터디가 없어요</p>
            <p className="text-gray-400 mb-6">스터디를 만들고 함께 성장할 멤버를 모아보세요</p>
            <button
              onClick={() => navigate('/study/create')}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-blue-600 text-white font-semibold hover:bg-blue-700 transition-colors shadow-sm shadow-blue-200"
            >
              <Plus size={18} />
              스터디 개설하기
            </button>
          </div>
        )}

        {/* 필터 결과 없음 */}
        {!loading && !error && studies.length > 0 && filteredStudies.length === 0 && (
          <div className="text-center py-16">
            <p className="text-gray-400">해당 상태의 스터디가 없습니다</p>
          </div>
        )}

        {/* 스터디 목록 */}
        {!loading && !error && filteredStudies.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <AnimatePresence>
              {filteredStudies.map((study, idx) => {
                const badge = getStatusBadge(study.status);
                return (
                  <motion.div
                    key={study.id}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -12 }}
                    transition={{ delay: idx * 0.03 }}
                    onClick={() => navigate(`/study/${study.id}`)}
                    className={cn(
                      'bg-white rounded-2xl p-5 border border-gray-100 cursor-pointer',
                      'hover:shadow-lg hover:border-blue-100 hover:-translate-y-0.5',
                      'transition-all duration-200 group'
                    )}
                  >
                    {/* 상단: 주제 + 상태 */}
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                          {study.topic?.icon ? (
                            <span className="text-sm">{study.topic.icon}</span>
                          ) : (
                            <Users size={14} className="text-blue-500" />
                          )}
                        </div>
                        {study.topic && (
                          <span className="text-xs font-medium text-blue-600">{study.topic.name}</span>
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
                    <h3 className="font-bold text-gray-900 mb-2 group-hover:text-blue-700 transition-colors line-clamp-1">
                      {study.name}
                    </h3>

                    {/* 설명 */}
                    {study.description && (
                      <p className="text-sm text-gray-500 mb-3 line-clamp-2">{study.description}</p>
                    )}

                    {/* 메타 정보 */}
                    <div className="flex items-center gap-3 text-xs text-gray-400 mt-auto pt-3 border-t border-gray-50">
                      {study.meetingType && (
                        <span className="flex items-center gap-1">
                          <MapPin size={12} />
                          {getMeetingTypeText(study.meetingType)}
                        </span>
                      )}
                      {study.maxMembers && (
                        <span className="flex items-center gap-1">
                          <Users size={12} />
                          최대 {study.maxMembers}명
                        </span>
                      )}
                      {study.scheduleTime && (
                        <span className="flex items-center gap-1">
                          <Clock size={12} />
                          {study.scheduleTime.substring(0, 5)}
                        </span>
                      )}
                    </div>

                    {/* 관리 버튼 + 워크스페이스 버튼 */}
                    <div className="mt-3 pt-3 border-t border-gray-50 flex gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/study/manage/${study.id}`);
                        }}
                        className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg bg-blue-50 text-blue-600 text-xs font-semibold hover:bg-blue-100 transition-colors"
                      >
                        <Settings size={12} />
                        스터디 관리
                      </button>
                      {/* 진행중/완료 상태일 때만 워크스페이스 버튼 표시 */}
                      {(study.status === 'IN_PROGRESS' || study.status === 'COMPLETED') && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/study/${study.id}/workspace`);
                          }}
                          className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 bg-blue-500 hover:bg-blue-600 transition-colors shadow-sm shadow-blue-200"
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

export default MyCreatedStudiesPage;
