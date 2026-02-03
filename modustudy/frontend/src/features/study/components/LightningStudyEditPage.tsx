import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Zap, MapPin, Users, Calendar, Loader2 } from 'lucide-react';
import { PageNavHeader } from '@/shared/components/layouts/PageNavHeader';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { DatePicker } from './DatePicker';
import { TimePicker } from './TimePicker';
import {
    getTopics,
    getFormats,
    updateStudy,
    studyApi,
    getProvinces,
    getDistricts,
    type TopicParent,
    type FormatItem,
    type StudyCreatePayload,
    type StudyDetailResponse,
    type RegionItem
} from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';


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

const LightningStudyEditPage: React.FC = () => {
    const navigate = useNavigate();
    const { studyId } = useParams<{ studyId: string }>();
    const [searchParams] = useSearchParams();
    const { showToast } = useUIStore();

    // 오늘 날짜 계산
    const today = new Date();
    const formattedToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    // 주제 및 형식 목록 상태
    const [topics, setTopics] = useState<TopicParent[]>([]);
    const [formats, setFormats] = useState<FormatItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [originalStudy, setOriginalStudy] = useState<StudyDetailResponse | null>(null);
    const [currentMemberCount, setCurrentMemberCount] = useState(1); // 현재 참여 인원 (최소 인원 제한용)

    // 지역 목록 상태 (API에서 가져옴)
    const [provinces, setProvinces] = useState<RegionItem[]>([]);
    const [districts, setDistricts] = useState<RegionItem[]>([]);

    const [formData, setFormData] = useState({
        // 기본 정보
        name: '',
        intro: '',
        description: '',
        format: '문제 풀이',
        topic: '알고리즘/코딩테스트',
        subTopic: '',
        meetingType: 'ONLINE',
        maxMembers: 4,
        provinceId: null as number | null,
        districtId: null as number | null,

        // 일정 정보 (1회성)
        meetingDate: '',
        meetingTime: '19:00',
        duration: '2',

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
            const offset = 24;
            const elementRect = element.getBoundingClientRect().top;
            const containerRect = container.getBoundingClientRect().top;
            const offsetPosition = elementRect - containerRect + container.scrollTop - offset;

            container.scrollTo({
                top: offsetPosition,
                behavior: 'smooth'
            });
        }
    };

    // 기존 스터디 데이터 로드
    useEffect(() => {
        const fetchStudyData = async () => {
            if (!studyId) return;

            setIsLoading(true);
            try {
                // 병렬로 데이터 로드 (멤버 수, 지역 목록 포함)
                const [study, topicsData, formatsData, memberCount, provincesData] = await Promise.all([
                    studyApi.getStudyDetail(Number(studyId)),
                    getTopics(),
                    getFormats(),
                    studyApi.getMemberCount(Number(studyId)),
                    getProvinces(),
                ]);

                setOriginalStudy(study);
                setTopics(topicsData);
                setFormats(formatsData);
                setProvinces(provincesData);
                // 현재 멤버 수 설정 (최소 1명 - 스터디장)
                setCurrentMemberCount(Math.max(1, memberCount));

                // 주제 매핑 (topic.name에서 부모/자식 찾기)
                let parentTopicName = '알고리즘/코딩테스트';
                let childTopicName = '';

                if (study.topic) {
                    // topic이 자식 토픽인 경우, 부모를 찾아야 함
                    for (const parent of topicsData) {
                        const foundChild = parent.children.find(c => c.id === study.topic.id);
                        if (foundChild) {
                            parentTopicName = parent.name;
                            childTopicName = foundChild.name;
                            break;
                        }
                        // 부모 자체가 선택된 경우
                        if (parent.id === study.topic.id) {
                            parentTopicName = parent.name;
                            break;
                        }
                    }
                }

                // 형식 매핑
                let formatName = '문제 풀이';
                if (study.format) {
                    formatName = study.format.name;
                }

                // 기존 지역 정보 처리 (regionId가 있으면 해당 구/군 ID를 설정하고 시/도도 찾아야 함)
                let provinceId: number | null = null;
                let districtId: number | null = null;

                if (study.regionId) {
                    // regionId가 있으면 해당 지역의 시/도를 찾기 위해 각 시/도의 구/군 목록 확인
                    districtId = study.regionId;
                    // 시/도 찾기 - 각 시/도의 구/군 목록을 확인해야 함
                    for (const province of provincesData) {
                        try {
                            const districtsData = await getDistricts(province.id);
                            const foundDistrict = districtsData.find(d => d.id === study.regionId);
                            if (foundDistrict) {
                                provinceId = province.id;
                                setDistricts(districtsData);
                                break;
                            }
                        } catch (e) {
                            // 무시
                        }
                    }
                }

                // 폼 데이터 설정
                setFormData({
                    name: study.name || '',
                    intro: study.intro || '',
                    description: study.description || '',
                    format: formatName,
                    topic: parentTopicName,
                    subTopic: childTopicName,
                    meetingType: study.meetingType || 'ONLINE',
                    maxMembers: study.maxMembers || 4,
                    provinceId,
                    districtId,
                    meetingDate: study.startDate || '',
                    meetingTime: study.scheduleTime ? study.scheduleTime.substring(0, 5) : '19:00',
                    duration: '2',
                    isPublic: study.isPublic !== false,
                    goal: study.goal || '',
                });

            } catch (error) {
                console.error('스터디 정보 로드 실패:', error);
                showToast('스터디 정보를 불러오는데 실패했습니다.', 'error');
                navigate('/study');
            } finally {
                setIsLoading(false);
            }
        };

        fetchStudyData();
    }, [studyId]);

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
        // 초기 로드 시에는 이미 districts가 설정되어 있으므로 건너뜀
        if (!isLoading) {
            fetchDistricts();
        }
    }, [formData.provinceId, isLoading]);

    useEffect(() => {
        const container = document.getElementById('main-content-scroll');

        const handleScroll = () => {
            if (!container) return;

            const scrollPosition = container.scrollTop + 100;

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
            handleScroll();
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

        if (!studyId) return;

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

            // 모집 기간 유지 (기존 값 사용 또는 새로 설정)
            const recruitStartDate = originalStudy?.recruitStartDate || formattedToday;

            // 모집 종료일은 모임 전날로 설정 (생성 페이지와 동일한 로직)
            const meetingDateParts = formData.meetingDate.split('-').map(Number);
            const meetingDateObj = new Date(meetingDateParts[0], meetingDateParts[1] - 1, meetingDateParts[2]);
            meetingDateObj.setDate(meetingDateObj.getDate() - 1);
            let recruitEndDate = `${meetingDateObj.getFullYear()}-${String(meetingDateObj.getMonth() + 1).padStart(2, '0')}-${String(meetingDateObj.getDate()).padStart(2, '0')}`;

            // 당일 모임인 경우 모집 종료일이 모집 시작일보다 앞서면 모임 당일로 설정
            if (recruitEndDate < recruitStartDate) {
                recruitEndDate = formData.meetingDate;
            }

            const payload: StudyCreatePayload = {
                name: formData.name,
                intro: formData.intro || undefined,
                description: formData.description || undefined,
                topicId,
                formatId,
                studyType: 'LIGHTNING',
                meetingType: formData.meetingType,
                regionId: formData.districtId || undefined,
                scheduleTime: formData.meetingTime || undefined,
                maxMembers: formData.maxMembers,
                isPublic: formData.isPublic,
                startDate,
                endDate,
                totalSessions: 1,
                recruitStartDate,
                recruitEndDate,
                goal: formData.goal || undefined,
            };


            await updateStudy(Number(studyId), payload);

            showToast('번개 스터디가 수정되었습니다!', 'success');
            // from 파라미터에 따라 이전 페이지로 이동
            const from = searchParams.get('from');
            if (from === 'detail') {
                navigate(`/study/v3/${studyId}`);
            } else {
                navigate(`/study/manage/${studyId}`);
            }
        } catch (error: any) {
            console.error('번개 스터디 수정 실패:', error);
            const message = error?.response?.data?.error?.message || error?.response?.data?.message || '번개 스터디 수정에 실패했습니다.';
            showToast(message, 'error');
        } finally {
            setIsSubmitting(false);
        }
    };

    // 로딩 중 화면
    if (isLoading) {
        return (
            <UserLayoutV2>
                <div className="flex items-center justify-center min-h-[60vh]">
                    <div className="text-center">
                        <Loader2 size={48} className="animate-spin text-amber-500 mx-auto mb-4" />
                        <p className="text-gray-500">스터디 정보를 불러오는 중...</p>
                    </div>
                </div>
            </UserLayoutV2>
        );
    }

    return (
        <UserLayoutV2>
            <div className="max-w-6xl mx-auto px-4 py-8">
                {/* 헤더 */}
                <PageNavHeader
                    title="번개 스터디 수정"
                    breadcrumbs={[
                        { label: '스터디', path: '/study' },
                        { label: '번개 스터디 수정' },
                    ]}
                    onBack={() => navigate(`/study/manage/${studyId}`)}
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
                                                        onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.max(currentMemberCount, prev.maxMembers - 1) }))}
                                                        disabled={formData.maxMembers <= currentMemberCount}
                                                        className={cn(
                                                            "w-10 h-10 flex items-center justify-center rounded-lg font-bold transition-all",
                                                            formData.maxMembers <= currentMemberCount
                                                                ? "bg-gray-100 text-gray-300 cursor-not-allowed"
                                                                : "bg-gray-100 hover:bg-gray-200 text-gray-600"
                                                        )}
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
                                                {currentMemberCount > 1 && (
                                                    <p className="text-xs text-amber-600 mt-1">
                                                        현재 {currentMemberCount}명이 참여 중이므로 최소 인원은 {currentMemberCount}명입니다.
                                                    </p>
                                                )}
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
                                    onClick={() => navigate(`/study/manage/${studyId}`)}
                                >
                                    취소
                                </Button>
                                <Button
                                    type="submit"
                                    size="lg"
                                    disabled={isSubmitting}
                                    className="flex-1 sm:flex-none sm:min-w-[200px] bg-amber-500 hover:bg-amber-600 shadow-lg shadow-amber-500/20 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                                >
                                    {isSubmitting ? '수정 중...' : '수정 완료'}
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

export default LightningStudyEditPage;
