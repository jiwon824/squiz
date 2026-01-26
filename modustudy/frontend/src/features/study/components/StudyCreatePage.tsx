import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Info, Calendar, Plus, Trash2, BookOpen, MapPin, AlertCircle, Clock, Users, Target, Shield } from 'lucide-react';
import { MainLayout } from '@/layouts/MainLayout';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { cn } from '@/shared/utils/cn';
import { DateRangePicker } from './DateRangePicker';

interface CurriculumItem {
    session: number;
    description: string;
}

// 시/도 데이터
const CITIES = ['서울특별시', '부산광역시', '대구광역시', '인천광역시', '광주광역시', '대전광역시', '울산광역시', '세종특별자치시', '경기도', '강원도', '충청북도', '충청남도', '전라북도', '전라남도', '경상북도', '경상남도', '제주특별자치도'];

// 구/군 데이터 (간소화)
const DISTRICTS: Record<string, string[]> = {
    '서울특별시': ['강남구', '강동구', '강북구', '강서구', '관악구', '광진구', '구로구', '금천구', '노원구', '도봉구', '동대문구', '동작구', '마포구', '서대문구', '서초구', '성동구', '성북구', '송파구', '양천구', '영등포구', '용산구', '은평구', '종로구', '중구', '중랑구'],
    '부산광역시': ['강서구', '금정구', '남구', '동구', '동래구', '부산진구', '북구', '사상구', '사하구', '서구', '수영구', '연제구', '영도구', '중구', '해운대구', '기장군'],
    '대구광역시': ['남구', '달서구', '동구', '북구', '서구', '수성구', '중구', '달성군'],
    '인천광역시': ['계양구', '남동구', '동구', '미추홀구', '부평구', '서구', '연수구', '중구', '강화군', '옹진군'],
    '광주광역시': ['광산구', '남구', '동구', '북구', '서구'],
    '대전광역시': ['대덕구', '동구', '서구', '유성구', '중구'],
    '울산광역시': ['남구', '동구', '북구', '중구', '울주군'],
    '세종특별자치시': ['세종시'],
    '경기도': ['수원시', '성남시', '고양시', '용인시', '부천시', '안산시', '안양시', '남양주시', '화성시', '평택시', '의정부시', '시흥시', '파주시', '김포시', '광명시', '광주시', '군포시', '하남시', '오산시', '이천시', '안성시', '의왕시', '양평군', '여주시', '과천시', '고양시', '구리시', '포천시', '양주시', '동두천시', '가평군', '연천군'],
    '강원도': ['춘천시', '원주시', '강릉시', '동해시', '태백시', '속초시', '삼척시', '홍천군', '횡성군', '영월군', '평창군', '정선군', '철원군', '화천군', '양구군', '인제군', '고성군', '양양군'],
    '충청북도': ['청주시', '충주시', '제천시', '보은군', '옥천군', '영동군', '증평군', '진천군', '괴산군', '음성군', '단양군'],
    '충청남도': ['천안시', '공주시', '보령시', '아산시', '서산시', '논산시', '계룡시', '당진시', '금산군', '부여군', '서천군', '청양군', '홍성군', '예산군', '태안군'],
    '전라북도': ['전주시', '군산시', '익산시', '정읍시', '남원시', '김제시', '완주군', '진안군', '무주군', '장수군', '임실군', '순창군', '고창군', '부안군'],
    '전라남도': ['목포시', '여수시', '순천시', '나주시', '광양시', '담양군', '곡성군', '구례군', '고흥군', '보성군', '화순군', '장흥군', '강진군', '해남군', '영암군', '무안군', '함평군', '영광군', '장성군', '완도군', '진도군', '신안군'],
    '경상북도': ['포항시', '경주시', '김천시', '안동시', '구미시', '영주시', '영천시', '상주시', '문경시', '경산시', '군위군', '의성군', '청송군', '영양군', '영덕군', '청도군', '고령군', '성주군', '칠곡군', '예천군', '봉화군', '울진군', '울릉군'],
    '경상남도': ['창원시', '진주시', '통영시', '사천시', '김해시', '밀양시', '거제시', '양산시', '의령군', '함안군', '창녕군', '고성군', '남해군', '하동군', '산청군', '함양군', '거창군', '합천군'],
    '제주특별자치도': ['제주시', '서귀포시']
};

