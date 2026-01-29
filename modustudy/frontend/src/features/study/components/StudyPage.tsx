import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, SlidersHorizontal, Grid, List, X, Sparkles } from 'lucide-react';
import StudyListContainer from './StudyListContainer';
import StudyCardContentV2 from './StudyCardContentV2';
import StudyFilter, { FilterState } from './StudyFilter';
import { studyService, Study, SortOption } from '../services/studyService';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button } from '@/shared/components';
import { cn } from '@/shared/utils/cn';

/**
 * StudyPageV2 - Google Material Design 스타일 스터디 목록 페이지
 *
 * 특징:
 * - 깔끔한 헤더 디자인
 * - 통합 검색 바
 * - 필터/정렬 컨트롤
 * - 그리드/리스트 뷰 전환
 * - CSS 변수 활용
 */
const StudyPageV2: React.FC = () => {
    const navigate = useNavigate();
    const [filteredStudies, setFilteredStudies] = useState<Study[]>([]);
    const [searchKeyword, setSearchKeyword] = useState('');
    const [filters, setFilters] = useState<FilterState>({
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
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [showFilters, setShowFilters] = useState(false);
    const pageSize = 12;

    // 초기 데이터 로드
    useEffect(() => {
        const allStudies = studyService.getAllStudies();
        setFilteredStudies(allStudies);
    }, []);

    // 필터 및 검색 적용
    useEffect(() => {
        let result = studyService.getFilteredStudies({
            ...filters,
            keyword: searchKeyword,
        });
        result = studyService.sortStudies(result, sortOption);
        setFilteredStudies(result);
        setCurrentPage(1);
    }, [filters, searchKeyword, sortOption]);

    // 필터 변경 핸들러
    const handleFilterChange = (newFilters: FilterState) => {
        setFilters(newFilters);
    };

    // 검색 핸들러
    const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
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

    // 찜하기 토글 핸들러
    const handleBookmarkToggle = (studyId: number) => {
        studyService.toggleBookmark(studyId);
        setFilteredStudies((prev) =>
            prev.map((study) =>
                study.id === studyId ? { ...study, isBookmarked: !study.isBookmarked } : study
            )
        );
    };

    // 스터디 클릭 핸들러 (V3 페이지로 이동)
    const handleStudyClick = (studyId: number) => {
        navigate(`/study/v3/${studyId}`);
    };

    // 미팅 타입 빠른 필터
    const handleMeetingTypeFilter = (type: string | null) => {
        if (type === null) {
            setFilters(prev => ({ ...prev, meetingType: [] }));
        } else {
            setFilters(prev => ({ ...prev, meetingType: [type] }));
        }
    };

    // 페이지네이션
    const paginatedData = studyService.paginateStudies(filteredStudies, currentPage, pageSize);

    // 현재 정렬 옵션 값
    const currentSortValue =
        sortOption.field === 'createdAt' && sortOption.order === 'desc' ? 'latest' :
        sortOption.field === 'createdAt' && sortOption.order === 'asc' ? 'oldest' :
        sortOption.field === 'currentMembers' ? 'popular' :
        sortOption.field === 'recruitEndDate' ? 'deadline' : 'latest';

    return (
        <UserLayoutV2>
            <StudyListContainer>
                <div className="max-w-7xl mx-auto px-4 md:px-6 py-6">
                    {/* 헤더 */}
                    <div className="flex justify-between mb-2">
                        <div className="flex items-center pt-2">
                            <div>
                                <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-text-primary)]">
                                    성장의 시작, 스터디 둘러보기
                                </h1>
                                <p className="text-sm text-[var(--color-text-secondary)] mt-1">
                                    총 <span className="font-bold text-[var(--color-primary)]">{filteredStudies.length}</span>개의 스터디
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center">
                            <Button
                                variant="primary"
                                size="md"
                                onClick={() => navigate('/study/create')}
                                leftIcon={<Plus size={18} />}
                                className="h-11 rounded-xl font-semibold shadow-md shadow-[var(--color-primary-alpha-20)]"
                            >
                                스터디 만들기
                            </Button>
                        </div>
                    </div>

                    {/* 검색 및 컨트롤 바 */}
                    <div className="bg-white rounded-2xl border border-[var(--color-border)] p-4 mb-6 shadow-sm">
                        <div className="flex flex-col lg:flex-row gap-4">
                            {/* 검색 바 */}
                            <form onSubmit={handleSearch} className="flex-1">
                                <div className="relative">
                                    <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-text-tertiary)]" />
                                    <input
                                        type="text"
                                        placeholder="스터디 이름, 주제로 검색..."
                                        value={searchKeyword}
                                        onChange={(e) => setSearchKeyword(e.target.value)}
                                        className="w-full h-11 pl-11 pr-4 bg-[var(--color-background)] border border-[var(--color-border-lighter)] rounded-xl text-sm focus:outline-none focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary-alpha-10)] transition-all"
                                    />
                                    {searchKeyword && (
                                        <button
                                            type="button"
                                            onClick={() => setSearchKeyword('')}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-[var(--color-background-secondary)] text-[var(--color-text-tertiary)]"
                                        >
                                            <X size={16} />
                                        </button>
                                    )}
                                </div>
                            </form>

                            {/* 컨트롤 버튼들 */}
                            <div className="flex items-center gap-3">
                                {/* 미팅 타입 필터 */}
                                <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1 border border-[var(--color-border-lighter)]">
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
                                                (option.value === null && filters.meetingType.length === 0) ||
                                                (option.value && filters.meetingType.includes(option.value))
                                                    ? "bg-white text-[var(--color-primary)] shadow-sm"
                                                    : "text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                                            )}
                                        >
                                            {option.label}
                                        </button>
                                    ))}
                                </div>

                                {/* 정렬 */}
                                <select
                                    value={currentSortValue}
                                    onChange={(e) => handleSortChange(e.target.value)}
                                    className="h-11 px-3 pr-8 bg-[var(--color-background)] border border-[var(--color-border-lighter)] rounded-xl text-xs font-semibold text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] cursor-pointer appearance-none"
                                    style={{
                                        backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%235F6368' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E")`,
                                        backgroundRepeat: 'no-repeat',
                                        backgroundPosition: 'right 10px center'
                                    }}
                                >
                                    <option value="latest">최신순</option>
                                    <option value="popular">인기순</option>
                                    <option value="deadline">마감임박순</option>
                                </select>

                                {/* 뷰 모드 전환 */}
                                <div className="flex items-center h-11 bg-[var(--color-background)] rounded-xl px-1 border border-[var(--color-border-lighter)]">
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
                                    onClick={() => setShowFilters(!showFilters)}
                                    leftIcon={<SlidersHorizontal size={16} />}
                                    className={cn(
                                        "h-11 rounded-xl",
                                        showFilters && "border-[var(--color-primary)] text-[var(--color-primary)]"
                                    )}
                                >
                                    필터
                                </Button>
                            </div>
                        </div>

                        {/* 확장 필터 */}
                        {showFilters && (
                            <div className="mt-4 pt-4 border-t border-[var(--color-border-lighter)]">
                                <StudyFilter onFilterChange={handleFilterChange} onSearch={() => {}} />
                            </div>
                        )}
                    </div>

                    {/* 난이도 범례 */}
                    <div className="flex items-center gap-4 mb-4 text-xs">
                        <span className="text-[var(--color-text-tertiary)] font-medium">난이도:</span>
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
                    {paginatedData.studies.length > 0 ? (
                        <>
                            <div className={cn(
                                "mb-8",
                                viewMode === 'grid'
                                    ? "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5"
                                    : "flex flex-col gap-3"
                            )}>
                                {paginatedData.studies.map((study) => (
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
                            {paginatedData.totalPages > 1 && (
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
                                        {Array.from({ length: paginatedData.totalPages }, (_, i) => i + 1).map((pageNum) => (
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
                                        disabled={currentPage === paginatedData.totalPages}
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
                                검색 결과가 없습니다
                            </p>
                            <p className="text-sm text-[var(--color-text-tertiary)]">
                                다른 검색어나 필터를 시도해보세요.
                            </p>
                        </div>
                    )}
                </div>
            </StudyListContainer>
        </UserLayoutV2>
    );
};

export default StudyPageV2;
