import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Zap, MapPin, Users, Calendar } from 'lucide-react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { DatePicker } from './DatePicker';
import { TimePicker } from './TimePicker';

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

const styles = {
    card: 'bg-white rounded-2xl shadow-sm border border-gray-100',
    section: 'p-6',
    sectionTitle: 'text-lg font-bold text-gray-800 flex items-center gap-2',
    sectionIcon: 'text-amber-500',
    label: 'block text-sm font-semibold text-gray-700 mb-2',
    selectWrapper: 'relative',
    select: 'w-full appearance-none bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 pr-10 text-gray-700 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all',
    selectIcon: 'absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none',
    textarea: "w-full p-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition-all outline-none min-h-[120px] resize-none text-base text-gray-800",
    toggleGroup: 'flex gap-2 p-1 bg-gray-100 rounded-xl',
    toggleBtn: (active: boolean) => cn(
        'flex-1 py-2.5 text-sm font-semibold rounded-lg transition-all text-center',
        active
            ? 'bg-white text-amber-600 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'
    ),
    footer: "flex flex-col sm:flex-row sm:justify-end gap-3 pt-6 border-t border-gray-100"
};

const LightningStudyCreatePage: React.FC = () => {
    const navigate = useNavigate();

    // 오늘 날짜 계산
    const today = new Date();
    const formattedToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    const [formData, setFormData] = useState({
        // 기본 정보
        name: '',
        intro: '',
        description: '',
        format: '문제 풀이', // 스터디 형식
        topic: '알고리즘/코딩테스트',
        subTopic: '',
        meetingType: 'ONLINE',
        maxMembers: 4,
        city: '',
        district: '',

        // 일정 정보 (1회성)
        meetingDate: '',
        meetingTime: '19:00',
        duration: '2', // 예상 소요 시간 (시간)

        // 공개 여부
        isPublic: true,

        // 추가 정보
        goal: '',
    });

    // Navigation Logic
    const [activeSection, setActiveSection] = useState('basic-info');
    const sections = [
        { id: 'basic-info', label: '기본 정보' },
        { id: 'schedule-info', label: '모임 일정' },
        { id: 'additional-info', label: '추가 정보' },
    ];

    const scrollToSection = (id: string) => {
        const element = document.getElementById(id);
        const container = document.getElementById('main-content-scroll');
        if (element && container) {
            const offset = 24; // 여유 공간
            const elementRect = element.getBoundingClientRect().top;
            const containerRect = container.getBoundingClientRect().top;
            const offsetPosition = elementRect - containerRect + container.scrollTop - offset;

            container.scrollTo({
                top: offsetPosition,
                behavior: 'smooth'
            });
        }
    };

    useEffect(() => {
        const container = document.getElementById('main-content-scroll');

        const handleScroll = () => {
            if (!container) return;

            const scrollPosition = container.scrollTop + 100; // 트리거 포인트 조정

            for (const section of sections) {
                const element = document.getElementById(section.id);
                if (element) {
                    const elementRect = element.getBoundingClientRect().top;
                    const containerRect = container.getBoundingClientRect().top;
                    const relativeTop = elementRect - containerRect + container.scrollTop;
                    const height = element.offsetHeight;

                    if (scrollPosition >= relativeTop && scrollPosition < relativeTop + height) {
                        setActiveSection(section.id);
                    }
                }
            }
        };

        if (container) {
            container.addEventListener('scroll', handleScroll);
            handleScroll(); // 초기 상태 설정
        }

        return () => {
            if (container) {
                container.removeEventListener('scroll', handleScroll);
            }
        };
    }, []);

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

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('번개 스터디 생성:', { ...formData, studyType: 'LIGHTNING' });
        // TODO: API 호출
        navigate('/study');
    };

    return (
        <UserLayoutV2>
            <div className="max-w-6xl mx-auto px-4 py-8">
                {/* 헤더 */}
                <div className="mb-8">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={() => navigate('/study/create')}
                            className="p-2 hover:bg-gray-100 rounded-xl transition-colors"
                        >
                            <ChevronLeft size={24} />
                        </button>
                        <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
                            <Zap size={22} className="text-amber-500" />
                        </div>
                        <h1 className="text-2xl font-bold text-gray-800">번개 스터디 개설</h1>
                    </div>
                    <p className="text-gray-500 text-sm mt-0.5 ml-16">1회성 빠른 스터디를 만들어보세요</p>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="flex flex-col lg:flex-row gap-8 items-start relative">
                        <div className="flex-1 min-w-0 space-y-6">
                            {/* 기본 정보 카드 */}
                            <div id="basic-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Zap size={20} className={styles.sectionIcon} />
                                        기본 정보
                                    </h2>

                                    <div className="space-y-4 mt-4">
                                        <Input
                                            label="스터디 이름"
                                            name="name"
                                            placeholder="예: 백준 골드 문제풀이 번개"
                                            value={formData.name}
                                            onChange={handleChange}
                                            required
                                        />

                                        <Input
                                            label="한줄 소개 (썸네일용)"
                                            name="intro"
                                            placeholder="스터디를 한 문장으로 소개해 주세요"
                                            value={formData.intro}
                                            onChange={handleChange}
                                            required
                                        />

                                        <div>
                                            <label className={styles.label}>상세 설명</label>
                                            <textarea
                                                name="description"
                                                className={styles.textarea}
                                                placeholder="스터디에 대해 간단히 소개해주세요"
                                                value={formData.description}
                                                onChange={handleChange}
                                            />
                                        </div>

                                        {/* 스터디 형식 */}
                                        <div>
                                            <label className={styles.label}>스터디 형식</label>
                                            <div className="grid grid-cols-4 gap-2">
                                                {['문제 풀이', '독서/책 스터디', '강의 수강', '프로젝트', '모의 면접', '코드 리뷰', '발표/세미나', '토론'].map((format) => (
                                                    <button
                                                        key={format}
                                                        type="button"
                                                        onClick={() => handleOptionToggle('format', format)}
                                                        className={cn(
                                                            "py-2.5 px-3 rounded-xl text-center transition-all border-2 text-sm font-medium",
                                                            formData.format === format
                                                                ? "border-amber-400 bg-amber-50 text-amber-700"
                                                                : "border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300"
                                                        )}
                                                    >
                                                        {format}
                                                    </button>
                                                ))}
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                            <div>
                                                <Select
                                                    label="주제"
                                                    value={formData.topic}
                                                    onChange={handleTopicChange}
                                                    options={Object.keys(TOPIC_SUBTOPICS)}
                                                />
                                            </div>
                                            <div>
                                                <Select
                                                    label="세부 주제"
                                                    value={formData.subTopic}
                                                    onChange={(val) => setFormData(prev => ({ ...prev, subTopic: val }))}
                                                    options={TOPIC_SUBTOPICS[formData.topic] || []}
                                                    placeholder="세부 주제 선택"
                                                />
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                            <div>
                                                <label className={styles.label}>모집 인원</label>
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
                                                        onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.min(20, prev.maxMembers + 1) }))}
                                                        className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                                    >
                                                        +
                                                    </button>
                                                </div>
                                            </div>

                                            <div>
                                                <label className={styles.label}>진행 방식</label>
                                                <div className={styles.toggleGroup}>
                                                    {['ONLINE', 'OFFLINE'].map((type) => (
                                                        <button
                                                            key={type}
                                                            type="button"
                                                            className={styles.toggleBtn(formData.meetingType === type)}
                                                            onClick={() => handleOptionToggle('meetingType', type)}
                                                        >
                                                            {type === 'ONLINE' && '온라인'}
                                                            {type === 'OFFLINE' && '오프라인'}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>

                                        {/* 오프라인 위치 선택 */}
                                        {formData.meetingType === 'OFFLINE' && (
                                            <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                                                <div className="flex items-center gap-2 mb-3">
                                                    <MapPin size={18} className="text-amber-500" />
                                                    <span className="font-semibold text-gray-800">모임 장소</span>
                                                </div>
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                                    <div>
                                                        <Select
                                                            label="시/도"
                                                            value={formData.city}
                                                            onChange={handleCityChange}
                                                            options={CITIES}
                                                            placeholder="시/도 선택"
                                                        />
                                                    </div>
                                                    <div>
                                                        <Select
                                                            label="구/군"
                                                            value={formData.district}
                                                            onChange={(val) => setFormData(prev => ({ ...prev, district: val }))}
                                                            options={formData.city ? DISTRICTS[formData.city] || [] : []}
                                                            placeholder="구/군 선택"
                                                            disabled={!formData.city}
                                                        />
                                                    </div>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* 일정 정보 카드 */}
                            <div id="schedule-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Calendar size={20} className={styles.sectionIcon} />
                                        모임 일정
                                    </h2>

                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
                                        <div>
                                            <label className={styles.label}>모임 날짜</label>
                                            <DatePicker
                                                value={formData.meetingDate}
                                                onChange={(date) => setFormData(prev => ({ ...prev, meetingDate: date }))}
                                                min={formattedToday}
                                                placeholder="날짜 선택"
                                            />
                                        </div>

                                        <div>
                                            <label className={styles.label}>모임 시간</label>
                                            <TimePicker
                                                value={formData.meetingTime}
                                                onChange={(time) => setFormData(prev => ({ ...prev, meetingTime: time }))}
                                            />
                                        </div>

                                        <div>
                                            <Select
                                                label="예상 소요 시간"
                                                value={formData.duration}
                                                onChange={(val) => setFormData(prev => ({ ...prev, duration: val }))}
                                                options={[
                                                    { value: '1', label: '1시간' },
                                                    { value: '2', label: '2시간' },
                                                    { value: '3', label: '3시간' },
                                                    { value: '4', label: '4시간 이상' }
                                                ]}
                                            />
                                        </div>
                                    </div>

                                    <div className="mt-4">
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
                            </div>

                            {/* 추가 정보 카드 */}
                            <div id="additional-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Users size={20} className={styles.sectionIcon} />
                                        추가 정보(선택)
                                    </h2>

                                    <div className="mt-4">
                                        <Input
                                            label="스터디 목표"
                                            name="goal"
                                            placeholder="예: 백준 골드 문제 3개 함께 풀기"
                                            value={formData.goal}
                                            onChange={handleChange}
                                        />
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
                                    onClick={() => navigate('/study/create')}
                                >
                                    취소
                                </Button>
                                <Button
                                    type="submit"
                                    size="lg"
                                    className="flex-1 sm:flex-none sm:min-w-[200px] bg-amber-500 hover:bg-amber-600 shadow-lg shadow-amber-500/20"
                                >
                                    <Zap size={18} className="mr-2" />
                                    번개 스터디 개설
                                </Button>
                            </div>
                        </div>

                        {/* 우측 네비게이션 */}
                        <aside className="hidden lg:block w-48 shrink-0 sticky top-24">
                            <div className="space-y-1">
                                <div className="px-3 py-2 text-xs font-bold text-gray-400 uppercase tracking-wider">
                                    목차
                                </div>
                                {sections.map(section => (
                                    <button
                                        key={section.id}
                                        type="button"
                                        onClick={() => scrollToSection(section.id)}
                                        className={cn(
                                            "w-full text-left px-3 py-2 text-sm rounded-lg transition-all",
                                            activeSection === section.id
                                                ? "bg-amber-50 text-amber-600 font-bold"
                                                : "text-gray-500 hover:text-gray-900 hover:bg-gray-100"
                                        )}
                                    >
                                        {section.label}
                                    </button>
                                ))}
                            </div>
                        </aside>
                    </div>
                </form>
            </div>
        </UserLayoutV2>
    );
};

export default LightningStudyCreatePage;