// 대분류 → 세부주제 매핑
const TOPIC_SUBTOPICS: Record<string, string[]> = {
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

const StudyCreatePage: React.FC = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        // 기본 정보
        name: '',
        description: '',
        topic: '알고리즘/코딩테스트',
        subTopic: '', // 세부주제
        format: '문제풀이', // 스터디 형식
        difficulty: 'BEGINNER',
        meetingType: 'ONLINE',
        maxMembers: 4,
        totalSessions: 8,
        city: '',
        district: '',

        // 일정 정보
        startDate: null as string | null,
        endDate: null as string | null,
        recruitStartDate: null as string | null, // 모집 시작일
        recruitEndDate: null as string | null,   // 모집 종료일
        scheduleDays: [] as string[],            // 정기 모임 요일
        scheduleTime: '19:00',                   // 정기 모임 시간

        // 스터디 설정
        studyType: 'PLANNED',
        isPublic: true,                          // 공개 여부
        penaltyPolicy: 'NORMAL',                 // 패널티 정책

        // 추가 정보
        goal: '',                                // 스터디 목표
        textbook: '',                            // 교재/자료
        prerequisites: '',                       // 선행 조건
        processDetail: '',                       // 진행 방식 상세
        targetOrgType: '',                       // 대상 소속 타입

        // 커리큘럼
        hasCurriculum: false,
        curriculum: [{ session: 1, description: '' }] as CurriculumItem[]
    });

    // 요일 토글 핸들러
    const handleDayToggle = (day: string) => {
        setFormData(prev => ({
            ...prev,
            scheduleDays: prev.scheduleDays.includes(day)
                ? prev.scheduleDays.filter(d => d !== day)
                : [...prev.scheduleDays, day]
        }));
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleOptionToggle = (name: string, value: string | boolean) => {
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleCityChange = (city: string) => {
        setFormData(prev => ({ ...prev, city, district: '' }));
    };

    const handleTopicChange = (topic: string) => {
        setFormData(prev => ({ ...prev, topic, subTopic: '' }));
    };

    const handleDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => ({ ...prev, startDate: start, endDate: end }));
    };

    const handleRecruitDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => ({ ...prev, recruitStartDate: start, recruitEndDate: end }));
    };

    const handleCurriculumChange = (index: number, value: string) => {
        const newCurriculum = [...formData.curriculum];
        newCurriculum[index].description = value;
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const addSession = () => {
        setFormData(prev => ({
            ...prev,
            curriculum: [...prev.curriculum, { session: prev.curriculum.length + 1, description: '' }]
        }));
    };

    const removeSession = (index: number) => {
        const newCurriculum = formData.curriculum
            .filter((_, i) => i !== index)
            .map((item, i) => ({ ...item, session: i + 1 }));
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('Form submitted:', formData);
        alert('스터디가 생성되었습니다! (모의)');
        navigate('/study');
    };

    // 스타일 정의
    const styles = {
        container: "max-w-4xl mx-auto px-4 py-8",
        header: "mb-8",
        card: "bg-white rounded-2xl border border-gray-200 shadow-sm p-6 md:p-8",
        section: "space-y-4",
        sectionTitle: "flex items-center gap-2 text-lg font-bold text-gray-800",
        sectionIcon: "text-primary",
        label: "block text-sm font-semibold text-gray-700 mb-1.5",
        selectWrapper: "relative",
        select: "w-full p-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none appearance-none text-gray-800",
        selectIcon: "absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400",
        textarea: "w-full p-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none min-h-[120px] resize-none text-base text-gray-800",
        toggleGroup: "flex gap-2 p-1 bg-gray-100 rounded-xl",
        toggleBtn: (active: boolean) => cn(
            "flex-1 py-2.5 text-sm font-semibold rounded-lg transition-all text-center",
            active ? "bg-white text-primary shadow-sm" : "text-gray-500 hover:text-gray-700"
        ),
        curriculumItem: "flex gap-3 items-start",
        curriculumBadge: "shrink-0 w-14 h-11 flex items-center justify-center bg-primary/10 text-primary text-sm font-bold rounded-xl",
        deleteBtn: "shrink-0 p-2.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all",
        footer: "flex flex-col sm:flex-row gap-3 pt-6 border-t border-gray-100",
    };

    return (
        <MainLayout>
            <div className={styles.container}>
                {/* 헤더 */}
                <header className={styles.header}>
                    <Button
                        variant="ghost"
                        onClick={() => navigate(-1)}
                        leftIcon={<ChevronLeft size={20} />}
                        className="text-gray-500 hover:text-gray-800 mb-4 -ml-2"
                    >
                        뒤로가기
                    </Button>
                    <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
                        새로운 스터디 시작하기
                    </h1>
                    <p className="text-gray-500">
                        함께 성장할 팀원을 모집해보세요.
                    </p>
                </header>

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* 기본 정보 카드 */}
                    <div className={styles.card}>
                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>
                                <Info size={20} className={styles.sectionIcon} />
                                기본 정보
                            </h2>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                <div className="md:col-span-2">
                                    <Input
                                        label="스터디 이름"
                                        name="name"
                                        placeholder="모두가 이해하기 쉬운 이름을 지어주세요"
                                        required
                                        value={formData.name}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div className="md:col-span-2">
                                    <label className={styles.label}>스터디 설명</label>
                                    <textarea
                                        name="description"
                                        className={styles.textarea}
                                        placeholder="스터디의 목표와 진행 방식에 대해 자세히 적어주세요"
                                        required
                                        value={formData.description}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div>
                                    <label className={styles.label}>주제</label>
                                    <div className={styles.selectWrapper}>
                                        <select
                                            name="topic"
                                            className={styles.select}
                                            value={formData.topic}
                                            onChange={(e) => handleTopicChange(e.target.value)}
                                        >
                                            <option value="알고리즘/코딩테스트">알고리즘/코딩테스트</option>
                                            <option value="CS 기초">CS 기초</option>
                                            <option value="프론트엔드">프론트엔드</option>
                                            <option value="백엔드">백엔드</option>
                                            <option value="인프라/DevOps">인프라/DevOps</option>
                                            <option value="AI/ML">AI/ML</option>
                                            <option value="모바일">모바일</option>
                                            <option value="자격증">자격증</option>
                                            <option value="취업 준비">취업 준비</option>
                                            <option value="프로젝트">프로젝트</option>
                                        </select>
                                        <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                    </div>
                                </div>

                                <div>
                                    <label className={styles.label}>세부 주제</label>
                                    <div className={styles.selectWrapper}>
                                        <select
                                            name="subTopic"
                                            className={styles.select}
                                            value={formData.subTopic}
                                            onChange={handleChange}
                                        >
                                            <option value="">세부 주제 선택 (선택사항)</option>
                                            {TOPIC_SUBTOPICS[formData.topic]?.map((sub) => (
                                                <option key={sub} value={sub}>{sub}</option>
                                            ))}
                                        </select>
                                        <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                    </div>
                                </div>

                                <div>
                                    <label className={styles.label}>최대 인원</label>
                                    <div className="flex items-center justify-center gap-2">
                                        <button
                                            type="button"
                                            onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.max(2, prev.maxMembers - 1) }))}
                                            className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                        >
                                            −
                                        </button>
                                        <div className="flex-1 text-center py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-bold text-gray-800">
                                            {formData.maxMembers}명
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.min(50, prev.maxMembers + 1) }))}
                                            className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                        >
                                            +
                                        </button>
                                    </div>
                                </div>

                                <div>
                                    <label className={styles.label}>총 스터디 횟수</label>
                                    <div className="flex items-center justify-center gap-2">
                                        <button
                                            type="button"
                                            onClick={() => setFormData(prev => ({ ...prev, totalSessions: Math.max(1, prev.totalSessions - 1) }))}
                                            className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                        >
                                            −
                                        </button>
                                        <div className="flex-1 text-center py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-bold text-gray-800">
                                            {formData.totalSessions}회
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => setFormData(prev => ({ ...prev, totalSessions: Math.min(100, prev.totalSessions + 1) }))}
                                            className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                        >
                                            +
                                        </button>
                                    </div>
                                </div>

                                <div>
                                    <label className={styles.label}>권장 난이도</label>
                                    <div className={styles.toggleGroup}>
                                        {['BEGINNER', 'INTERMEDIATE', 'ADVANCED'].map((level) => (
                                            <button
                                                key={level}
                                                type="button"
                                                className={styles.toggleBtn(formData.difficulty === level)}
                                                onClick={() => handleOptionToggle('difficulty', level)}
                                            >
                                                {level === 'BEGINNER' && '입문'}
                                                {level === 'INTERMEDIATE' && '중급'}
                                                {level === 'ADVANCED' && '고급'}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <label className={styles.label}>진행 방식</label>
                                    <div className={styles.toggleGroup}>
                                        {['ONLINE', 'OFFLINE', 'HYBRID'].map((type) => (
                                            <button
                                                key={type}
                                                type="button"
                                                className={styles.toggleBtn(formData.meetingType === type)}
                                                onClick={() => handleOptionToggle('meetingType', type)}
                                            >
                                                {type === 'ONLINE' && '온라인'}
                                                {type === 'OFFLINE' && '오프라인'}
                                                {type === 'HYBRID' && '혼합'}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* 오프라인 위치 선택 */}
                            {(formData.meetingType === 'OFFLINE' || formData.meetingType === 'HYBRID') && (
                                <div className="mt-4 p-4 bg-gray-50 rounded-xl border border-gray-200">
                                    <div className="flex items-center gap-2 mb-3">
                                        <MapPin size={18} className="text-primary" />
                                        <span className="font-semibold text-gray-800">오프라인 모임 장소</span>
                                    </div>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                        <div>
                                            <label className={styles.label}>시/도</label>
                                            <div className={styles.selectWrapper}>
                                                <select
                                                    name="city"
                                                    className={styles.select}
                                                    value={formData.city}
                                                    onChange={(e) => handleCityChange(e.target.value)}
                                                >
                                                    <option value="">시/도 선택</option>
                                                    {CITIES.map((city) => (
                                                        <option key={city} value={city}>{city}</option>
                                                    ))}
                                                </select>
                                                <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                            </div>
                                        </div>
                                        <div>
                                            <label className={styles.label}>구/군</label>
                                            <div className={styles.selectWrapper}>
                                                <select
                                                    name="district"
                                                    className={styles.select}
                                                    value={formData.district}
                                                    onChange={handleChange}
                                                    disabled={!formData.city}
                                                >
                                                    <option value="">구/군 선택</option>
                                                    {formData.city && DISTRICTS[formData.city]?.map((district) => (
                                                        <option key={district} value={district}>{district}</option>
                                                    ))}
                                                </select>
                                                <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* 일정 및 모집 카드 */}
                    <div className={styles.card}>
                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>
                                <Clock size={20} className={styles.sectionIcon} />
                                일정 및 모집
                            </h2>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                {/* 정기 모임 요일 */}
                                <div className="md:col-span-2">
                                    <label className={styles.label}>정기 모임 요일</label>
                                    <div className="flex gap-2 flex-wrap">
                                        {[
                                            { key: 'MON', label: '월' },
                                            { key: 'TUE', label: '화' },
                                            { key: 'WED', label: '수' },
                                            { key: 'THU', label: '목' },
                                            { key: 'FRI', label: '금' },
                                            { key: 'SAT', label: '토' },
                                            { key: 'SUN', label: '일' }
                                        ].map(({ key, label }) => (
                                            <button
                                                key={key}
                                                type="button"
                                                onClick={() => handleDayToggle(key)}
                                                className={cn(
                                                    "w-11 h-11 rounded-xl font-semibold transition-all",
                                                    formData.scheduleDays.includes(key)
                                                        ? "bg-primary text-white"
                                                        : "bg-gray-100 text-gray-500 hover:bg-gray-200"
                                                )}
                                            >
                                                {label}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* 정기 모임 시간 */}
                                <div>
                                    <label className={styles.label}>정기 모임 시간</label>
                                    <Input
                                        type="time"
                                        name="scheduleTime"
                                        value={formData.scheduleTime}
                                        onChange={handleChange}
                                    />
                                </div>

                                {/* 스터디 타입 */}
                                <div>
                                    <label className={styles.label}>스터디 타입</label>
                                    <div className={styles.toggleGroup}>
                                        {['PLANNED', 'LIGHTNING'].map((type) => (
                                            <button
                                                key={type}
                                                type="button"
                                                className={styles.toggleBtn(formData.studyType === type)}
                                                onClick={() => handleOptionToggle('studyType', type)}
                                            >
                                                {type === 'PLANNED' && '계획형'}
                                                {type === 'LIGHTNING' && '번개형'}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* 스터디 형식 */}
                                <div>
                                    <label className={styles.label}>스터디 형식</label>
                                    <div className={styles.selectWrapper}>
                                        <select
                                            name="format"
                                            className={styles.select}
                                            value={formData.format}
                                            onChange={handleChange}
                                        >
                                            <option value="문제풀이">문제풀이</option>
                                            <option value="독서">독서</option>
                                            <option value="프로젝트">프로젝트</option>
                                            <option value="강의">강의</option>
                                            <option value="토론">토론</option>
                                            <option value="코드리뷰">코드리뷰</option>
                                            <option value="기타">기타</option>
                                        </select>
                                        <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                    </div>
                                </div>

                                {/* 공개 여부 */}
                                <div>
                                    <label className={styles.label}>공개 여부</label>
                                    <div className={styles.toggleGroup}>
                                        <button
                                            type="button"
                                            className={styles.toggleBtn(formData.isPublic === true)}
                                            onClick={() => handleOptionToggle('isPublic', true)}
                                        >
                                            공개
                                        </button>
                                        <button
                                            type="button"
                                            className={styles.toggleBtn(formData.isPublic === false)}
                                            onClick={() => handleOptionToggle('isPublic', false)}
                                        >
                                            비공개
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {/* 모집 기간 */}
                            <div className="mt-6 p-4 bg-blue-50 rounded-xl border border-blue-200">
                                <div className="flex items-center gap-2 mb-3">
                                    <Users size={18} className="text-blue-600" />
                                    <span className="font-semibold text-blue-800">모집 기간</span>
                                </div>
                                <p className="text-sm text-blue-600 mb-3">스터디 멤버 모집 시작일과 종료일을 설정하세요.</p>
                                <DateRangePicker
                                    startDate={formData.recruitStartDate}
                                    endDate={formData.recruitEndDate}
                                    onRangeChange={handleRecruitDateRangeChange}
                                />
                            </div>
                        </div>
                    </div>

                    {/* 커리큘럼 등록 여부 카드 */}
                    <div className={styles.card}>
                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>
                                <BookOpen size={20} className={styles.sectionIcon} />
                                커리큘럼 설정
                            </h2>
                            <p className="text-sm text-gray-500 mt-1">
                                미리 커리큘럼을 등록하시겠습니까?
                            </p>

                            <div className="mt-4">
                                <div className={styles.toggleGroup}>
                                    <button
                                        type="button"
                                        className={styles.toggleBtn(formData.hasCurriculum === true)}
                                        onClick={() => handleOptionToggle('hasCurriculum', true)}
                                    >
                                        예, 등록할게요
                                    </button>
                                    <button
                                        type="button"
                                        className={styles.toggleBtn(formData.hasCurriculum === false)}
                                        onClick={() => handleOptionToggle('hasCurriculum', false)}
                                    >
                                        아니오, 나중에 할게요
                                    </button>
                                </div>
                            </div>

                            {/* 아니오 선택 시 안내 메시지 */}
                            {!formData.hasCurriculum && (
                                <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-xl">
                                    <div className="flex items-start gap-3">
                                        <AlertCircle size={20} className="text-amber-500 shrink-0 mt-0.5" />
                                        <div>
                                            <p className="font-semibold text-amber-800">일정 등록 안내</p>
                                            <p className="text-sm text-amber-700 mt-1">
                                                스터디가 확정되면 <strong>출석 체크</strong>를 위해 반드시 일정을 등록해주셔야 합니다.
                                                스터디 상세 페이지에서 언제든지 일정을 추가할 수 있습니다.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* 커리큘럼 등록 선택 시에만 표시 */}
                    {formData.hasCurriculum && (
                        <>
                            {/* 스터디 기간 카드 */}
                            <div className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Calendar size={20} className={styles.sectionIcon} />
                                        스터디 기간
                                    </h2>
                                    <p className="text-sm text-gray-500 mt-1">
                                        스터디 시작일과 종료일을 선택해주세요. 날짜를 두 번 클릭하여 범위를 지정합니다.
                                    </p>

                                    <div className="mt-4">
                                        <DateRangePicker
                                            startDate={formData.startDate}
                                            endDate={formData.endDate}
                                            onRangeChange={handleDateRangeChange}
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 커리큘럼 카드 */}
                            <div className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <BookOpen size={20} className={styles.sectionIcon} />
                                        커리큘럼 계획
                                    </h2>
                                    <p className="text-sm text-gray-500 mt-1">
                                        회차별 학습 목표를 설정하면 참여자들이 스터디 방향을 파악하는 데 도움이 됩니다.
                                    </p>

                                    <div className="space-y-3 mt-4">
                                        {formData.curriculum.map((item, index) => (
                                            <div key={index} className={styles.curriculumItem}>
                                                <div className={styles.curriculumBadge}>
                                                    {item.session}회차
                                                </div>
                                                <div className="flex-1">
                                                    <Input
                                                        placeholder={`${item.session}회차 학습 목표나 주제를 입력하세요`}
                                                        value={item.description}
                                                        onChange={(e) => handleCurriculumChange(index, e.target.value)}
                                                    />
                                                </div>
                                                {formData.curriculum.length > 1 && (
                                                    <button
                                                        type="button"
                                                        className={styles.deleteBtn}
                                                        onClick={() => removeSession(index)}
                                                    >
                                                        <Trash2 size={18} />
                                                    </button>
                                                )}
                                            </div>
                                        ))}

                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={addSession}
                                            fullWidth
                                            leftIcon={<Plus size={18} />}
                                            className="border-dashed mt-2"
                                        >
                                            회차 추가하기
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        </>
                    )}

                    {/* 추가 정보 카드 */}
                    <div className={styles.card}>
                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>
                                <Target size={20} className={styles.sectionIcon} />
                                추가 정보
                            </h2>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                <div className="md:col-span-2">
                                    <Input
                                        label="스터디 목표"
                                        name="goal"
                                        placeholder="예: 골드 티어 달성, 토이 프로젝트 완성"
                                        value={formData.goal}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div>
                                    <Input
                                        label="교재/자료 (선택)"
                                        name="textbook"
                                        placeholder="예: 백준 온라인 저지, Do It! 알고리즘"
                                        value={formData.textbook}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div className="md:col-span-2">
                                    <label className={styles.label}>진행 방식 상세 (선택)</label>
                                    <textarea
                                        name="processDetail"
                                        className={styles.textarea}
                                        placeholder="예: 매주 월/수/금 19:00-21:00, 문제 풀이 후 코드 리뷰 진행"
                                        value={formData.processDetail}
                                        onChange={handleChange}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>



                    {/* 스터디 규칙 카드 */}
                    <div className={styles.card}>
                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>
                                <Shield size={20} className={styles.sectionIcon} />
                                스터디 규칙
                            </h2>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                {/* 패널티 정책 */}
                                <div className="md:col-span-2">
                                    <label className={styles.label}>패널티 정책</label>
                                    <div className="grid grid-cols-5 gap-2">
                                        {[
                                            { key: 'STRICT', label: '엄격', desc: '결석 시 강퇴' },
                                            { key: 'NORMAL', label: '보통', desc: '경고 후 조치' },
                                            { key: 'LENIENT', label: '관대', desc: '유연하게 운영' },
                                            { key: 'RATIO', label: '비율', desc: '출석률 기반' },
                                            { key: 'NONE', label: '없음', desc: '자율 참여' }
                                        ].map(({ key, label, desc }) => (
                                            <button
                                                key={key}
                                                type="button"
                                                onClick={() => handleOptionToggle('penaltyPolicy', key)}
                                                className={cn(
                                                    "p-3 rounded-xl text-center transition-all border-2",
                                                    formData.penaltyPolicy === key
                                                        ? "border-primary bg-primary/10 text-primary"
                                                        : "border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300"
                                                )}
                                            >
                                                <div className="font-bold text-sm">{label}</div>
                                                <div className="text-xs mt-1 opacity-70">{desc}</div>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* 대상 소속 */}
                                <div>
                                    <label className={styles.label}>대상 소속 (선택)</label>
                                    <div className={styles.selectWrapper}>
                                        <select
                                            name="targetOrgType"
                                            className={styles.select}
                                            value={formData.targetOrgType}
                                            onChange={handleChange}
                                        >
                                            <option value="">제한 없음</option>
                                            <option value="SSAFY">SSAFY</option>
                                            <option value="NBC">내일배움캠프</option>
                                            <option value="WTC">우아한테크코스</option>
                                            <option value="KRAFTON">크래프톤 정글</option>
                                            <option value="OTHER">기타</option>
                                        </select>
                                        <ChevronLeft size={16} className={cn(styles.selectIcon, "-rotate-90")} />
                                    </div>
                                </div>

                                {/* 선행 조건 */}
                                <div>
                                    <Input
                                        label="선행 조건 (선택)"
                                        name="prerequisites"
                                        placeholder="예: Python 기초 필수"
                                        value={formData.prerequisites}
                                        onChange={handleChange}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 하단 버튼 */}
                    <div className={styles.footer}>
                        <Button
                            type="button"
                            variant="outline"
                            size="lg"
                            className="flex-1 sm:flex-none sm:min-w-[120px]"
                            onClick={() => navigate(-1)}
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            className="flex-1 sm:flex-none sm:min-w-[200px] shadow-lg shadow-primary/20"
                        >
                            스터디 개설하기
                        </Button>
                    </div>
                </form>
            </div>
        </MainLayout>
    );
};

export default StudyCreatePage;
