import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, SlidersHorizontal, Grid, List } from 'lucide-react';
import { PageListHeader, PageListSubHeader } from '@/shared/components/layouts';
import StudyListContainer from './StudyListContainer';
import StudyCardContentV2 from './StudyCardContentV2';
import StudyFilter, { FilterState } from './StudyFilter';
import { Study, SortOption } from '../services/studyService';
import { getStudyList, getLeaderInfo, getProvinces, getDistricts, StudyListItem, LeaderInfoResponse, studyApi } from '@/api/endpoints/studyApi';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button, StudyCardSkeletonGrid } from '@/shared/components';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { usePageLoading } from '@/shared/hooks/usePageLoading';

// 햇빛이 자연스럽게 스치는 효과
const sunlightStyles = `
@keyframes sunlight {
  0%, 100% {
    background-position: 200% center;
  }
  50% {
    background-position: -100% center;
  }
}

.sunlight-text {
  background: linear-gradient(
    110deg,
    var(--color-text-primary) 0%,
    var(--color-text-primary) 25%,
    #7ba8d4 42%,
    #a3c4e8 50%,
    #7ba8d4 58%,
    var(--color-text-primary) 75%,
    var(--color-text-primary) 100%
  );
  background-size: 300% auto;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: sunlight 10s ease-in-out infinite;
}
`;

/**
 * StudyPageV2 - Google Material Design 스타일 스터디 목록 페이지
 *
 * 특징:
 * - 프리미엄 헤더 디자인
 * - 통합 검색바
 * - 필터/정렬 컨트롤
 * - 그리드/리스트 뷰 전환
 * - CSS 변수 사용
 */
