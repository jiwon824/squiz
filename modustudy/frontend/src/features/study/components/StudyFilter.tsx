import React, { useState } from 'react';
import { Search, SlidersHorizontal, X, Plus, ChevronDown, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import '../styles/StudyFilter.css';

interface StudyFilterProps {
    onFilterChange: (filters: FilterState) => void;
    onSearch: (keyword: string) => void;
}

export interface FilterState {
    status: string[];
    topic: string[];
    subTopic: string[]; // 세부 주제 추가
    meetingType: string[];
    difficulty: string[];
    studyType: string[];
    regionId: number[];
}

// 대분류 → 소분류 매핑
const TOPIC_CATEGORIES: Record<string, string[]> = {
    '알고리즘/코딩테스트': ['백준', '프로그래머스', 'SWEA', 'LeetCode', '코딩테스트 대비'],
    'CS 기초': ['자료구조', '알고리즘 이론', '운영체제', '네트워크', '데이터베이스', '컴퓨터구조', '디자인패턴', '시스템 설계'],
    '프론트엔드': ['HTML/CSS', 'JavaScript', 'TypeScript', 'React', 'Vue', 'Next.js', '웹 접근성/성능'],
    '백엔드': ['Java/Spring', 'Python/Django', 'Python/FastAPI', 'Node.js/Express', 'Go', 'Kotlin', 'API 설계'],
    '인프라/DevOps': ['Docker', 'Kubernetes', 'CI/CD', 'AWS', 'GCP', 'Linux', '모니터링'],
    'AI/ML': ['머신러닝 기초', '딥러닝', 'NLP', '컴퓨터 비전', 'MLOps', '논문 리뷰'],
    '모바일': ['Android (Kotlin)', 'Android (Java)', 'iOS (Swift)', 'Flutter', 'React Native'],
    '자격증': ['정보처리기사', 'SQLD/SQLP', '리눅스마스터', '네트워크관리사', 'AWS 자격증', 'Azure 자격증', 'CKAD/CKA'],
    '취업 준비': ['기술 면접', '코딩테스트 대비', '포트폴리오', '이력서/자소서', '모의 면접'],
    '프로젝트': ['사이드 프로젝트', '클론 코딩', '오픈소스 기여', '해커톤 준비']
};

const StudyFilter: React.FC<StudyFilterProps> = ({ onFilterChange, onSearch }) => {
    const navigate = useNavigate();
    const [searchKeyword, setSearchKeyword] = useState('');
    const [showFilters, setShowFilters] = useState(false);
    const [expandedTopics, setExpandedTopics] = useState<string[]>([]); // 펼쳐진 대분류
    const [filters, setFilters] = useState<FilterState>({
        status: [],
        topic: [],
        subTopic: [],
        meetingType: [],
        difficulty: [],
        studyType: [],
        regionId: [],
    });

    // 필터 옵션 정의
    const filterOptions = {
        status: [
            { value: 'RECRUITING', label: '모집중' },
            { value: 'IN_PROGRESS', label: '진행중' },
            { value: 'COMPLETED', label: '완료' },
        ],
        topic: Object.keys(TOPIC_CATEGORIES).map(topic => ({ value: topic, label: topic })),
        meetingType: [
            { value: 'ONLINE', label: '온라인' },
            { value: 'OFFLINE', label: '오프라인' },
            { value: 'HYBRID', label: '혼합' },
        ],
        difficulty: [
            { value: 'BEGINNER', label: '초급' },
            { value: 'INTERMEDIATE', label: '중급' },
            { value: 'ADVANCED', label: '고급' },
        ],
        studyType: [
            { value: 'PLANNED', label: '정규' },
            { value: 'LIGHTNING', label: '번개' },
        ],
    };

    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchKeyword(e.target.value);
    };

    const handleSearchSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSearch(searchKeyword);
    };

    const handleFilterToggle = (category: keyof FilterState, value: string | number) => {
        setFilters((prev) => {
            const categoryFilters = prev[category] as any[];
            const newFilters = categoryFilters.includes(value)
                ? categoryFilters.filter((v) => v !== value)
                : [...categoryFilters, value];

            const updatedFilters = {
                ...prev,
                [category]: newFilters,
            };

            onFilterChange(updatedFilters);
            return updatedFilters;
        });
    };

    // 대분류 펼침/접기 토글
    const toggleTopicExpand = (topic: string) => {
        setExpandedTopics(prev =>
            prev.includes(topic)
                ? prev.filter(t => t !== topic)
                : [...prev, topic]
        );
    };

    const clearFilters = () => {
        const emptyFilters: FilterState = {
            status: [],
            topic: [],
            subTopic: [],
            meetingType: [],
            difficulty: [],
            studyType: [],
            regionId: [],
        };
        setFilters(emptyFilters);
        setExpandedTopics([]);
        onFilterChange(emptyFilters);
    };

    const getActiveFilterCount = () => {
        return Object.values(filters).reduce((acc, curr) => acc + curr.length, 0);
    };

    return (
        <div className="study-filter">
            {/* 검색 및 필터 버튼 행 */}
            <div className="filter-row">
                {/* 스터디 생성 버튼 (강조) */}
                <button
                    className="create-study-btn"
                    onClick={() => navigate('/study/create')}
                    title="새 스터디 만들기"
                >
                    <Plus size={20} />
                    <span>스터디 생성</span>
                </button>

                {/* 검색바 */}
                <form className="search-bar" onSubmit={handleSearchSubmit}>
                    <Search size={20} className="search-icon" />
                    <input
                        type="text"
                        placeholder="스터디 검색..."
                        value={searchKeyword}
                        onChange={handleSearchChange}
                        className="search-input"
                    />
                    {searchKeyword && (
                        <button
                            type="button"
                            className="clear-search-btn"
                            onClick={() => {
                                setSearchKeyword('');
                                onSearch('');
                            }}
                        >
                            <X size={18} />
                        </button>
                    )}
                </form>

                {/* 필터 토글 버튼 */}
                <button className="filter-toggle-btn" onClick={() => setShowFilters(!showFilters)}>
                    <SlidersHorizontal size={20} />
                    <span>필터</span>
                    {getActiveFilterCount() > 0 && (
                        <span className="filter-count">{getActiveFilterCount()}</span>
                    )}
                </button>
            </div>

            {/* 필터 패널 */}
            {showFilters && (
                <div className="filter-panel">
                    <div className="filter-header">
                        <h3>필터</h3>
                        {getActiveFilterCount() > 0 && (
                            <button className="clear-filters-btn" onClick={clearFilters}>
                                초기화
                            </button>
                        )}
                    </div>

                    <div className="filter-sections">
                        {/* 모집 상태 */}
                        <div className="filter-section">
                            <h4 className="filter-section-title">모집 상태</h4>
                            <div className="filter-options">
                                {filterOptions.status.map((option) => (
                                    <label key={option.value} className="filter-option">
                                        <input
                                            type="checkbox"
                                            checked={filters.status.includes(option.value)}
                                            onChange={() => handleFilterToggle('status', option.value)}
                                        />
                                        <span>{option.label}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        {/* 주제 (대분류 + 세부 카테고리) */}
                        <div className="filter-section filter-section-topic">
                            <h4 className="filter-section-title">주제</h4>
                            <div className="filter-topics-grid">
                                {filterOptions.topic.map((option) => (
                                    <div key={option.value} className="filter-topic-group">
                                        <div className="filter-topic-header">
                                            <label className="filter-option filter-option-main">
                                                <input
                                                    type="checkbox"
                                                    checked={filters.topic.includes(option.value)}
                                                    onChange={() => handleFilterToggle('topic', option.value)}
                                                />
                                                <span>{option.label}</span>
                                            </label>
                                            <button
                                                type="button"
                                                className="topic-expand-btn"
                                                onClick={() => toggleTopicExpand(option.value)}
                                            >
                                                {expandedTopics.includes(option.value) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                                            </button>
                                        </div>
                                        {expandedTopics.includes(option.value) && (
                                            <div className="filter-subtopics">
                                                {TOPIC_CATEGORIES[option.value]?.map((subTopic) => (
                                                    <label key={subTopic} className="filter-option filter-option-sub">
                                                        <input
                                                            type="checkbox"
                                                            checked={filters.subTopic.includes(subTopic)}
                                                            onChange={() => handleFilterToggle('subTopic', subTopic)}
                                                        />
                                                        <span>{subTopic}</span>
                                                    </label>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* 미팅 타입 */}
                        <div className="filter-section">
                            <h4 className="filter-section-title">미팅 방식</h4>
                            <div className="filter-options">
                                {filterOptions.meetingType.map((option) => (
                                    <label key={option.value} className="filter-option">
                                        <input
                                            type="checkbox"
                                            checked={filters.meetingType.includes(option.value)}
                                            onChange={() => handleFilterToggle('meetingType', option.value)}
                                        />
                                        <span>{option.label}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        {/* 난이도 */}
                        <div className="filter-section">
                            <h4 className="filter-section-title">난이도</h4>
                            <div className="filter-options">
                                {filterOptions.difficulty.map((option) => (
                                    <label key={option.value} className="filter-option">
                                        <input
                                            type="checkbox"
                                            checked={filters.difficulty.includes(option.value)}
                                            onChange={() => handleFilterToggle('difficulty', option.value)}
                                        />
                                        <span>{option.label}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        {/* 스터디 타입 */}
                        <div className="filter-section">
                            <h4 className="filter-section-title">스터디 타입</h4>
                            <div className="filter-options">
                                {filterOptions.studyType.map((option) => (
                                    <label key={option.value} className="filter-option">
                                        <input
                                            type="checkbox"
                                            checked={filters.studyType.includes(option.value)}
                                            onChange={() => handleFilterToggle('studyType', option.value)}
                                        />
                                        <span>{option.label}</span>
                                    </label>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default StudyFilter;
