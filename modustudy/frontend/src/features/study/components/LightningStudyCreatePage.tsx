import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Zap, MapPin, Users, Calendar } from 'lucide-react';
import { PageNavHeader } from '@/shared/components/layouts/PageNavHeader';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { DatePicker } from './DatePicker';
import { TimePicker } from './TimePicker';
import { getTopics, getFormats, createStudy, getProvinces, getDistricts, type TopicParent, type FormatItem, type StudyCreatePayload, type RegionItem } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';
import { getErrorMessage } from '@/shared/utils/errorUtils';


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
    const { showToast } = useUIStore();

    // 오늘 날짜 계산
    const today = new Date();
    const formattedToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    // 주제 및 형식 목록 상태
    const [topics, setTopics] = useState<TopicParent[]>([]);
    const [formats, setFormats] = useState<FormatItem[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // 지역 목록 상태 (API에서 가져옴)
    const [provinces, setProvinces] = useState<RegionItem[]>([]);
    const [districts, setDistricts] = useState<RegionItem[]>([]);

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
        provinceId: null as number | null,
        districtId: null as number | null,

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

    // 주제, 형식, 지역 목록 불러오기
    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const [topicsData, formatsData, provincesData] = await Promise.all([
                    getTopics(),
                    getFormats(),
                    getProvinces(),
                ]);
                setTopics(topicsData);
                setFormats(formatsData);
                setProvinces(provincesData);
            } catch (error) {
                console.error('주제/형식/지역 목록 불러오기 실패:', error);
                showToast('데이터를 불러오는데 실패했습니다.', 'error');
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, []);

    // 시/도 변경 시 구/군 목록 가져오기
    useEffect(() => {
        const fetchDistricts = async () => {
            if (formData.provinceId) {
                try {
                    const districtsData = await getDistricts(formData.provinceId);
                    setDistricts(districtsData);
                } catch (error) {
                    console.error('구/군 목록 불러오기 실패:', error);
                    setDistricts([]);
                }
            } else {
                setDistricts([]);
            }
        };
        fetchDistricts();
    }, [formData.provinceId]);

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

    const handleProvinceChange = (provinceId: number | null) => {
        setFormData(prev => ({ ...prev, provinceId, districtId: null }));
    };

    const handleTopicChange = (topic: string) => {
        setFormData(prev => ({ ...prev, topic, subTopic: '' }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // 필수 필드 검증
        if (!formData.name.trim()) {
            showToast('스터디 이름을 입력해주세요.', 'error');
            return;
        }
        if (!formData.meetingDate) {
            showToast('모임 날짜를 선택해주세요.', 'error');
            return;
        }

        // 오프라인 모임 시 지역 선택 필수
        if (formData.meetingType === 'OFFLINE' && !formData.districtId) {
            showToast('오프라인 모임은 지역 선택이 필요합니다.', 'error');
            return;
        }

        setIsSubmitting(true);
        try {
            // topic과 format을 ID로 매핑
            let topicId: number | undefined;
            let formatId: number | undefined;

            // 세부 주제 ID 찾기
            if (formData.topic && formData.subTopic) {
                const parentTopic = topics.find(t => t.name === formData.topic);
                if (parentTopic) {
                    const childTopic = parentTopic.children.find(c => c.name === formData.subTopic);
                    if (childTopic) {
                        topicId = childTopic.id;
                    }
                }
            }

            // 형식 ID 찾기
            if (formData.format) {
                const foundFormat = formats.find(f => f.name === formData.format);
                if (foundFormat) {
                    formatId = foundFormat.id;
                }
            }

            // topicId 필수 검증
            if (!topicId) {
                showToast('세부 주제를 선택해주세요.', 'error');
                setIsSubmitting(false);
                return;
            }

            // 번개 스터디는 1회성이므로 startDate = endDate = meetingDate
            const startDate = formData.meetingDate;
            const endDate = formData.meetingDate;

            // 모집 기간 설정 (즉시 모집 시작)
            const today = new Date();
            const recruitStartDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

            // 모집 종료일 설정 (시간대 문제 방지를 위해 문자열 직접 파싱)
            const meetingDateParts = formData.meetingDate.split('-').map(Number);
            const meetingDateObj = new Date(meetingDateParts[0], meetingDateParts[1] - 1, meetingDateParts[2]);
            meetingDateObj.setDate(meetingDateObj.getDate() - 1);
            let recruitEndDate = `${meetingDateObj.getFullYear()}-${String(meetingDateObj.getMonth() + 1).padStart(2, '0')}-${String(meetingDateObj.getDate()).padStart(2, '0')}`;

            // 당일 모임인 경우 모집 종료일이 모집 시작일보다 앞서면 모임 당일로 설정
            if (recruitEndDate < recruitStartDate) {
                recruitEndDate = formData.meetingDate;
            }

            // 모집 상태 결정 (문자열 비교로 시간대 문제 방지)
            const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
            let status = 'RECRUITING'; // 기본값: 모집중
            if (recruitStartDate > todayStr) {
                status = 'PENDING'; // 모집시작일이 미래면 대기중
            }

            const payload: StudyCreatePayload = {
                name: formData.name,
                intro: formData.intro || undefined,
                description: formData.description || undefined,
                topicId,
                formatId,
                studyType: 'LIGHTNING',
                meetingType: formData.meetingType,
                regionId: formData.districtId || undefined, // 오프라인일 때만 설정
                scheduleTime: formData.meetingTime || undefined,
                maxMembers: formData.maxMembers,
                isPublic: formData.isPublic,
                startDate,
                endDate,
                totalSessions: 1, // 번개 스터디는 1회성
                recruitStartDate,
                recruitEndDate,
                goal: formData.goal || undefined,
                status,
            };


            await createStudy(payload);

            showToast('번개 스터디가 개설되었습니다!', 'success');
            navigate('/study');
        } catch (error: unknown) {
            const message = getErrorMessage(error, '번개 스터디 개설에 실패했습니다.');
            showToast(message, 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <UserLayoutV2>
            <div className="max-w-6xl mx-auto px-4 py-8">
                {/* 헤더 */}
                <PageNavHeader
                    title="번개 스터디 만들기"
                    breadcrumbs={[
                        { label: '스터디', path: '/study' },
                        { label: '번개 스터디 만들기' },
                    ]}
                    onBack={() => navigate('/study/create')}
                />

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
                                                            value={formData.provinceId?.toString() || ''}
                                                            onChange={(val) => handleProvinceChange(val ? Number(val) : null)}
                                                            options={provinces.map(p => ({ value: String(p.id), label: p.name }))}
                                                            placeholder="시/도 선택"
                                                        />
                                                    </div>
                                                    <div>
                                                        <Select
                                                            label="구/군"
                                                            value={formData.districtId?.toString() || ''}
                                                            onChange={(val) => setFormData(prev => ({ ...prev, districtId: val ? Number(val) : null }))}
                                                            options={districts.map(d => ({ value: String(d.id), label: d.name }))}
                                                            placeholder="구/군 선택"
                                                            disabled={!formData.provinceId}
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
                                    disabled={isSubmitting || isLoading}
                                    className="flex-1 sm:flex-none sm:min-w-[200px] bg-amber-500 hover:bg-amber-600 shadow-lg shadow-amber-500/20 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                                >
                                    {isSubmitting ? '개설 중...' : '번개 스터디 개설'}
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