const StudyPageV2: React.FC = () => {
    const navigate = useNavigate();
    const [studies, setStudies] = useState<Study[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchInput, setSearchInput] = useState('');

    // 상단 로딩 게이지바 연동
    const { startLoading, finishLoading } = usePageLoading();

    useEffect(() => {
        if (isLoading) {
            startLoading();
        } else {
            finishLoading();
        }
    }, [isLoading, startLoading, finishLoading]);
    const [appliedSearchKeyword, setAppliedSearchKeyword] = useState('');
    // 지역명 매핑 (regionId -> region name)
    const [regionMap, setRegionMap] = useState<Map<number, string>>(new Map());
    const [pendingFilters, setPendingFilters] = useState<FilterState>({
        status: [],
        topic: [],
        subTopic: [],
        meetingType: [],
        difficulty: [],
        studyType: [],
        regionId: [],
    });
    const [appliedFilters, setAppliedFilters] = useState<FilterState>({
        status: [],
        topic: [],
        subTopic: [],
        meetingType: [],
        difficulty: [],
        studyType: [],
        regionId: [],
    });
    const [sortOption, setSortOption] = useState<SortOption>({
        field: 'createdAt',
        order: 'desc',
    });
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [showFilters, setShowFilters] = useState(false);
    const pageSize = 12;

    // 리더 정보가 포함된 스터디 변환 (별도 API 조회 결과 사용)
    const convertToStudyWithLeader = (item: StudyListItem, leaderInfo?: LeaderInfoResponse, regionNameMap?: Map<number, string>, isBookmarked?: boolean): Study => {
        // regionId가 있으면 region 객체 생성
        let region: { id: number; name: string } | undefined;
        if (item.regionId && regionNameMap && regionNameMap.has(item.regionId)) {
            region = {
                id: item.regionId,
                name: regionNameMap.get(item.regionId)!
            };
        }

        return {
            id: item.id,
            leaderId: leaderInfo?.userId || item.leader?.id || item.leaderId || 0,
            name: item.name,
            description: item.description || '',
            topic: item.topic?.name || '',
            format: item.format?.name || '',
            studyType: item.studyType,
            meetingType: item.meetingType,
            status: item.status,
            isPublic: true,
            maxMembers: item.maxMembers,
            currentMembers: item.currentMembers || 1, // API 값 사용
            difficulty: item.difficulty || 'BEGINNER',
            scheduleDays: item.scheduleDays || '',
            scheduleTime: item.scheduleTime,
            regionId: item.regionId,
            region, // region 객체 추가
            recruitEndDate: item.recruitEndDate,
            leader: {
                id: leaderInfo?.userId || item.leader?.id || item.leaderId || 0,
                nickname: leaderInfo?.nickname || item.leader?.nickname || '스터디장',
                profileImage: leaderInfo?.profileImage || item.leader?.profileImage || null,
                leaderRating: leaderInfo?.leaderRating ?? item.leader?.leaderRating ?? null,
                leaderReviewCount: leaderInfo?.leaderReviewCount || item.leader?.leaderReviewCount || 0,
            },
            isBookmarked: isBookmarked ?? false,
            createdAt: item.createdAt,
        };
    };

    // 지역 데이터 로드 (최초 1회)
    const loadRegionData = async (): Promise<Map<number, string>> => {
        // 이미 로드된 경우 기존 맵 반환
        if (regionMap.size > 0) {
            return regionMap;
        }

        const newRegionMap = new Map<number, string>();
        try {
            const provinces = await getProvinces();
            // 각 시/도에 대한 구/군 목록 병렬 로드
            await Promise.all(provinces.map(async (province) => {
                try {
                    const districts = await getDistricts(province.id);
                    districts.forEach((district) => {
                        // "시도 구군" 형식으로 저장
                        newRegionMap.set(district.id, `${province.name} ${district.name}`);
                    });
                } catch (err) {
                }
            }));
            setRegionMap(newRegionMap);
        } catch (error) {
        }
        return newRegionMap;
    };

    // API에서 스터디 목록 로드 (리더 정보 포함)
    const loadStudies = async () => {
        setIsLoading(true);
        try {
            // 지역 데이터 먼저 로드
            const currentRegionMap = await loadRegionData();

            const sortParam = sortOption.field === 'createdAt'
                ? `createdAt,${sortOption.order}`
                : sortOption.field === 'recruitEndDate'
                ? `recruitEndDate,${sortOption.order}`
                : `createdAt,desc`;

            const meetingTypeParam = appliedFilters.meetingType.length === 1 ? appliedFilters.meetingType[0] : undefined;
            const statusParam = appliedFilters.status.length === 1 ? appliedFilters.status[0] : undefined;

            const response = await getStudyList({
                page: currentPage - 1, // API는 0-based
                size: pageSize,
                sort: sortParam,
                keyword: appliedSearchKeyword || undefined,
                meetingType: meetingTypeParam,
                difficulty: appliedFilters.difficulty.length === 1 ? appliedFilters.difficulty[0] : undefined,
                status: statusParam,
            });

            // 안전한 배열 처리 (백엔드 반환 참조 대비)
            const content = response?.content || [];
            const filteredContent = content.filter((item) => {
                const topicName = item.topic?.name ?? '';
                const parentTopicName = item.topic?.parent?.name ?? '';
                const matchesTopic =
                    appliedFilters.topic.length === 0 ||
                    appliedFilters.topic.includes(topicName) ||
                    appliedFilters.topic.includes(parentTopicName);
                const matchesSubTopic =
                    appliedFilters.subTopic.length === 0 || appliedFilters.subTopic.includes(topicName);
                const matchesMeetingType =
                    appliedFilters.meetingType.length === 0 || appliedFilters.meetingType.includes(item.meetingType);
                const matchesDifficulty =
                    appliedFilters.difficulty.length === 0 ||
                    appliedFilters.difficulty.includes(item.difficulty ?? '');
                const matchesStatus =
                    appliedFilters.status.length === 0 || appliedFilters.status.includes(item.status);
                const matchesStudyType =
                    appliedFilters.studyType.length === 0 || appliedFilters.studyType.includes(item.studyType);

                return (
                    matchesTopic &&
                    matchesSubTopic &&
                    matchesMeetingType &&
                    matchesDifficulty &&
                    matchesStatus &&
                    matchesStudyType
                );
            });

            // 각 스터디의 리더 정보 및 북마크 상태 병렬 조회
            const leaderInfoMap = new Map<number, LeaderInfoResponse>();
            const bookmarkMap = new Map<number, boolean>();

            const fetchPromises = filteredContent.map(async (item) => {
                // 리더 정보 조회
                try {
                    const leaderInfo = await getLeaderInfo(item.id);
                    leaderInfoMap.set(item.id, leaderInfo);
                } catch (err) {
                }
                // 북마크 상태 조회
                try {
                    const isBookmarked = await studyApi.checkBookmark(item.id);
                    bookmarkMap.set(item.id, isBookmarked);
                } catch (err) {
                    // 에러 발생 시 false로 처리
                    bookmarkMap.set(item.id, false);
                }
            });

            await Promise.all(fetchPromises);

            // 리더/지역/북마크 정보를 포함해 스터디 변환
            const convertedStudies = filteredContent.map((item) => {
                const leaderInfo = leaderInfoMap.get(item.id);
                const isBookmarked = bookmarkMap.get(item.id);
                return convertToStudyWithLeader(item, leaderInfo, currentRegionMap, isBookmarked);
            });

            setStudies(convertedStudies);
            setTotalPages(response?.totalPages || 0);
            setTotalElements(response?.totalElements || 0);
        } catch (error) {
            setStudies([]);
        } finally {
            setIsLoading(false);
        }
    };

    // 초기 로드 및 필터/정렬/페이지 변경 시 로드
    useEffect(() => {
        loadStudies();
    }, [currentPage, sortOption, appliedFilters, appliedSearchKeyword]);

    // 페이지 복귀 시 최신 상태 반영을 위해 다시 로드
    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                loadStudies();
            }
        };
        document.addEventListener('visibilitychange', handleVisibilityChange);
        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [currentPage, sortOption, appliedFilters, appliedSearchKeyword]);

    // 검색/필터 시 페이지 리셋
    // 검색어 변경은 "검색" 버튼 클릭 시 적용

    // 필터 변경 핸들러
    const handleFilterChange = (newFilters: FilterState) => {
        setPendingFilters(newFilters);
    };

    const applyFilters = (closePanel: boolean) => {
        setAppliedFilters(pendingFilters);
        setAppliedSearchKeyword(searchInput);
        setCurrentPage(1);
        if (closePanel) {
            setShowFilters(false);
        }
    };

    // 검색 핸들러
    const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        applyFilters(false);
    };

    // 정렬 변경 핸들러
    const handleSortChange = (value: string) => {
        switch (value) {
            case 'latest':
                setSortOption({ field: 'createdAt', order: 'desc' });
                break;
            case 'oldest':
                setSortOption({ field: 'createdAt', order: 'asc' });
                break;
            case 'popular':
                setSortOption({ field: 'currentMembers', order: 'desc' });
                break;
            case 'deadline':
                setSortOption({ field: 'recruitEndDate', order: 'asc' });
                break;
            default:
                setSortOption({ field: 'createdAt', order: 'desc' });
        }
    };

    // 북마크 토글 핸들러
    const handleBookmarkToggle = async (studyId: number) => {
        try {
            await studyApi.toggleBookmark(studyId);
            setStudies((prev) =>
                prev.map((study) =>
                    study.id === studyId ? { ...study, isBookmarked: !study.isBookmarked } : study
                )
            );
        } catch (error) {
        }
    };

    // 스터디 클릭 핸들러
    const handleStudyClick = (studyId: number) => {
        navigate(`/study/${studyId}`);
    };

    // 미팅 타입 빠른 필터
    const handleMeetingTypeFilter = (type: string | null) => {
        if (type === null) {
            setPendingFilters(prev => ({ ...prev, meetingType: [] }));
        } else {
            setPendingFilters(prev => ({ ...prev, meetingType: [type] }));
        }
        
    };

    // 현재 정렬 옵션 값
    const currentSortValue =
        sortOption.field === 'createdAt' && sortOption.order === 'desc' ? 'latest' :
        sortOption.field === 'createdAt' && sortOption.order === 'asc' ? 'oldest' :
        sortOption.field === 'currentMembers' ? 'popular' :
        sortOption.field === 'recruitEndDate' ? 'deadline' : 'latest';

    return (
        <UserLayoutV2>
            <style>{sunlightStyles}</style>
            <StudyListContainer>
                <div className="max-w-7xl mx-auto px-4 md:px-6 py-6">
                    {/* 헤더 - 공통 컴포넌트 */}
                    <PageListHeader
                        title="성장을 시작하고, 스터디를 둘러보기"
                        titleClassName="sunlight-text"
                        subtitle={
                            <>총 <span className="font-bold text-[var(--color-primary)]">{totalElements}</span>개의 스터디</>
                        }
                        actions={
                            <Button
                                variant="primary"
                                size="md"
                                onClick={() => navigate('/study/create')}
                                leftIcon={<Plus size={18} />}
                                className="h-11 rounded-xl font-semibold shadow-md shadow-[var(--color-primary-alpha-20)]"
                            >
                                스터디 만들기
                            </Button>
                        }
                    />

                    {/* 서브헤더 - 공통 컴포넌트 */}
                    <PageListSubHeader
                        searchValue={searchInput}
                        onSearchChange={setSearchInput}
                        searchPlaceholder="스터디 이름, 주제로 검색..."
                        onSearchSubmit={handleSearch}
                        filterControls={
                            <>
                                {/* 미팅 타입 필터 */}
                                <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1">
                                    {[
                                        { value: null, label: '전체' },
                                        { value: 'ONLINE', label: '온라인' },
                                        { value: 'OFFLINE', label: '오프라인' },
                                    ].map((option) => (
                                        <button
                                            key={option.label}
                                            onClick={() => handleMeetingTypeFilter(option.value)}
                                            className={cn(
                                                "px-3 py-2 text-xs font-semibold rounded-lg transition-all",
                                                (option.value === null && pendingFilters.meetingType.length === 0) ||
                                                (option.value && pendingFilters.meetingType.includes(option.value))
                                                    ? "bg-white text-[var(--color-primary)] shadow-sm"
                                                    : "text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                                            )}
                                        >
                                            {option.label}
                                        </button>
                                    ))}
                                </div>

                                {/* 정렬 */}
                                <Select
                                    value={currentSortValue}
                                    onChange={(value) => handleSortChange(value)}
                                    options={[
                                        { value: 'latest', label: '최신순' },
                                        { value: 'popular', label: '인기순' },
                                        { value: 'deadline', label: '마감임박순' },
                                    ]}
                                    className="w-auto mb-0"
                                    buttonClassName="h-11 px-3 py-2 text-xs font-semibold rounded-xl bg-[var(--color-background)]"
                                />

                                {/* 뷰 모드 전환 */}
                                <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1">
                                    <button
                                        onClick={() => setViewMode('grid')}
                                        className={cn(
                                            "p-2 rounded-lg transition-all",
                                            viewMode === 'grid'
                                                ? "bg-white text-[var(--color-primary)] shadow-sm"
                                                : "text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)]"
                                        )}
                                    >
                                        <Grid size={18} />
                                    </button>
                                    <button
                                        onClick={() => setViewMode('list')}
                                        className={cn(
                                            "p-2 rounded-lg transition-all",
                                            viewMode === 'list'
                                                ? "bg-white text-[var(--color-primary)] shadow-sm"
                                                : "text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)]"
                                        )}
                                    >
                                        <List size={18} />
                                    </button>
                                </div>

                                {/* 필터 버튼 */}
                                <Button
                                    variant="google-outline"
                                    size="sm"
                                    onClick={() => {
                                        setShowFilters(!showFilters);
                                    }}
                                    leftIcon={<SlidersHorizontal size={16} />}
                                    className={cn(
                                        "h-11 rounded-xl",
                                        showFilters && "border-[var(--color-primary)] text-[var(--color-primary)]"
                                    )}
                                >
                                    필터
                                </Button>
                            </>
                        }
                        expandedFilter={
                            showFilters ? (
                                <div className="mt-4 pt-4 border-t border-[var(--color-border-lighter)]">
                                    <StudyFilter
                                        onFilterChange={handleFilterChange}
                                        onSearch={() => applyFilters(true)}
                                        defaultOpen
                                        showHeader={false}
                                        filters={pendingFilters}
                                    />
                                </div>
                            ) : null
                        }
                    />

                    {/* 난이도 범례 */}
                    <div className="flex items-center gap-4 mb-4 text-xs">
                        <span className="text-[var(--color-text-tertiary)] font-medium">난이도</span>
                        <div className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-sm bg-[var(--color-success)]" />
                            <span className="text-[var(--color-text-secondary)]">입문</span>
                        </div>
                        <div className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-sm bg-[var(--color-primary)]" />
                            <span className="text-[var(--color-text-secondary)]">중급</span>
                        </div>
                        <div className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-sm bg-[var(--color-error)]" />
                            <span className="text-[var(--color-text-secondary)]">고급</span>
                        </div>
                    </div>

                    {/* 스터디 목록 */}
                    {isLoading ? (
                        <StudyCardSkeletonGrid count={8} />
                    ) : studies.length > 0 ? (
                        <>
                            <div className={cn(
                                "mb-8",
                                viewMode === 'grid'
                                    ? "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5"
                                    : "flex flex-col gap-3"
                            )}>
                                {studies.map((study) => (
                                    <StudyCardContentV2
                                        key={study.id}
                                        study={study}
                                        variant={viewMode === 'grid' ? 'card' : 'list'}
                                        onBookmarkToggle={handleBookmarkToggle}
                                        onClick={handleStudyClick}
                                    />
                                ))}
                            </div>

                            {/* 페이지네이션 */}
                            {totalPages > 1 && (
                                <div className="flex justify-center items-center gap-6">
                                    <button
                                        className="p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                                        disabled={currentPage === 1}
                                        onClick={() => setCurrentPage((prev) => prev - 1)}
                                    >
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="m15 18-6-6 6-6" />
                                        </svg>
                                    </button>

                                    <div className="flex items-center gap-4">
                                        {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                                            <button
                                                key={pageNum}
                                                className={cn(
                                                    "text-sm font-medium transition-colors",
                                                    currentPage === pageNum
                                                        ? "text-[var(--color-primary)] font-bold"
                                                        : "text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)]"
                                                )}
                                                onClick={() => setCurrentPage(pageNum)}
                                            >
                                                {pageNum}
                                            </button>
                                        ))}
                                    </div>

                                    <button
                                        className="p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                                        disabled={currentPage === totalPages}
                                        onClick={() => setCurrentPage((prev) => prev + 1)}
                                    >
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="m9 18 6-6-6-6" />
                                        </svg>
                                    </button>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="text-center py-16 bg-white rounded-2xl border border-[var(--color-border)]">
                            <div className="w-16 h-16 mx-auto mb-4 bg-[var(--color-background-secondary)] rounded-full flex items-center justify-center">
                                <Search size={28} className="text-[var(--color-text-muted)]" />
                            </div>
                            <p className="text-lg font-semibold text-[var(--color-text-primary)] mb-2">
                                등록된 스터디가 없습니다
                            </p>
                            <p className="text-sm text-[var(--color-text-tertiary)]">
                                첫 번째 스터디를 만들어보세요!
                            </p>
                        </div>
                    )}
                </div>
            </StudyListContainer>
        </UserLayoutV2>
    );
};

export default StudyPageV2;











