import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Info, Calendar, Plus, Trash2, BookOpen, MapPin, AlertCircle, Clock, Users, Target, Shield, Sparkles, ChevronUp, ChevronDown, GripVertical } from 'lucide-react';
import { Spinner, ButtonSpinner } from '@/shared/components/Spinner';
import { PageNavHeader } from '@/shared/components/layouts/PageNavHeader';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { DateRangePicker } from './DateRangePicker';
import { DatePicker, TimePicker } from '@/shared/components';
import {
    getTopics, getFormats, getProvinces, getDistricts, createStudy, updateStudy, generateStudyPlan, generateStudyPlanStream, getMyTemplates, createTemplate, studyApi, createStudySessions, getStudySessions,
    type TopicParent, type FormatItem, type RegionItem, type StudyCreatePayload, type AiStudyPlanResponse, type StudyTemplateItem
} from '@/api/endpoints/studyApi';
import { getStudyPreference } from '@/features/setting/api/settingApi';

interface CurriculumItem {
    session: number;
    description: string;
    type?: 'ONLINE' | 'OFFLINE';
    date?: string;
}

// 하드코딩 폴백 (API 실패 시 사용)
const FALLBACK_FORMATS = ['문제 풀이', '독서/책 스터디', '강의 수강', '프로젝트', '모의 면접', '코드 리뷰', '발표/세미나', '토론'];

const StudyCreatePage: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const showToast = useUIStore((state) => state.showToast);
    const { user } = useAuthStore();
    const hasCheckedSavedData = useRef(false);

    // 수정 모드 관련 상태
    const editStudyId = searchParams.get('studyId');
    const isEditMode = !!editStudyId;
    const [isLoadingStudy, setIsLoadingStudy] = useState(false);
    const [currentMemberCount, setCurrentMemberCount] = useState(1); // 현재 참여 인원 (최소 인원 제한용)

    // API 데이터 상태
    const [topics, setTopics] = useState<TopicParent[]>([]);
    const [formats, setFormats] = useState<FormatItem[]>([]);
    const [provinces, setProvinces] = useState<RegionItem[]>([]);
    const [districts, setDistricts] = useState<RegionItem[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [aiTopicInput, setAiTopicInput] = useState('');
    const [isAiGenerating, setIsAiGenerating] = useState(false);
    const [streamingText, setStreamingText] = useState(''); // 스트리밍 중 실시간 텍스트
    const [generationStep, setGenerationStep] = useState(''); // AI 생성 단계 메시지
    const [showDifficultyInfo, setShowDifficultyInfo] = useState(false);
    const [savedTemplates, setSavedTemplates] = useState<StudyTemplateItem[]>([]);
    const [showTemplateModal, setShowTemplateModal] = useState(false);
    const [isSavingTemplate, setIsSavingTemplate] = useState(false);
    const [showPreferenceModal, setShowPreferenceModal] = useState(false);
    const [validationErrors, setValidationErrors] = useState<string[]>([]); // validation 에러 목록
    const [showErrorModal, setShowErrorModal] = useState(false);

    // 오늘 날짜 계산 (YYYY-MM-DD)
    const today = new Date();
    const formattedToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    const [formData, setFormData] = useState({
        // 기본 정보
        name: '',
        intro: '', // 한줄 소개 (썸네일용)
        description: '', // 상세 설명
        topicParentId: null as number | null, // 대분류 ID
        topicId: null as number | null,       // 세부주제 ID
        formatId: null as number | null,      // 형식 ID
        difficulty: 'BEGINNER',
        meetingType: 'ONLINE',
        maxMembers: 4,
        totalSessions: 8,
        provinceId: null as number | null,    // 시/도 ID
        districtId: null as number | null,    // 시/군/구 ID (= regionId)

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
        goal: '',                                // 최종 스터디 목표
        textbook: '',                            // 교재/자료
        prerequisites: '',                       // 선행 조건
        processDetail: '',                       // 진행 방식 상세
        targetOrgType: '',                       // 대상 소속 타입

        // 커리큘럼
        hasCurriculum: false,
        curriculum: [{ session: 1, description: '' }] as CurriculumItem[]
    });

    // API 데이터 로딩
    useEffect(() => {
        const loadData = async () => {
            try {
                const [topicsData, formatsData, provincesData] = await Promise.all([
                    getTopics(),
                    getFormats(),
                    getProvinces()
                ]);
                setTopics(topicsData);
                setFormats(formatsData);
                setProvinces(provincesData);
            } catch (err) {
                console.error('초기 데이터 로딩 실패:', err);
            }
        };
        loadData();
    }, []);

    // 수정 모드: 기존 스터디 데이터 로딩
    useEffect(() => {
        if (!isEditMode || !editStudyId || topics.length === 0) return;

        const loadStudyData = async () => {
            setIsLoadingStudy(true);
            try {
                // 스터디 정보, 세션(커리큘럼), 멤버 수를 함께 로드
                const [study, sessions, memberCount] = await Promise.all([
                    studyApi.getStudyDetail(Number(editStudyId)),
                    getStudySessions(Number(editStudyId)),
                    studyApi.getMemberCount(Number(editStudyId))
                ]);

                // 현재 멤버 수 설정 (최소 1명 - 스터디장)
                setCurrentMemberCount(Math.max(1, memberCount));

                // 토픽 ID로 대분류 찾기
                let topicParentId: number | null = null;
                if (study.topic?.id) {
                    for (const parent of topics) {
                        const found = parent.children.find((c: any) => c.id === study.topic.id);
                        if (found) {
                            topicParentId = parent.id;
                            break;
                        }
                    }
                }

                // 세션 데이터를 커리큘럼 형태로 변환
                const hasSessions = sessions && sessions.length > 0;
                const curriculumData: CurriculumItem[] = hasSessions
                    ? sessions
                        .sort((a, b) => a.sessionNumber - b.sessionNumber)
                        .map(session => ({
                            session: session.sessionNumber,
                            description: session.description || session.title || '',
                            date: session.scheduledAt ? session.scheduledAt.split('T')[0] : undefined
                        }))
                    : [{ session: 1, description: '' }];

                setFormData(prev => ({
                    ...prev,
                    name: study.name || '',
                    intro: study.intro || '',
                    description: study.description || '',
                    topicParentId: topicParentId,
                    topicId: study.topic?.id || null,
                    formatId: study.format?.id || null,
                    difficulty: study.difficulty || 'BEGINNER',
                    meetingType: study.meetingType || 'ONLINE',
                    maxMembers: study.maxMembers || 4,
                    totalSessions: study.totalSessions || 8,
                    provinceId: null, // 지역 처리 필요 시 추가
                    districtId: study.regionId || null,
                    startDate: study.startDate || null,
                    endDate: study.endDate || null,
                    recruitStartDate: study.recruitStartDate || null,
                    recruitEndDate: study.recruitEndDate || null,
                    scheduleDays: study.scheduleDays ? study.scheduleDays.split(',') : [],
                    scheduleTime: study.scheduleTime ? study.scheduleTime.substring(0, 5) : '19:00',
                    studyType: study.studyType || 'PLANNED',
                    isPublic: study.isPublic !== false,
                    penaltyPolicy: study.penaltyPolicy || 'NORMAL',
                    goal: study.goal || '',
                    textbook: study.textbook || '',
                    prerequisites: study.prerequisites || '',
                    processDetail: study.processDetail || '',
                    targetOrgType: study.targetOrgType || '',
                    hasCurriculum: hasSessions,
                    curriculum: curriculumData
                }));
            } catch (err) {
                console.error('스터디 데이터 로딩 실패:', err);
                showToast('스터디 정보를 불러오는데 실패했습니다.', 'error');
            } finally {
                setIsLoadingStudy(false);
            }
        };

        loadStudyData();
    }, [isEditMode, editStudyId, topics]);

    // 시/도 선택 시 시/군/구 로딩
    useEffect(() => {
        if (formData.provinceId) {
            getDistricts(formData.provinceId)
                .then(setDistricts)
                .catch((err) => console.error('시/군/구 로딩 실패:', err));
        } else {
            setDistricts([]);
        }
    }, [formData.provinceId]);

    // DB에서 저장된 템플릿 확인 (수정 모드가 아닐 때만)
    useEffect(() => {
        if (!user?.id || hasCheckedSavedData.current || isEditMode) return;
        hasCheckedSavedData.current = true;

        const checkSavedTemplates = async () => {
            try {
                const templates = await getMyTemplates();
                if (templates && templates.length > 0) {
                    setSavedTemplates(templates);
                    setShowTemplateModal(true);
                }
            } catch (err) {
                // 템플릿 조회 실패 시 무시 (신규 유저 등)
            }
        };
        checkSavedTemplates();
    }, [user?.id, isEditMode]);

    // 템플릿 선택 시 폼에 적용 (내용만 채우고, 날짜/기간은 사용자가 직접 설정)
    const applyTemplate = (template: StudyTemplateItem) => {
        setFormData(prev => {
            const updated = { ...prev };

            // 내용 필드만 적용 (날짜/기간 제외)
            if (template.name) updated.name = template.name;
            if (template.intro) updated.intro = template.intro;
            if (template.description) updated.description = template.description;
            if (template.goal) updated.goal = template.goal;
            if (template.textbook) updated.textbook = template.textbook;
            if (template.prerequisites) updated.prerequisites = template.prerequisites;
            if (template.processDetail) updated.processDetail = template.processDetail;
            if (template.difficulty) updated.difficulty = template.difficulty;
            if (template.meetingType) updated.meetingType = template.meetingType;
            if (template.penaltyPolicy) updated.penaltyPolicy = template.penaltyPolicy;

            // 토픽 매칭 (이름으로 ID 찾기)
            if (template.topic && topics.length > 0) {
                const topicLower = template.topic.toLowerCase().trim();
                for (const parent of topics) {
                    const child = parent.children.find(c =>
                        c.name.toLowerCase() === topicLower ||
                        c.name.toLowerCase().includes(topicLower) ||
                        topicLower.includes(c.name.toLowerCase())
                    );
                    if (child) {
                        updated.topicParentId = parent.id;
                        updated.topicId = child.id;
                        break;
                    }
                }
            }

            // 형식 매칭 (이름으로 ID 찾기)
            if (template.format && formats.length > 0) {
                const formatLower = template.format.toLowerCase().trim();
                const matched = formats.find(f =>
                    f.name.toLowerCase() === formatLower ||
                    f.name.toLowerCase().includes(formatLower) ||
                    formatLower.includes(f.name.toLowerCase())
                );
                if (matched) {
                    updated.formatId = matched.id;
                }
            }

            return updated;
        });
        setShowTemplateModal(false);
        showToast('템플릿을 불러왔습니다.', 'success');
    };

    // 사용자 스터디 선호 설정 (기술스택 + 일정)
    const [userTechStack, setUserTechStack] = useState<string[]>([]);
    const [userSchedule, setUserSchedule] = useState<{
        availableDays: string[];
        preferredDurationWeeks: number;
        preferredTimeSlot: string | null;
    }>({ availableDays: [], preferredDurationWeeks: 4, preferredTimeSlot: null });
    const [preferenceLoaded, setPreferenceLoaded] = useState(false);

    // 시간대 → 중간 시간 변환 맵
    const timeSlotToMiddleTime: Record<string, string> = {
        morning: '10:00',      // 07:00-12:00 → 중간 09:30 → 10:00
        afternoon: '15:00',    // 12:00-18:00 → 중간 15:00
        evening: '20:00',      // 18:00-22:00 → 중간 20:00
        night: '23:00',        // 22:00-02:00 → 23:00
    };

    useEffect(() => {
        const loadPreference = async () => {
            try {
                // API에서 선호 설정 가져오기
                const pref: any = await getStudyPreference();
                const techStacks = pref.techStacks || pref.techStack || [];
                if (techStacks.length > 0) {
                    setUserTechStack(techStacks);
                }
                // 일정 정보 로드 (선호 시간대 포함)
                const timeSlots = pref.preferredTimeSlots || [];
                setUserSchedule({
                    availableDays: pref.availableDays || [],
                    preferredDurationWeeks: pref.preferredDurationWeeks || 4,
                    preferredTimeSlot: timeSlots[0] || pref.preferredTimeSlot || null,
                });
            } catch (err) {
                // localStorage fallback
                try {
                    const saved = localStorage.getItem('studyPreference');
                    if (saved) {
                        const pref = JSON.parse(saved);
                        const stack = pref.techStacks || pref.techStack || [];
                        if (stack.length > 0) setUserTechStack(stack);
                        const timeSlots = pref.preferredTimeSlots || [];
                        setUserSchedule({
                            availableDays: pref.availableDays || [],
                            preferredDurationWeeks: pref.preferredDurationWeeks || 4,
                            preferredTimeSlot: timeSlots[0] || pref.preferredTimeSlot || null,
                        });
                    }
                } catch { /* 무시 */ }
            }
            setPreferenceLoaded(true);
        };
        loadPreference();
    }, []);

    // 선호 설정 여부 확인 (가용 요일만 있으면 AI 추천 가능, 기술스택은 선택)
    const hasStudyPreference = userSchedule.availableDays.length > 0;

    const aiSuggestions = useMemo(() => {
        const items: string[] = [];
        for (const tech of userTechStack.slice(0, 5)) {
            items.push(`${tech} 심화 학습`);
        }
        if (items.length === 0) {
            // 선호 설정 없을 때 예시 표시
            items.push('코딩테스트 준비', 'CS 기초 스터디', '프로젝트 협업');
        }
        return items;
    }, [userTechStack]);

    // Navigation Logic
    const [activeSection, setActiveSection] = useState('basic-info');
    const sections = [
        { id: 'basic-info', label: '기본 정보' },
        { id: 'schedule-info', label: '일정 및 모집' },
        { id: 'curriculum-info', label: '커리큘럼' },
        { id: 'additional-info', label: '추가 정보' },
        { id: 'rule-info', label: '스터디 규칙' },
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

    // 요일 토글 핸들러
    const handleDayToggle = (day: string) => {
        setFormData(prev => {
            const newScheduleDays = prev.scheduleDays.includes(day)
                ? prev.scheduleDays.filter(d => d !== day)
                : [...prev.scheduleDays, day];

            // 커리큘럼이 있으면 날짜 재계산
            if (prev.hasCurriculum && prev.curriculum.length > 0 && prev.startDate && newScheduleDays.length > 0) {
                const formatDate = (d: Date) =>
                    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

                const dayToNum: Record<string, number> = {
                    '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                    'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                };

                const startDate = new Date(prev.startDate);
                const selectedDayNums = newScheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b);
                const daysPerWeek = newScheduleDays.length;

                // 시작일 이후 첫 번째 선호 요일 찾기
                const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
                    const result = new Date(from);
                    const currentDay = result.getDay();
                    // 가장 가까운 선호 요일까지의 거리 계산
                    let minDays = 7;
                    for (const dayNum of preferredDays) {
                        let daysUntil = dayNum - currentDay;
                        if (daysUntil < 0) daysUntil += 7;
                        if (daysUntil < minDays) minDays = daysUntil;
                    }
                    result.setDate(result.getDate() + minDays);
                    return result;
                };

                const firstSessionDate = findFirstPreferredDay(startDate, selectedDayNums);
                const firstDayNum = firstSessionDate.getDay();

                // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
                const orderedDayNums = [
                    ...selectedDayNums.filter(d => d >= firstDayNum),
                    ...selectedDayNums.filter(d => d < firstDayNum)
                ];

                let lastSessionDate: Date | null = null;
                const updatedCurriculum = prev.curriculum.map((item, index) => {
                    const sessionDate = new Date(firstSessionDate);
                    if (daysPerWeek === 1) {
                        sessionDate.setDate(sessionDate.getDate() + index * 7);
                    } else {
                        const weekIndex = Math.floor(index / daysPerWeek);
                        const dayIndex = index % daysPerWeek;
                        const targetDayNum = orderedDayNums[dayIndex];
                        let daysFromFirst = targetDayNum - firstDayNum;
                        if (daysFromFirst < 0) daysFromFirst += 7;
                        sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
                    }
                    lastSessionDate = sessionDate;
                    return { ...item, date: formatDate(sessionDate) };
                });

                // 종료일 업데이트
                let newEndDate = prev.endDate;
                if (lastSessionDate && prev.endDate) {
                    const currentEndDate = new Date(prev.endDate);
                    if (lastSessionDate > currentEndDate) {
                        newEndDate = formatDate(lastSessionDate);
                    }
                }

                return {
                    ...prev,
                    scheduleDays: newScheduleDays,
                    curriculum: updatedCurriculum,
                    endDate: newEndDate,
                };
            }

            return { ...prev, scheduleDays: newScheduleDays };
        });
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleOptionToggle = (name: string, value: string | boolean) => {
        // 커리큘럼 설정 시 사용자 선호 요일 자동 적용
        if (name === 'hasCurriculum' && value === true) {
            setFormData(prev => {
                // scheduleDays가 비어있으면 사용자 선호 요일 적용
                const newScheduleDays = prev.scheduleDays.length > 0
                    ? prev.scheduleDays
                    : userSchedule.availableDays;

                // 커리큘럼 초기화 (비어있으면 totalSessions 만큼 생성)
                let newCurriculum = prev.curriculum;
                let newEndDate = prev.endDate;

                if (prev.curriculum.length === 0 && prev.totalSessions > 0 && prev.startDate) {
                    const formatDate = (d: Date) =>
                        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

                    const dayToNum: Record<string, number> = {
                        '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                        'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                    };

                    const startDate = new Date(prev.startDate);
                    const selectedDayNums = newScheduleDays.length > 0
                        ? newScheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b)
                        : [startDate.getDay()];
                    const daysPerWeek = newScheduleDays.length || 1;

                    // 시작일 이후 첫 번째 선호 요일 찾기
                    const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
                        const result = new Date(from);
                        const currentDay = result.getDay();
                        let minDays = 7;
                        for (const dayNum of preferredDays) {
                            let daysUntil = dayNum - currentDay;
                            if (daysUntil < 0) daysUntil += 7;
                            if (daysUntil < minDays) minDays = daysUntil;
                        }
                        result.setDate(result.getDate() + minDays);
                        return result;
                    };

                    const firstSessionDate = findFirstPreferredDay(startDate, selectedDayNums);
                    const firstDayNum = firstSessionDate.getDay();

                    // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
                    const orderedDayNums = [
                        ...selectedDayNums.filter(d => d >= firstDayNum),
                        ...selectedDayNums.filter(d => d < firstDayNum)
                    ];

                    newCurriculum = [];
                    let lastSessionDate: Date | null = null;

                    for (let i = 0; i < prev.totalSessions; i++) {
                        const sessionDate = new Date(firstSessionDate);
                        if (daysPerWeek === 1) {
                            sessionDate.setDate(sessionDate.getDate() + i * 7);
                        } else {
                            const weekIndex = Math.floor(i / daysPerWeek);
                            const dayIndex = i % daysPerWeek;
                            const targetDayNum = orderedDayNums[dayIndex];
                            let daysFromFirst = targetDayNum - firstDayNum;
                            if (daysFromFirst < 0) daysFromFirst += 7;
                            sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
                        }
                        lastSessionDate = sessionDate;
                        newCurriculum.push({
                            session: i + 1,
                            description: '',
                            type: 'ONLINE',
                            date: formatDate(sessionDate),
                        });
                    }

                    // 마지막 세션 날짜로 종료일 업데이트
                    if (lastSessionDate) {
                        const currentEndDate = prev.endDate ? new Date(prev.endDate) : null;
                        if (!currentEndDate || lastSessionDate > currentEndDate) {
                            newEndDate = formatDate(lastSessionDate);
                        }
                    }
                }

                // 선호 시간대에 맞춰 scheduleTime 업데이트
                const newScheduleTime = userSchedule.preferredTimeSlot
                    ? timeSlotToMiddleTime[userSchedule.preferredTimeSlot] || prev.scheduleTime
                    : prev.scheduleTime;

                return {
                    ...prev,
                    hasCurriculum: true,
                    scheduleDays: newScheduleDays,
                    curriculum: newCurriculum,
                    endDate: newEndDate,
                    scheduleTime: newScheduleTime,
                };
            });
            return;
        }

        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleProvinceChange = (provinceId: number | null) => {
        setFormData(prev => ({ ...prev, provinceId, districtId: null }));
    };

    const handleTopicParentChange = (parentId: number | null) => {
        setFormData(prev => ({ ...prev, topicParentId: parentId, topicId: null }));
    };

    const handleDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => {
            const formatDate = (d: Date) => {
                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            };

            // 커리큘럼이 있고 시작일이 변경된 경우 커리큘럼 날짜 재계산
            if (start && prev.curriculum.length > 0 && prev.hasCurriculum && start !== prev.startDate) {
                const startDate = new Date(start);
                const scheduleDays = prev.scheduleDays;

                // 요일 매핑
                const dayToNum: Record<string, number> = {
                    '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                    'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                };

                const selectedDayNums = scheduleDays.length > 0
                    ? scheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b)
                    : [startDate.getDay()];
                const daysPerWeek = scheduleDays.length || 1;

                // 시작일 이후 첫 번째 선호 요일 찾기
                const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
                    const result = new Date(from);
                    const currentDay = result.getDay();
                    let minDays = 7;
                    for (const dayNum of preferredDays) {
                        let daysUntil = dayNum - currentDay;
                        if (daysUntil < 0) daysUntil += 7;
                        if (daysUntil < minDays) minDays = daysUntil;
                    }
                    result.setDate(result.getDate() + minDays);
                    return result;
                };

                const firstSessionDate = findFirstPreferredDay(startDate, selectedDayNums);
                const firstDayNum = firstSessionDate.getDay();

                // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
                const orderedDayNums = [
                    ...selectedDayNums.filter(d => d >= firstDayNum),
                    ...selectedDayNums.filter(d => d < firstDayNum)
                ];

                // 각 세션의 날짜 재계산
                let lastSessionDate: Date | null = null;
                const updatedCurriculum = prev.curriculum.map((item, index) => {
                    const sessionDate = new Date(firstSessionDate);

                    if (daysPerWeek === 1 || selectedDayNums.length === 0) {
                        // 주 1회: 매주 같은 요일
                        sessionDate.setDate(sessionDate.getDate() + index * 7);
                    } else {
                        // 주 n회: 선택된 요일에 맞춰 계산
                        const weekIndex = Math.floor(index / daysPerWeek);
                        const dayIndex = index % daysPerWeek;
                        const targetDayNum = orderedDayNums[dayIndex];
                        let daysFromFirst = targetDayNum - firstDayNum;
                        if (daysFromFirst < 0) daysFromFirst += 7;
                        sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
                    }

                    lastSessionDate = sessionDate;
                    return {
                        ...item,
                        date: formatDate(sessionDate)
                    };
                });

                // 마지막 세션 날짜가 종료일보다 이후면 종료일 자동 확장
                let finalEndDate = end;
                if (lastSessionDate) {
                    const endDateObj = end ? new Date(end) : null;
                    if (!endDateObj || lastSessionDate > endDateObj) {
                        finalEndDate = formatDate(lastSessionDate);
                    }
                }

                return {
                    ...prev,
                    startDate: start,
                    endDate: finalEndDate,
                    curriculum: updatedCurriculum
                };
            }

            // 커리큘럼이 있지만 시작일은 같고 종료일만 변경된 경우, 마지막 세션 날짜 확인
            if (prev.hasCurriculum && prev.curriculum.length > 0) {
                const lastSession = prev.curriculum[prev.curriculum.length - 1];
                if (lastSession?.date && end) {
                    const lastSessionDate = new Date(lastSession.date);
                    const newEndDate = new Date(end);
                    // 마지막 세션이 종료일보다 이후면 종료일을 마지막 세션으로 확장
                    if (lastSessionDate > newEndDate) {
                        return { ...prev, startDate: start, endDate: formatDate(lastSessionDate) };
                    }
                }
            }

            // 일반적인 날짜 변경
            return { ...prev, startDate: start, endDate: end };
        });
    };

    const handleRecruitDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => {
            const updates: Partial<typeof prev> = {
                recruitStartDate: start,
                recruitEndDate: end
            };

            // 모집 종료일이 설정되고 스터디 시작일이 모집 종료일보다 이전이면 스터디 기간 조정
            if (end) {
                const recruitEnd = new Date(end);

                // 스터디 시작일이 모집 종료일보다 이전이면 스터디 시작일을 모집 종료일 다음 날로 설정
                if (prev.startDate) {
                    const studyStart = new Date(prev.startDate);
                    if (studyStart <= recruitEnd) {
                        // 모집 종료일 다음 날로 시작일 조정
                        const newStartDate = new Date(recruitEnd);
                        newStartDate.setDate(newStartDate.getDate() + 1);
                        const formatDate = (d: Date) =>
                            `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                        updates.startDate = formatDate(newStartDate);

                        // 종료일도 시작일 기준으로 재계산 (기존 기간 유지)
                        if (prev.endDate) {
                            const originalDuration = new Date(prev.endDate).getTime() - studyStart.getTime();
                            const newEndDate = new Date(newStartDate.getTime() + originalDuration);
                            updates.endDate = formatDate(newEndDate);
                        }

                        // 커리큘럼이 있으면 날짜도 재계산
                        if (prev.hasCurriculum && prev.curriculum.length > 0) {
                            const dayToNum: Record<string, number> = {
                                '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                                'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                            };
                            const scheduleDays = prev.scheduleDays;
                            const selectedDayNums = scheduleDays.length > 0
                                ? scheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b)
                                : [newStartDate.getDay()];
                            const daysPerWeek = scheduleDays.length || 1;

                            // 시작일 이후 첫 번째 선호 요일 찾기
                            const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
                                const result = new Date(from);
                                const currentDay = result.getDay();
                                let minDays = 7;
                                for (const dayNum of preferredDays) {
                                    let daysUntil = dayNum - currentDay;
                                    if (daysUntil < 0) daysUntil += 7;
                                    if (daysUntil < minDays) minDays = daysUntil;
                                }
                                result.setDate(result.getDate() + minDays);
                                return result;
                            };

                            const firstSessionDate = findFirstPreferredDay(newStartDate, selectedDayNums);
                            const firstDayNum = firstSessionDate.getDay();

                            // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
                            const orderedDayNums = [
                                ...selectedDayNums.filter(d => d >= firstDayNum),
                                ...selectedDayNums.filter(d => d < firstDayNum)
                            ];

                            let lastSessionDate: Date | null = null;
                            updates.curriculum = prev.curriculum.map((item, index) => {
                                const sessionDate = new Date(firstSessionDate);
                                if (daysPerWeek === 1) {
                                    // 주 1회: 매주 같은 요일
                                    sessionDate.setDate(sessionDate.getDate() + index * 7);
                                } else {
                                    // 주 n회: 선택된 요일에 맞춰 계산
                                    const weekIndex = Math.floor(index / daysPerWeek);
                                    const dayIndex = index % daysPerWeek;
                                    const targetDayNum = orderedDayNums[dayIndex];
                                    let daysFromFirst = targetDayNum - firstDayNum;
                                    if (daysFromFirst < 0) daysFromFirst += 7;
                                    sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
                                }
                                lastSessionDate = sessionDate;
                                return { ...item, date: formatDate(sessionDate) };
                            });

                            // 마지막 세션 날짜가 종료일보다 이후면 종료일 자동 확장
                            if (lastSessionDate && updates.endDate) {
                                const currentEndDate = new Date(updates.endDate);
                                if (lastSessionDate > currentEndDate) {
                                    updates.endDate = formatDate(lastSessionDate);
                                }
                            }
                        }
                    }
                }
            }

            return { ...prev, ...updates };
        });
    };

    const handleCurriculumChange = (index: number, field: keyof CurriculumItem, value: string) => {
        const newCurriculum = [...formData.curriculum];
        newCurriculum[index] = { ...newCurriculum[index], [field]: value };
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const addSession = () => {
        // 총 회차 수를 초과하면 totalSessions도 함께 증가
        setFormData(prev => {
            const newSessionCount = prev.curriculum.length + 1;

            // 새 회차 날짜 계산
            let newDate = '';
            let newEndDate = prev.endDate;
            if (prev.startDate) {
                const dayToNum: Record<string, number> = {
                    '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                    'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                };
                const formatDate = (d: Date) => {
                    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                };

                const startDate = new Date(prev.startDate);
                const scheduleDays = prev.scheduleDays;
                const selectedDayNums = scheduleDays.length > 0
                    ? scheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b)
                    : [startDate.getDay()];
                const daysPerWeek = scheduleDays.length || 1;
                const index = prev.curriculum.length; // 새 항목의 인덱스

                // 시작일 이후 첫 번째 선호 요일 찾기
                const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
                    const result = new Date(from);
                    const currentDay = result.getDay();
                    let minDays = 7;
                    for (const dayNum of preferredDays) {
                        let daysUntil = dayNum - currentDay;
                        if (daysUntil < 0) daysUntil += 7;
                        if (daysUntil < minDays) minDays = daysUntil;
                    }
                    result.setDate(result.getDate() + minDays);
                    return result;
                };

                const firstSessionDate = findFirstPreferredDay(startDate, selectedDayNums);
                const firstDayNum = firstSessionDate.getDay();

                // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
                const orderedDayNums = [
                    ...selectedDayNums.filter(d => d >= firstDayNum),
                    ...selectedDayNums.filter(d => d < firstDayNum)
                ];

                const sessionDate = new Date(firstSessionDate);
                if (daysPerWeek === 1 || selectedDayNums.length === 0) {
                    sessionDate.setDate(sessionDate.getDate() + index * 7);
                } else {
                    const weekIndex = Math.floor(index / daysPerWeek);
                    const dayIndex = index % daysPerWeek;
                    const targetDayNum = orderedDayNums[dayIndex];
                    let daysFromFirst = targetDayNum - firstDayNum;
                    if (daysFromFirst < 0) daysFromFirst += 7;
                    sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
                }
                newDate = formatDate(sessionDate);

                // 새 세션 날짜가 종료일보다 이후면 종료일 자동 확장
                if (prev.endDate) {
                    const currentEndDate = new Date(prev.endDate);
                    if (sessionDate > currentEndDate) {
                        newEndDate = formatDate(sessionDate);
                    }
                }
            }

            return {
                ...prev,
                curriculum: [...prev.curriculum, {
                    session: newSessionCount,
                    description: '',
                    type: 'ONLINE',
                    date: newDate
                }],
                // 현재 커리큘럼 수가 totalSessions를 넘으면 자동으로 증가
                totalSessions: Math.max(prev.totalSessions, newSessionCount),
                // 종료일 업데이트
                endDate: newEndDate
            };
        });
    };

    const removeSession = (index: number) => {
        setFormData(prev => {
            const newCurriculum = prev.curriculum
                .filter((_, i) => i !== index)
                .map((item, i) => ({ ...item, session: i + 1 }));
            return {
                ...prev,
                curriculum: newCurriculum,
                // 커리큘럼 수에 맞춰 totalSessions도 감소
                totalSessions: newCurriculum.length
            };
        });
    };

    // 커리큘럼 날짜 재계산 헬퍼 함수
    const recalculateCurriculumDates = (curriculum: CurriculumItem[], startDate: string | null, scheduleDays: string[]) => {
        if (!startDate) return curriculum;

        const start = new Date(startDate);
        const dayToNum: Record<string, number> = {
            '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
            'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
        };
        const formatDate = (d: Date) => {
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        };

        const selectedDayNums = scheduleDays.length > 0
            ? scheduleDays.map(d => dayToNum[d] ?? 0).sort((a, b) => a - b)
            : [start.getDay()];
        const daysPerWeek = scheduleDays.length || 1;

        // 시작일 이후 첫 번째 선호 요일 찾기
        const findFirstPreferredDay = (from: Date, preferredDays: number[]): Date => {
            const result = new Date(from);
            const currentDay = result.getDay();
            let minDays = 7;
            for (const dayNum of preferredDays) {
                let daysUntil = dayNum - currentDay;
                if (daysUntil < 0) daysUntil += 7;
                if (daysUntil < minDays) minDays = daysUntil;
            }
            result.setDate(result.getDate() + minDays);
            return result;
        };

        const firstSessionDate = findFirstPreferredDay(start, selectedDayNums);
        const firstDayNum = firstSessionDate.getDay();

        // 첫 세션 요일 기준으로 재정렬 (첫 세션 요일이 먼저 오도록)
        const orderedDayNums = [
            ...selectedDayNums.filter(d => d >= firstDayNum),
            ...selectedDayNums.filter(d => d < firstDayNum)
        ];

        return curriculum.map((item, index) => {
            const sessionDate = new Date(firstSessionDate);

            if (daysPerWeek === 1 || selectedDayNums.length === 0) {
                sessionDate.setDate(sessionDate.getDate() + index * 7);
            } else {
                const weekIndex = Math.floor(index / daysPerWeek);
                const dayIndex = index % daysPerWeek;
                const targetDayNum = orderedDayNums[dayIndex];
                let daysFromFirst = targetDayNum - firstDayNum;
                if (daysFromFirst < 0) daysFromFirst += 7;
                sessionDate.setDate(firstSessionDate.getDate() + weekIndex * 7 + daysFromFirst);
            }

            return {
                ...item,
                session: index + 1,
                date: formatDate(sessionDate)
            };
        });
    };

    // 회차 위치 변경 (위로/아래로 이동) - 날짜도 재계산
    const moveSession = (index: number, direction: 'up' | 'down') => {
        setFormData(prev => {
            const newCurriculum = [...prev.curriculum];
            const targetIndex = direction === 'up' ? index - 1 : index + 1;

            // 범위 체크
            if (targetIndex < 0 || targetIndex >= newCurriculum.length) return prev;

            // 내용만 교환 (날짜는 위치에 따라 재계산)
            const tempDescription = newCurriculum[index].description;
            const tempType = newCurriculum[index].type;
            newCurriculum[index].description = newCurriculum[targetIndex].description;
            newCurriculum[index].type = newCurriculum[targetIndex].type;
            newCurriculum[targetIndex].description = tempDescription;
            newCurriculum[targetIndex].type = tempType;

            // 날짜 재계산
            const reorderedCurriculum = recalculateCurriculumDates(newCurriculum, prev.startDate, prev.scheduleDays);

            // 마지막 세션 날짜 기준으로 종료일 확인
            let newEndDate = prev.endDate;
            const lastSession = reorderedCurriculum[reorderedCurriculum.length - 1];
            if (lastSession?.date && prev.endDate) {
                const lastDate = new Date(lastSession.date);
                const currentEndDate = new Date(prev.endDate);
                if (lastDate > currentEndDate) {
                    const formatDate = (d: Date) =>
                        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                    newEndDate = formatDate(lastDate);
                }
            }

            return { ...prev, curriculum: reorderedCurriculum, endDate: newEndDate };
        });
    };

    // 드래그앤드롭 상태
    const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

    // 드래그앤드롭 핸들러
    const handleDragStart = (index: number) => {
        setDraggedIndex(index);
    };

    const handleDragOver = (e: React.DragEvent, index: number) => {
        e.preventDefault();
    };

    const handleDrop = (targetIndex: number) => {
        if (draggedIndex === null || draggedIndex === targetIndex) {
            setDraggedIndex(null);
            return;
        }

        setFormData(prev => {
            const newCurriculum = [...prev.curriculum];
            // 드래그된 항목의 내용 저장
            const draggedItem = { ...newCurriculum[draggedIndex] };

            // 항목 이동
            newCurriculum.splice(draggedIndex, 1);
            newCurriculum.splice(targetIndex, 0, draggedItem);

            // 날짜 재계산 (session 번호도 재정렬됨)
            const reorderedCurriculum = recalculateCurriculumDates(newCurriculum, prev.startDate, prev.scheduleDays);

            // 마지막 세션 날짜 기준으로 종료일 확인
            let newEndDate = prev.endDate;
            const lastSession = reorderedCurriculum[reorderedCurriculum.length - 1];
            if (lastSession?.date && prev.endDate) {
                const lastDate = new Date(lastSession.date);
                const currentEndDate = new Date(prev.endDate);
                if (lastDate > currentEndDate) {
                    const formatDate = (d: Date) =>
                        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
                    newEndDate = formatDate(lastDate);
                }
            }

            return { ...prev, curriculum: reorderedCurriculum, endDate: newEndDate };
        });

        setDraggedIndex(null);
    };

    const handleDragEnd = () => {
        setDraggedIndex(null);
    };

    // AI 스터디 계획 생성
    const handleAiGenerate = async () => {
        // 선호 설정 체크를 먼저 수행
        if (!hasStudyPreference) {
            setShowPreferenceModal(true);
            return;
        }

        if (!aiTopicInput.trim()) {
            showToast('스터디 주제를 입력해주세요.', 'error');
            return;
        }

        setIsAiGenerating(true);
        let stepInterval: ReturnType<typeof setInterval> | null = null;
        try {
            // 사용자 스터디 선호 설정 API에서 로드
            let techStack: string[] | undefined;
            let schedule: string[] | undefined;
            let preferredDurationWeeks = 4; // 기본값
            let availableDays: string[] = [];
            let preferredTimeSlot: string | null = null;

            try {
                const pref: any = await getStudyPreference();
                // 백엔드 필드명: techStacks, preferredTimeSlots (복수형)
                const stacks = pref.techStacks || pref.techStack || [];
                if (stacks.length > 0) techStack = stacks;

                availableDays = pref.availableDays || [];
                const timeSlots = pref.preferredTimeSlots || [];
                preferredTimeSlot = timeSlots[0] || pref.preferredTimeSlot || null;
                preferredDurationWeeks = pref.preferredDurationWeeks || 4;

                if (availableDays.length > 0) {
                    const timeSlotMap: Record<string, string> = {
                        morning: '07:00-12:00',
                        afternoon: '12:00-18:00',
                        evening: '18:00-22:00',
                        night: '22:00-02:00',
                    };
                    const timeStr = preferredTimeSlot ? timeSlotMap[preferredTimeSlot] : '';
                    schedule = availableDays.map((d: string) => timeStr ? `${d} ${timeStr}` : d);
                }
            } catch {
                // API 실패 시 localStorage fallback
                try {
                    const saved = localStorage.getItem('studyPreference');
                    if (saved) {
                        const pref = JSON.parse(saved);
                        const stacks = pref.techStacks || pref.techStack || [];
                        if (stacks.length > 0) techStack = stacks;
                        availableDays = pref.availableDays || [];
                        const timeSlots = pref.preferredTimeSlots || [];
                        preferredTimeSlot = timeSlots[0] || pref.preferredTimeSlot || null;
                        preferredDurationWeeks = pref.preferredDurationWeeks || 4;

                        if (availableDays.length > 0) {
                            const timeSlotMap: Record<string, string> = {
                                morning: '07:00-12:00',
                                afternoon: '12:00-18:00',
                                evening: '18:00-22:00',
                                night: '22:00-02:00',
                            };
                            const timeStr = preferredTimeSlot ? timeSlotMap[preferredTimeSlot] : '';
                            schedule = availableDays.map((d: string) => timeStr ? `${d} ${timeStr}` : d);
                        }
                    }
                } catch { /* 무시 */ }
            }

            // 날짜 계산 (모집기간: 오늘~7일, 스터디기간: 7일후~선호주)
            const today = new Date();
            const formatDate = (d: Date) => {
                return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            };
            const recruitStart = formatDate(today);
            const recruitEndDate = new Date(today);
            recruitEndDate.setDate(recruitEndDate.getDate() + 7);
            const recruitEnd = formatDate(recruitEndDate);
            // 모집 종료 다음날부터 스터디 시작 가능 (선호 요일에 맞춰 조정)
            const studyStartDate = new Date(recruitEndDate);
            studyStartDate.setDate(studyStartDate.getDate() + 1); // 모집 종료 다음날
            // 선호 요일이 있으면 그 요일에 맞춰 시작일 조정 (한글/영문 모두 지원)
            if (availableDays.length > 0) {
                const dayMap: Record<string, number> = {
                    // 영문
                    SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6,
                    // 한글
                    '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                };
                const targetDay = dayMap[availableDays[0]];
                if (targetDay !== undefined) {
                    const currentDay = studyStartDate.getDay();
                    let daysToAdd = (targetDay - currentDay + 7) % 7;
                    // 같은 요일이면 다음 주로
                    if (daysToAdd === 0) daysToAdd = 7;
                    studyStartDate.setDate(studyStartDate.getDate() + daysToAdd);
                }
            }
            const studyStart = formatDate(studyStartDate);
            const studyEndDate = new Date(studyStartDate);
            // 스터디 종료일 = 마지막 회차 날짜 (시작일 + (회차-1) * 7일)
            studyEndDate.setDate(studyEndDate.getDate() + (preferredDurationWeeks - 1) * 7);
            const studyEnd = formatDate(studyEndDate);

            // 시간대 → 시간 변환 (각 시간대의 중간값 사용)
            // morning: 07:00-12:00 → 10:00 (중간)
            // afternoon: 12:00-18:00 → 15:00 (중간)
            // evening: 18:00-22:00 → 20:00 (중간)
            // night: 22:00-02:00 → 23:00
            const timeSlotToTime: Record<string, string> = {
                morning: '10:00',
                afternoon: '15:00',
                evening: '20:00',
                night: '23:00',
            };
            const scheduleTime = preferredTimeSlot ? timeSlotToTime[preferredTimeSlot] : '19:00';

            // 총 회차 계산: 요일수 × 주수 (요일 선택 없으면 주당 1회)
            const daysPerWeek = availableDays.length || 1;
            const totalSessions = daysPerWeek * preferredDurationWeeks;

            // 스트리밍용 변수
            let accumulatedText = '';

            // 생성 단계 메시지 (순차적으로 표시)
            const generationSteps = [
                '스터디 이름을 생성하고 있습니다...',
                '한줄 소개를 작성하고 있습니다...',
                '스터디 설명을 구성하고 있습니다...',
                '학습 목표를 설정하고 있습니다...',
                '추천 교재를 선정하고 있습니다...',
                '진행 방식을 설계하고 있습니다...',
                '커리큘럼을 구성하고 있습니다...',
                '마무리 작업 중입니다...',
            ];
            let stepIndex = 0;

            // 첫 번째 메시지 설정
            setGenerationStep(generationSteps[0]);

            // 3초마다 다음 단계 메시지로 전환 (마지막에서 멈춤)
            stepInterval = setInterval(() => {
                if (stepIndex < generationSteps.length - 1) {
                    stepIndex++;
                    setGenerationStep(generationSteps[stepIndex]);
                }
            }, 3000);

            // AI 결과를 폼에 반영하는 함수 (스트리밍 완료 시 호출 - 토픽/형식 매칭, 커리큘럼 등)
            const applyAiResult = (result: AiStudyPlanResponse) => {
                // 디버깅: AI 응답 확인
                // AI 결과를 폼에 반영
                setFormData(prev => {
                    const updated = { ...prev };
                    updated.name = result.name || `${aiTopicInput.trim()} 스터디`;
                    updated.intro = result.intro || `${aiTopicInput.trim()} 학습을 위한 스터디입니다.`;
                    updated.description = result.description || `${aiTopicInput.trim()}에 대해 함께 학습합니다.`;
                    updated.goal = result.goal || prev.goal;
                    updated.textbook = result.textbook || prev.textbook;
                    updated.prerequisites = result.prerequisites || prev.prerequisites;
                    updated.processDetail = result.processDetail || prev.processDetail;

                    // 날짜 설정
                    updated.recruitStartDate = recruitStart;
                    updated.recruitEndDate = recruitEnd;
                    updated.startDate = studyStart;
                    updated.endDate = studyEnd;

                    // 선호 요일/시간 적용
                    if (availableDays.length > 0) {
                        updated.scheduleDays = availableDays;
                    }
                    // 사용자가 직접 시간을 변경했으면 유지, 기본값(19:00)이면 AI 설정 적용
                    if (prev.scheduleTime === '19:00') {
                        updated.scheduleTime = scheduleTime;
                    }

                    // 난이도 매핑
                    if (['BEGINNER', 'INTERMEDIATE', 'ADVANCED'].includes(result.difficulty)) {
                        updated.difficulty = result.difficulty;
                    }

                    // 토픽 매칭 (키워드 기반 매칭)
                    if (result.topic && topics.length > 0) {
                        let found = false;
                        const topicLower = result.topic.toLowerCase().trim();

                        // 키워드 매핑 (AI 응답 키워드 → DB 세부주제 키워드)
                        const keywordMap: Record<string, string[]> = {
                            'spring': ['java/spring'], '스프링': ['java/spring'], 'java/spring': ['java/spring'],
                            'react': ['react'], '리액트': ['react'], 'vue': ['vue'], '뷰': ['vue'],
                            'next': ['next.js'], 'next.js': ['next.js'], 'nextjs': ['next.js'],
                            'docker': ['docker'], '도커': ['docker'],
                            'python': ['python/django', 'python/fastapi'], '파이썬': ['python/django', 'python/fastapi'],
                            'django': ['python/django'], '장고': ['python/django'], 'fastapi': ['python/fastapi'],
                            'java': ['java/spring'], '자바': ['java/spring'],
                            'node': ['node.js/express'], '노드': ['node.js/express'], 'node.js': ['node.js/express'],
                            'algorithm': ['알고리즘 이론'], '알고리즘': ['알고리즘 이론'], '알고리즘 이론': ['알고리즘 이론'],
                            'baekjoon': ['백준'], '백준': ['백준'], 'programmers': ['프로그래머스'], '프로그래머스': ['프로그래머스'],
                            'leetcode': ['leetcode'], '리트코드': ['leetcode'], 'swea': ['swea'],
                            '코딩테스트': ['코딩테스트 대비'], '자료구조': ['자료구조'],
                            'kotlin': ['kotlin', 'android (kotlin)'], '코틀린': ['kotlin', 'android (kotlin)'],
                            'android': ['android (kotlin)', 'android (java)'], '안드로이드': ['android (kotlin)', 'android (java)'],
                            'ios': ['ios (swift)'], 'swift': ['ios (swift)'], '스위프트': ['ios (swift)'],
                            'flutter': ['flutter'], '플러터': ['flutter'], 'react native': ['react native'],
                            'aws': ['aws'], 'gcp': ['gcp'], 'kubernetes': ['kubernetes'], '쿠버네티스': ['kubernetes'],
                            'ci/cd': ['ci/cd'], 'linux': ['linux'], '리눅스': ['linux'],
                            'go': ['go'], '고랭': ['go'], 'golang': ['go'],
                            'typescript': ['typescript'], '타입스크립트': ['typescript'],
                            'javascript': ['javascript'], '자바스크립트': ['javascript'],
                            'html': ['html/css'], 'css': ['html/css'], 'html/css': ['html/css'],
                            '운영체제': ['운영체제'], 'os': ['운영체제'], '네트워크': ['네트워크'], 'network': ['네트워크'],
                            '데이터베이스': ['데이터베이스'], 'database': ['데이터베이스'], 'db': ['데이터베이스'],
                            '컴퓨터구조': ['컴퓨터구조'], '디자인패턴': ['디자인패턴'], '시스템 설계': ['시스템 설계'],
                            // TDD/테스트 관련 키워드
                            'tdd': ['java/spring', 'python/django', 'node.js/express'], 'test': ['java/spring'],
                            '테스트': ['java/spring'], '테스트 주도 개발': ['java/spring'], 'unit test': ['java/spring'],
                            '단위 테스트': ['java/spring'], 'junit': ['java/spring'], 'jest': ['react', 'node.js/express'],
                            // 클린코드/리팩토링 관련
                            'clean code': ['java/spring'], '클린코드': ['java/spring'], '클린 코드': ['java/spring'],
                            'refactoring': ['java/spring'], '리팩토링': ['java/spring'], 'solid': ['java/spring'],
                            'api': ['api 설계'], 'api 설계': ['api 설계'], '모니터링': ['모니터링'],
                            '머신러닝': ['머신러닝 기초'], 'machine learning': ['머신러닝 기초'], 'ml': ['머신러닝 기초'],
                            '딥러닝': ['딥러닝'], 'deep learning': ['딥러닝'], 'dl': ['딥러닝'],
                            'nlp': ['nlp'], '자연어처리': ['nlp'], '컴퓨터 비전': ['컴퓨터 비전'],
                            'mlops': ['mlops'], '논문': ['논문 리뷰'], '논문 리뷰': ['논문 리뷰'],
                            '웹 접근성': ['웹 접근성/성능'], '웹 성능': ['웹 접근성/성능'],
                            // DevOps/인프라 관련 키워드
                            'devops': ['docker', 'kubernetes', 'ci/cd', 'aws', 'linux'],
                            '인프라': ['docker', 'kubernetes', 'ci/cd', 'aws', 'linux'],
                            'infrastructure': ['docker', 'kubernetes', 'ci/cd', 'aws', 'linux'],
                            'devops/인프라': ['docker', 'kubernetes', 'ci/cd', 'aws', 'linux'],
                            // 프론트엔드 관련 키워드
                            '프론트엔드': ['react', 'vue', 'javascript', 'typescript'],
                            'frontend': ['react', 'vue', 'javascript', 'typescript'],
                            '웹 프론트엔드': ['react', 'vue', 'javascript', 'typescript'],
                            // 백엔드 관련 키워드
                            '백엔드': ['java/spring', 'node.js/express', 'python/django'],
                            'backend': ['java/spring', 'node.js/express', 'python/django'],
                            '웹 백엔드': ['java/spring', 'node.js/express', 'python/django'],
                            // 모바일 관련 키워드
                            '모바일': ['android (kotlin)', 'ios (swift)', 'flutter'],
                            'mobile': ['android (kotlin)', 'ios (swift)', 'flutter'],
                            // CS 기초 관련 키워드
                            'cs': ['운영체제', '네트워크', '데이터베이스', '자료구조'],
                            'cs 기초': ['운영체제', '네트워크', '데이터베이스', '자료구조'],
                            '컴퓨터 과학': ['운영체제', '네트워크', '데이터베이스', '자료구조'],
                            // AI/ML 관련 키워드
                            'ai': ['머신러닝 기초', '딥러닝', 'nlp'],
                            '인공지능': ['머신러닝 기초', '딥러닝', 'nlp'],
                            'ai/ml': ['머신러닝 기초', '딥러닝', 'nlp'],
                        };

                        // 1차: 세부주제 정확 매칭
                        for (const parent of topics) {
                            const child = parent.children.find(c => c.name.toLowerCase() === topicLower);
                            if (child) {
                                updated.topicParentId = parent.id;
                                updated.topicId = child.id;
                                found = true;
                                break;
                            }
                        }

                        // 2차: 키워드 매칭
                        if (!found) {
                            for (const [keyword, dbNames] of Object.entries(keywordMap)) {
                                if (topicLower.includes(keyword) || keyword.includes(topicLower)) {
                                    for (const parent of topics) {
                                        const child = parent.children.find(c =>
                                            dbNames.some(dbName => c.name.toLowerCase() === dbName.toLowerCase())
                                        );
                                        if (child) {
                                            updated.topicParentId = parent.id;
                                            updated.topicId = child.id;
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (found) break;
                                }
                            }
                        }

                        // 3차: 부모 주제명 매칭 (첫 번째 세부주제 선택)
                        if (!found) {
                            for (const parent of topics) {
                                const parentLower = parent.name.toLowerCase();
                                if (topicLower.includes(parentLower) || parentLower.includes(topicLower) ||
                                    topicLower.split('/').some(part => parentLower.includes(part.trim())) ||
                                    parentLower.split('/').some(part => topicLower.includes(part.trim()))) {
                                    if (parent.children.length > 0) {
                                        updated.topicParentId = parent.id;
                                        updated.topicId = parent.children[0].id;
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }

                        // 4차: 기본값 설정 (매칭 실패 시 첫 번째 주제 선택)
                        if (!found && topics.length > 0 && topics[0].children.length > 0) {
                            updated.topicParentId = topics[0].id;
                            updated.topicId = topics[0].children[0].id;
                        }
                    }

                    // 형식 매칭
                    if (result.format && formats.length > 0) {
                        const formatLower = result.format.toLowerCase().trim();

                        const formatKeywordMap: Record<string, string> = {
                            '문제 풀이': '문제 풀이', '문제': '문제 풀이', '알고리즘': '문제 풀이', '코딩테스트': '문제 풀이',
                            '독서/책 스터디': '독서/책 스터디', '독서': '독서/책 스터디', '책': '독서/책 스터디',
                            '강의 수강': '강의 수강', '강의': '강의 수강', '수강': '강의 수강',
                            '프로젝트': '프로젝트', '개발': '프로젝트', 'project': '프로젝트',
                            '모의 면접': '모의 면접', '면접': '모의 면접',
                            '코드 리뷰': '코드 리뷰', '리뷰': '코드 리뷰',
                            '발표/세미나': '발표/세미나', '발표': '발표/세미나', '세미나': '발표/세미나',
                            '토론': '토론', '토의': '토론',
                            '실습': '프로젝트', 'hands-on': '프로젝트',
                        };

                        let matched = formats.find(f => f.name.toLowerCase() === formatLower);
                        if (!matched) {
                            for (const [keyword, dbFormat] of Object.entries(formatKeywordMap)) {
                                if (formatLower.includes(keyword) || keyword.includes(formatLower)) {
                                    matched = formats.find(f => f.name === dbFormat);
                                    if (matched) break;
                                }
                            }
                        }
                        // 3차: 부분 매칭 (형식명에 검색어가 포함되어 있으면)
                        if (!matched) {
                            matched = formats.find(f =>
                                f.name.toLowerCase().includes(formatLower) ||
                                formatLower.includes(f.name.toLowerCase())
                            );
                        }
                        if (matched) {
                            updated.formatId = matched.id;
                        }
                    }

                    // 총 회차 반영
                    updated.totalSessions = totalSessions;

                    // 커리큘럼 생성
                    updated.hasCurriculum = true;
                    const curriculum: CurriculumItem[] = [];
                    const dayToNum: Record<string, number> = {
                        '일': 0, '월': 1, '화': 2, '수': 3, '목': 4, '금': 5, '토': 6,
                        'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6,
                    };
                    const selectedDayNums = availableDays.map(d => dayToNum[d] ?? 0);
                    const firstDayNum = selectedDayNums[0] ?? studyStartDate.getDay();

                    const calculateSessionDate = (sessionIndex: number): Date => {
                        const date = new Date(studyStartDate);
                        if (daysPerWeek === 1 || selectedDayNums.length === 0) {
                            date.setDate(date.getDate() + sessionIndex * 7);
                        } else {
                            const weekIndex = Math.floor(sessionIndex / daysPerWeek);
                            const dayIndex = sessionIndex % daysPerWeek;
                            const targetDayNum = selectedDayNums[dayIndex];
                            let daysFromFirst = targetDayNum - firstDayNum;
                            if (daysFromFirst < 0) daysFromFirst += 7;
                            date.setDate(date.getDate() + weekIndex * 7 + daysFromFirst);
                        }
                        return date;
                    };

                    const curriculumCount = result.curriculum?.length || totalSessions;
                    for (let i = 0; i < curriculumCount; i++) {
                        const aiCurr = result.curriculum?.[i];
                        const sessionDate = calculateSessionDate(i);

                        let desc = aiCurr?.title || `${i + 1}회차 학습`;
                        if (aiCurr?.description) {
                            desc += `\n${aiCurr.description}`;
                        }

                        curriculum.push({
                            session: i + 1,
                            description: desc,
                            type: 'ONLINE',
                            date: formatDate(sessionDate),
                        });
                    }
                    updated.curriculum = curriculum;

                    if (curriculum.length > 0) {
                        updated.endDate = curriculum[curriculum.length - 1].date;
                    }

                    return updated;
                });

                setStreamingText(''); // 스트리밍 텍스트 초기화
                showToast('AI가 스터디 계획을 생성했습니다! 내용을 확인해주세요.', 'success');
                setIsAiGenerating(false);
            };

            // 스트리밍 API 호출
            await generateStudyPlanStream(
                {
                    topic: aiTopicInput.trim(),
                    techStack,
                    schedule,
                    durationWeeks: preferredDurationWeeks,
                    totalSessions,
                },
                {
                    onToken: (token) => {
                        accumulatedText += token;
                        setStreamingText(accumulatedText);
                    },
                    onComplete: (result) => {
                        if (stepInterval) clearInterval(stepInterval);
                        setGenerationStep('');
                        applyAiResult(result);
                    },
                    onError: (error) => {
                        if (stepInterval) clearInterval(stepInterval);
                        setGenerationStep('');
                        console.error('스트리밍 오류:', error);
                        setStreamingText('');
                        showToast('AI 생성에 실패했습니다. 다시 시도해주세요.', 'error');
                        setIsAiGenerating(false);
                    },
                }
            );

            // 스트리밍이 완료될 때까지 기다리므로 여기서 return
            return;
        } catch (err: any) {
            if (stepInterval) clearInterval(stepInterval);
            const message = err?.response?.data?.error?.message || 'AI 생성에 실패했습니다. 다시 시도해주세요.';
            showToast(message, 'error');
            console.error('AI 스터디 계획 생성 실패:', err);
            setGenerationStep('');
            setStreamingText('');
            setIsAiGenerating(false);
        }
    };

    // 임시저장 (템플릿 저장)
    const handleSaveTemplate = async () => {
        if (!formData.name.trim()) {
            showToast('스터디 제목을 입력해주세요.', 'error');
            return;
        }

        setIsSavingTemplate(true);
        try {
            // 현재 선택된 주제/형식 이름 가져오기
            const selectedTopic = topics.find(t => t.id === formData.topicParentId)
                ?.children.find(c => c.id === formData.topicId)?.name || '';
            const selectedFormat = formats.find(f => f.id === formData.formatId)?.name || '';

            await createTemplate({
                name: formData.name,
                intro: formData.intro || undefined,
                templateType: 'CUSTOM',
                topic: selectedTopic,
                format: selectedFormat,
                meetingType: formData.meetingType,
                description: formData.description || undefined,
                textbook: formData.textbook || undefined,
                goal: formData.goal || undefined,
                difficulty: formData.difficulty,
                prerequisites: formData.prerequisites || undefined,
                processDetail: formData.processDetail || undefined,
                penaltyPolicy: formData.penaltyPolicy,
            });

            showToast('임시저장되었습니다.', 'success');
        } catch (err: any) {
            const message = err?.response?.data?.error?.message || '임시저장에 실패했습니다.';
            showToast(message, 'error');
            console.error('임시저장 실패:', err);
        } finally {
            setIsSavingTemplate(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // topicId 필수 검증
        if (!formData.topicId) {
            showToast('세부 주제를 선택해주세요.', 'error');
            return;
        }

        setIsSubmitting(true);
        try {
            // 모집시작일 기준으로 상태 결정
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const recruitStart = formData.recruitStartDate ? new Date(formData.recruitStartDate) : null;
            let status = 'RECRUITING'; // 기본값: 모집중
            if (recruitStart && recruitStart > today) {
                status = 'PENDING'; // 모집시작일이 미래면 대기중
            }

            const payload: StudyCreatePayload = {
                name: formData.name,
                intro: formData.intro || undefined,
                description: formData.description || undefined,
                topicId: formData.topicId,
                formatId: formData.formatId || undefined,
                studyType: formData.studyType,
                meetingType: formData.meetingType,
                regionId: formData.districtId || formData.provinceId || undefined,
                scheduleDays: formData.scheduleDays.length > 0 ? formData.scheduleDays.join(',') : undefined,
                scheduleTime: formData.scheduleTime || undefined,
                maxMembers: formData.maxMembers,
                isPublic: formData.isPublic,
                penaltyPolicy: formData.penaltyPolicy,
                startDate: formData.startDate || undefined,
                endDate: formData.endDate || undefined,
                totalSessions: formData.totalSessions,
                recruitStartDate: formData.recruitStartDate || undefined,
                recruitEndDate: formData.recruitEndDate || undefined,
                textbook: formData.textbook || undefined,
                goal: formData.goal || undefined,
                difficulty: formData.difficulty,
                prerequisites: formData.prerequisites || undefined,
                processDetail: formData.processDetail || undefined,
                targetOrgType: formData.targetOrgType || undefined,
                status, // 모집기간 기준 상태
            };

            if (isEditMode && editStudyId) {
                // 수정 모드
                await updateStudy(Number(editStudyId), payload);
                showToast('스터디가 수정되었습니다!', 'success');
                // from 파라미터에 따라 이전 페이지로 이동
                const from = searchParams.get('from');
                if (from === 'manage') {
                    navigate(`/study/manage/${editStudyId}`);
                } else {
                    navigate(`/study/${editStudyId}`);
                }
            } else {
                // 생성 모드
                const createdStudy = await createStudy(payload);

                // 커리큘럼이 있으면 세션도 함께 생성
                if (formData.hasCurriculum && formData.curriculum.length > 0 && formData.startDate && createdStudy?.id) {
                    const validCurriculum = formData.curriculum.filter(c => c.description.trim() !== '');
                    if (validCurriculum.length > 0) {
                        await createStudySessions(
                            createdStudy.id,
                            validCurriculum,
                            formData.startDate,
                            formData.meetingType,
                            formData.scheduleTime  // 선호 시간 전달
                        );
                    }
                }

                showToast('스터디가 생성되었습니다!', 'success');
                navigate('/study');
            }
        } catch (err: any) {
            console.error(isEditMode ? '스터디 수정 실패:' : '스터디 생성 실패:', err);

            // validation 에러 추출
            const errorData = err?.response?.data;
            const errors: string[] = [];

            // 1. 백엔드 validation 에러 메시지 확인 (Spring Validation)
            // 형식: { error: { message: "시작일은 필수입니다" } } 또는 { message: "..." }
            const errorMsg = errorData?.error?.message || errorData?.message || '';

            // 2. 백엔드 BindingResult 형태의 에러 (필드별 에러)
            const fieldErrors = errorData?.error?.fieldErrors || errorData?.fieldErrors || errorData?.errors;

            if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
                // 필드별 에러 메시지 추출
                fieldErrors.forEach((fe: any) => {
                    const msg = fe.defaultMessage || fe.message || fe;
                    if (typeof msg === 'string' && !errors.includes(msg)) {
                        errors.push(msg);
                    }
                });
            }

            // 3. 에러 메시지 키워드 파싱 (백엔드에서 구체적 메시지를 제공한 경우)
            if (errorMsg) {
                // "시작일은 필수입니다", "종료일은 필수입니다" 등의 메시지가 있으면 추출
                if (errorMsg.includes('시작일')) {
                    errors.push('모집기간 시작일을 설정해주세요.');
                }
                if (errorMsg.includes('종료일')) {
                    errors.push('모집기간 종료일을 설정해주세요.');
                }
                if (errorMsg.includes('주제')) {
                    errors.push('스터디 주제를 선택해주세요.');
                }
                if (errorMsg.includes('스터디명')) {
                    errors.push('스터디 이름을 입력해주세요.');
                }

                // 위 키워드에 해당하지 않는 에러 메시지면 원본 추가
                if (errors.length === 0) {
                    errors.push(errorMsg);
                }
            }

            // 4. 에러가 없으면 formData 기반 검증 (프론트 폴백)
            if (errors.length === 0) {
                if (!formData.name.trim()) errors.push('스터디 이름을 입력해주세요.');
                if (!formData.topicId) errors.push('스터디 주제를 선택해주세요.');
                if (!formData.recruitStartDate) errors.push('모집기간 시작일을 설정해주세요.');
                if (!formData.recruitEndDate) errors.push('모집기간 종료일을 설정해주세요.');
            }

            // 모달 표시
            if (errors.length > 0) {
                setValidationErrors(errors);
                setShowErrorModal(true);
            } else {
                // 에러 메시지를 추출할 수 없는 경우 일반 에러 표시
                setValidationErrors(['스터디 생성에 실패했습니다. 입력 항목을 다시 확인해주세요.']);
                setShowErrorModal(true);
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    // 스타일 정의
    const styles = {
        container: "max-w-6xl mx-auto px-4 py-8",
        header: "max-w-4xl mb-8",
        card: "bg-white rounded-2xl border border-gray-200 shadow-sm p-6 md:p-8",
        section: "space-y-4",
        sectionTitle: "flex items-center gap-2 text-lg font-bold text-gray-800",
        sectionIcon: "text-primary",
        label: "block text-sm font-semibold text-gray-700 mb-1.5",
        textarea: "w-full p-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none min-h-[120px] resize-none text-base text-gray-800",
        toggleGroup: "flex gap-2 p-1 bg-gray-100 rounded-xl",
        toggleBtn: (active: boolean) => cn(
            "flex-1 py-2.5 text-sm font-semibold rounded-lg transition-all text-center",
            active ? "bg-white text-primary shadow-sm" : "text-gray-500 hover:text-gray-700"
        ),
        curriculumItem: "flex gap-3 items-center",
        curriculumBadge: "shrink-0 w-14 h-11 flex items-center justify-center bg-primary/10 text-primary text-sm font-bold rounded-xl",
        deleteBtn: "shrink-0 p-0 h-11 w-11 flex items-center justify-center text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all",
        footer: "flex flex-col sm:flex-row sm:justify-end gap-3 pt-6 border-t border-gray-100",
    };

    // 수정 모드에서 데이터 로딩 중일 때
    if (isEditMode && isLoadingStudy) {
        return (
            <UserLayoutV2>
                <div className="flex items-center justify-center min-h-[60vh]">
                    <Spinner variant="center" size="lg" label="스터디 정보를 불러오는 중..." />
                </div>
            </UserLayoutV2>
        );
    }

    return (
        <UserLayoutV2>
            <div className={styles.container}>
                {/* 헤더 */}
                <header className={styles.header}>
                    <PageNavHeader
                        title={isEditMode ? '스터디 수정하기' : '스터디 만들기'}
                        breadcrumbs={[
                            { label: '스터디', path: '/study' },
                            { label: isEditMode ? '스터디 수정하기' : '스터디 만들기' },
                        ]}
                    />
                </header>

                <form onSubmit={handleSubmit}>
                    <div className="flex flex-col lg:flex-row gap-8 items-start relative">
                        <div className="flex-1 min-w-0 space-y-6">
                            {/* AI 스터디 계획 생성 카드 */}
                            <div className={cn(styles.card, "border-primary/30 bg-gradient-to-br from-primary/5 to-transparent")}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Sparkles size={20} className="text-primary" />
                                        AI로 스터디 계획 생성하기
                                    </h2>
                                    <p className="text-sm text-gray-500 mt-1">
                                        어떤 스터디를 하고 싶은지 자유롭게 입력하면, AI가 전체 계획을 자동으로 채워드립니다.
                                    </p>

                                    <div className="mt-3 flex flex-wrap items-center gap-2">
                                        <span className="text-sm text-gray-500">예시:</span>
                                        {aiSuggestions.map((suggestion) => (
                                            <button
                                                key={suggestion}
                                                type="button"
                                                onClick={() => setAiTopicInput(suggestion)}
                                                className={cn(
                                                    "px-3 py-1.5 text-sm rounded-full border transition-all",
                                                    aiTopicInput === suggestion
                                                        ? "bg-primary text-white border-primary"
                                                        : "bg-white text-gray-600 border-gray-200 hover:border-primary hover:text-primary"
                                                )}
                                            >
                                                {suggestion}
                                            </button>
                                        ))}
                                    </div>

                                    <div className="mt-3 flex gap-3">
                                        <div className="flex-1">
                                            <Input
                                                placeholder="예: React 심화 학습, 코딩테스트 준비, Docker 입문..."
                                                value={aiTopicInput}
                                                onChange={(e) => setAiTopicInput(e.target.value)}
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') {
                                                        e.preventDefault();
                                                        handleAiGenerate();
                                                    }
                                                }}
                                                disabled={isAiGenerating}
                                            />
                                        </div>
                                        <Button
                                            type="button"
                                            variant="primary"
                                            onClick={handleAiGenerate}
                                            disabled={isAiGenerating}
                                            leftIcon={isAiGenerating ? <ButtonSpinner /> : <Sparkles size={18} />}
                                            className="shrink-0"
                                        >
                                            {isAiGenerating ? '생성 중...' : 'AI 생성'}
                                        </Button>
                                    </div>

                                    {/* 스트리밍 중 실시간 텍스트 표시 */}
                                    {isAiGenerating && (
                                        <div className="mt-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border border-blue-200">
                                            {/* 생성 단계 메시지 */}
                                            <div className="flex items-center gap-3 mb-3">
                                                <div className="flex items-center justify-center w-8 h-8 bg-blue-100 rounded-full">
                                                    <Sparkles size={16} className="text-blue-600 animate-pulse" />
                                                </div>
                                                <span className="text-sm font-medium text-blue-700 animate-pulse">
                                                    {generationStep || 'AI가 스터디 계획을 분석하고 있습니다...'}
                                                </span>
                                            </div>
                                            {/* 스트리밍 텍스트 */}
                                            {streamingText && (
                                                <pre className="text-xs text-gray-500 whitespace-pre-wrap font-mono max-h-40 overflow-y-auto bg-white/50 p-3 rounded border border-gray-200">
                                                    {streamingText}
                                                    <span className="animate-pulse text-blue-500">▌</span>
                                                </pre>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* 기본 정보 카드 */}
                            <div id="basic-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Info size={20} className={styles.sectionIcon} />
                                        기본 정보
                                    </h2>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                        <div className="md:col-span-2">
                                            <Input
                                                label="제목"
                                                name="name"
                                                placeholder="모두가 이해하기 쉬운 이름을 지어주세요"
                                                required
                                                value={formData.name}
                                                onChange={handleChange}
                                            />
                                        </div>

                                        <div className="md:col-span-2">
                                            <Input
                                                label="한줄 소개 (썸네일용)"
                                                name="intro"
                                                placeholder="스터디를 한 문장으로 소개해 주세요"
                                                required
                                                value={formData.intro}
                                                onChange={handleChange}
                                            />
                                        </div>

                                        <div className="md:col-span-2">
                                            <label className={styles.label}>상세 설명</label>
                                            <textarea
                                                name="description"
                                                className={styles.textarea}
                                                placeholder="스터디의 목표와 진행 방식에 대해 자세히 적어주세요"
                                                required
                                                value={formData.description}
                                                onChange={handleChange}
                                            />
                                        </div>

                                        {/* 스터디 형식 */}
                                        <div className="md:col-span-2">
                                            <label className={styles.label}>스터디 형식</label>
                                            <div className="grid grid-cols-4 gap-2">
                                                {(formats.length > 0 ? formats : FALLBACK_FORMATS.map((name, i) => ({ id: i, name, description: null, icon: null, sortOrder: i }))).map((format) => (
                                                    <button
                                                        key={typeof format === 'string' ? format : format.id}
                                                        type="button"
                                                        onClick={() => setFormData(prev => ({ ...prev, formatId: typeof format === 'string' ? null : format.id }))}
                                                        className={cn(
                                                            "py-2.5 px-3 rounded-xl text-center transition-all border-2 text-sm font-medium",
                                                            formData.formatId === (typeof format === 'string' ? null : format.id)
                                                                ? "border-primary bg-primary/10 text-primary"
                                                                : "border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300"
                                                        )}
                                                    >
                                                        {typeof format === 'string' ? format : format.name}
                                                    </button>
                                                ))}
                                            </div>
                                        </div>

                                        <div>
                                            <Select
                                                label="주제"
                                                value={topics.find(t => t.id === formData.topicParentId)?.name || ''}
                                                onChange={(val) => {
                                                    const selected = topics.find(t => t.name === val);
                                                    handleTopicParentChange(selected ? selected.id : null);
                                                }}
                                                options={topics.map(t => t.name)}
                                                placeholder="주제 선택"
                                            />
                                        </div>

                                        <div>
                                            <Select
                                                label="세부 주제"
                                                value={
                                                    topics.find(t => t.id === formData.topicParentId)
                                                        ?.children.find(c => c.id === formData.topicId)?.name || ''
                                                }
                                                onChange={(val) => {
                                                    const parent = topics.find(t => t.id === formData.topicParentId);
                                                    const child = parent?.children.find(c => c.name === val);
                                                    setFormData(prev => ({ ...prev, topicId: child ? child.id : null }));
                                                }}
                                                options={
                                                    (topics.find(t => t.id === formData.topicParentId)?.children || []).map(c => c.name)
                                                }
                                                placeholder="세부 주제 선택"
                                                disabled={!formData.topicParentId}
                                            />
                                        </div>

                                        <div>
                                            <label className={styles.label}>최대 인원</label>
                                            <div className="flex items-center justify-center gap-2">
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        const minMembers = isEditMode ? currentMemberCount : 2;
                                                        setFormData(prev => ({ ...prev, maxMembers: Math.max(minMembers, prev.maxMembers - 1) }));
                                                    }}
                                                    disabled={isEditMode && formData.maxMembers <= currentMemberCount}
                                                    className={cn(
                                                        "w-10 h-10 flex items-center justify-center rounded-lg font-bold transition-all",
                                                        isEditMode && formData.maxMembers <= currentMemberCount
                                                            ? "bg-gray-100 text-gray-300 cursor-not-allowed"
                                                            : "bg-gray-100 hover:bg-gray-200 text-gray-600"
                                                    )}
                                                >
                                                    −
                                                </button>
                                                <input
                                                    type="number"
                                                    value={formData.maxMembers || ''}
                                                    onChange={(e) => {
                                                        const val = parseInt(e.target.value);
                                                        if (isNaN(val)) {
                                                            setFormData(prev => ({ ...prev, maxMembers: 0 }));
                                                        } else {
                                                            setFormData(prev => ({ ...prev, maxMembers: Math.min(50, val) }));
                                                        }
                                                    }}
                                                    onBlur={() => {
                                                        const minMembers = isEditMode ? currentMemberCount : 2;
                                                        setFormData(prev => ({ ...prev, maxMembers: Math.max(minMembers, prev.maxMembers) }));
                                                    }}
                                                    className="flex-1 text-center py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                                                />
                                                <button
                                                    type="button"
                                                    onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.min(50, prev.maxMembers + 1) }))}
                                                    className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                                >
                                                    +
                                                </button>
                                            </div>
                                            {isEditMode && currentMemberCount > 1 && (
                                                <p className="text-xs text-primary mt-1">
                                                    현재 {currentMemberCount}명이 참여 중이므로 최소 인원은 {currentMemberCount}명입니다.
                                                </p>
                                            )}
                                        </div>

                                        <div>
                                            <label className={styles.label}>총 스터디 횟수</label>
                                            <div className="flex items-center justify-center gap-2">
                                                <button
                                                    type="button"
                                                    onClick={() => setFormData(prev => ({
                                                        ...prev,
                                                        totalSessions: Math.max(prev.curriculum.length, prev.totalSessions - 1)
                                                    }))}
                                                    className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                                    disabled={formData.totalSessions <= formData.curriculum.length}
                                                >
                                                    −
                                                </button>
                                                <input
                                                    type="number"
                                                    value={formData.totalSessions || ''}
                                                    onChange={(e) => {
                                                        const val = parseInt(e.target.value);
                                                        if (isNaN(val)) {
                                                            setFormData(prev => ({ ...prev, totalSessions: 0 }));
                                                        } else {
                                                            setFormData(prev => ({ ...prev, totalSessions: Math.min(100, val) }));
                                                        }
                                                    }}
                                                    onBlur={() => setFormData(prev => ({
                                                        ...prev,
                                                        totalSessions: Math.max(prev.curriculum.length, prev.totalSessions)
                                                    }))}
                                                    className="flex-1 text-center py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                                                />
                                                <button
                                                    type="button"
                                                    onClick={() => setFormData(prev => ({ ...prev, totalSessions: Math.min(100, prev.totalSessions + 1) }))}
                                                    className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
                                                >
                                                    +
                                                </button>
                                            </div>
                                            {formData.curriculum.length > 0 && (
                                                <p className="text-xs text-gray-500 mt-1">
                                                    현재 {formData.curriculum.length}회차 커리큘럼이 등록되어 있습니다.
                                                </p>
                                            )}
                                        </div>

                                        <div>
                                            <div className="flex items-center gap-1.5 mb-1.5">
                                                <label className="text-sm font-semibold text-gray-700">예상 스터디 난이도</label>
                                                <button
                                                    type="button"
                                                    onClick={() => setShowDifficultyInfo(prev => !prev)}
                                                    className="text-gray-400 hover:text-primary transition-colors"
                                                    aria-label="난이도 설명 보기"
                                                >
                                                    <Info size={15} />
                                                </button>
                                            </div>
                                            {showDifficultyInfo && (
                                                <div className="mb-3 p-3 bg-blue-50 border border-blue-200 rounded-xl text-sm text-blue-800 space-y-1.5">
                                                    <p className="font-semibold">난이도는 참여자 매칭에 사용됩니다.</p>
                                                    <ul className="space-y-1 text-blue-700">
                                                        <li><strong>입문</strong> — 해당 분야 경험이 없는 분도 참여 가능</li>
                                                        <li><strong>중급</strong> — 기초 지식이 있고 실전 경험을 쌓고 싶은 분</li>
                                                        <li><strong>고급</strong> — 실무 경험이 있거나 심화 학습을 원하는 분</li>
                                                    </ul>
                                                    <p className="text-xs text-blue-500 pt-1">스터디 추천 시 사용자 수준과 매칭하는 데 활용됩니다.</p>
                                                </div>
                                            )}
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
                                                    <Select
                                                        label="시/도"
                                                        value={provinces.find(p => p.id === formData.provinceId)?.name || ''}
                                                        onChange={(val) => {
                                                            const selected = provinces.find(p => p.name === val);
                                                            handleProvinceChange(selected ? selected.id : null);
                                                        }}
                                                        options={provinces.map(p => p.name)}
                                                        placeholder="시/도 선택"
                                                    />
                                                </div>
                                                <div>
                                                    <Select
                                                        label="구/군"
                                                        value={districts.find(d => d.id === formData.districtId)?.name || ''}
                                                        onChange={(val) => {
                                                            const selected = districts.find(d => d.name === val);
                                                            setFormData(prev => ({ ...prev, districtId: selected ? selected.id : null }));
                                                        }}
                                                        options={districts.map(d => d.name)}
                                                        placeholder="구/군 선택"
                                                        disabled={!formData.provinceId}
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* 일정 및 모집 카드 */}
                            <div id="schedule-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Clock size={20} className={styles.sectionIcon} />
                                        일정 및 모집
                                    </h2>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                        {/* 모임 시간 */}
                                        <div>
                                            <label className={styles.label}>모임 시간</label>
                                            <TimePicker
                                                value={formData.scheduleTime}
                                                onChange={(val) => setFormData(prev => ({ ...prev, scheduleTime: val }))}
                                            />
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
                                            minDate={formattedToday}
                                            onRangeChange={handleRecruitDateRangeChange}
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 커리큘럼 등록 여부 카드 */}
                            <div id="curriculum-info" className={styles.card}>
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

                            {/* 스터디 기간 카드 - 항상 표시 */}
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
                                            minDate={formData.recruitEndDate}
                                            onRangeChange={handleDateRangeChange}
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 커리큘럼 등록 선택 시에만 표시 */}
                            {formData.hasCurriculum && (
                                <>
                                    {/* 커리큘럼 카드 */}
                                    <div className={styles.card}>
                                        <div className={styles.section}>
                                            <h2 className={styles.sectionTitle}>
                                                <BookOpen size={20} className={styles.sectionIcon} />
                                                커리큘럼 계획
                                            </h2>
                                            <p className="text-sm text-gray-500 mt-1">
                                                회차별 학습 목표를 설정하면 참여자들이 스터디 방향을 파악하는 데 도움이 됩니다.
                                                <span className="text-primary font-medium ml-1">
                                                    ({formData.curriculum.filter(c => c.date || c.description?.trim()).length}회차 작성됨 / 총 {formData.curriculum.length}회차)
                                                </span>
                                            </p>

                                            <p className="text-xs text-gray-400 mt-2">
                                                💡 왼쪽 ⋮⋮ 아이콘을 드래그하여 회차 순서를 변경할 수 있습니다. 순서 변경 시 날짜가 자동으로 재배치됩니다.
                                            </p>

                                            <div className="space-y-3 mt-4">
                                                {formData.curriculum.map((item, index) => (
                                                    <div
                                                        key={index}
                                                        className={cn(
                                                            styles.curriculumItem,
                                                            "cursor-grab active:cursor-grabbing",
                                                            draggedIndex === index && "opacity-50 border-primary border-2",
                                                            draggedIndex !== null && draggedIndex !== index && "border-dashed"
                                                        )}
                                                        draggable
                                                        onDragStart={() => handleDragStart(index)}
                                                        onDragOver={(e) => handleDragOver(e, index)}
                                                        onDrop={() => handleDrop(index)}
                                                        onDragEnd={handleDragEnd}
                                                    >
                                                        {/* 드래그 핸들 */}
                                                        <div
                                                            className="shrink-0 flex items-center justify-center w-8 h-11 text-gray-400 hover:text-gray-600 cursor-grab active:cursor-grabbing"
                                                            title="드래그하여 순서 변경"
                                                        >
                                                            <GripVertical size={20} />
                                                        </div>
                                                        <div className={styles.curriculumBadge}>
                                                            {item.session}회차
                                                        </div>
                                                        <div className="shrink-0 w-40">
                                                            <DatePicker
                                                                value={item.date || ''}
                                                                min={formData.startDate || undefined}
                                                                max={formData.endDate || undefined}
                                                                onChange={(date) => handleCurriculumChange(index, 'date', date)}
                                                                placeholder="날짜 선택"
                                                            />
                                                        </div>
                                                        <div className="flex-1">
                                                            <Input
                                                                className="py-2.5 h-11"
                                                                placeholder="학습 목표나 주제를 입력하세요"
                                                                value={item.description}
                                                                onChange={(e) => handleCurriculumChange(index, 'description', e.target.value)}
                                                            />
                                                        </div>
                                                        {/* 혼합 방식일 때 온라인/오프라인 선택 */}
                                                        {formData.meetingType === 'HYBRID' && (
                                                            <div className="shrink-0 w-32">
                                                                <Select
                                                                    value={item.type || 'ONLINE'}
                                                                    onChange={(val) => handleCurriculumChange(index, 'type', val)}
                                                                    options={[
                                                                        { value: 'ONLINE', label: '온라인' },
                                                                        { value: 'OFFLINE', label: '오프라인' }
                                                                    ]}
                                                                    className="mb-0"
                                                                    buttonClassName="h-11 py-0 px-3 text-sm"
                                                                />
                                                            </div>
                                                        )}
                                                        <button
                                                            type="button"
                                                            className={styles.deleteBtn}
                                                            onClick={() => removeSession(index)}
                                                        >
                                                            <Trash2 size={18} />
                                                        </button>
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
                                                    {formData.curriculum.length + 1}회차 추가하기
                                                </Button>
                                            </div>
                                        </div>
                                    </div>
                                </>
                            )}

                            {/* 추가 정보 카드 */}
                            <div id="additional-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Target size={20} className={styles.sectionIcon} />
                                        추가 정보(선택)
                                    </h2>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                        <div className="md:col-span-2">
                                            <Input
                                                label="최종 스터디 목표"
                                                name="goal"
                                                placeholder="예: 골드 티어 달성, 토이 프로젝트 완성"
                                                value={formData.goal}
                                                onChange={handleChange}
                                            />
                                        </div>

                                        <div className="md:col-span-2">
                                            <Input
                                                label="교재/자료"
                                                name="textbook"
                                                placeholder="예: 백준 온라인 저지, Do It! 알고리즘"
                                                value={formData.textbook}
                                                onChange={handleChange}
                                                helperText={formData.textbook ? undefined : "예: 공식 문서, 인프런 강의, 교재명 등"}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>



                            {/* 스터디 규칙 카드 */}
                            <div id="rule-info" className={styles.card}>
                                <div className={styles.section}>
                                    <h2 className={styles.sectionTitle}>
                                        <Shield size={20} className={styles.sectionIcon} />
                                        스터디 규칙
                                    </h2>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                                        {/* 패널티 정책 */}
                                        <div className="md:col-span-2">
                                            <div className="flex items-center justify-between mb-2">
                                                <label className={cn(styles.label, "mb-0")}>패널티 정책</label>
                                                <div className="flex flex-col items-end gap-1">
                                                    <span className="text-xs text-red-500 font-medium">*남은 회차 모두 출석해도 참여율이 50% 미만이면 강제 탈퇴됩니다.</span>
                                                    <span className="text-xs text-red-500 font-medium">*3회 지각시 1회 결석으로 처리됩니다.</span>
                                                </div>
                                            </div>
                                            <div className="grid grid-cols-3 gap-2">
                                                {[
                                                    { key: 'STRICT', label: '엄격', desc: '1회 결석 시 강퇴', activeClass: 'border-red-300 bg-red-50 text-red-600' },
                                                    { key: 'NORMAL', label: '보통', desc: '경고 후 조치', activeClass: 'border-red-300 bg-red-50 text-red-600' },
                                                    { key: 'NONE', label: '없음', desc: '자율 참여', activeClass: 'border-red-300 bg-red-50 text-red-600' }
                                                ].map(({ key, label, desc, activeClass }) => (
                                                    <button
                                                        key={key}
                                                        type="button"
                                                        onClick={() => handleOptionToggle('penaltyPolicy', key)}
                                                        className={cn(
                                                            "p-3 rounded-xl text-center transition-all border-2",
                                                            formData.penaltyPolicy === key
                                                                ? activeClass
                                                                : "border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300"
                                                        )}
                                                    >
                                                        <div className="font-bold text-sm">{label}</div>
                                                        <div className="text-xs mt-1 opacity-70">{desc}</div>
                                                    </button>
                                                ))}
                                            </div>
                                        </div>

                                        {/* 선행 조건 */}
                                        <div className="md:col-span-2">
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
                                    type="button"
                                    variant="ghost"
                                    size="lg"
                                    className="flex-1 sm:flex-none sm:min-w-[120px] border border-gray-300"
                                    onClick={handleSaveTemplate}
                                    disabled={isSavingTemplate}
                                >
                                    {isSavingTemplate ? '저장 중...' : '임시저장'}
                                </Button>
                                <Button
                                    type="submit"
                                    variant="primary"
                                    size="lg"
                                    className="flex-1 sm:flex-none sm:min-w-[200px] shadow-lg shadow-primary/20"
                                    disabled={isSubmitting}
                                >
                                    {isSubmitting
                                        ? (isEditMode ? '수정 중...' : '생성 중...')
                                        : (isEditMode ? '스터디 수정하기' : '스터디 개설하기')}
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
                                                ? "bg-primary/5 text-primary font-bold"
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
            {/* 저장된 템플릿 선택 모달 */}
            {showTemplateModal && savedTemplates.length > 0 && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white rounded-2xl shadow-xl max-w-md w-full mx-4 overflow-hidden">
                        <div className="p-6">
                            <h3 className="text-lg font-bold text-gray-900 mb-2">
                                저장된 스터디 템플릿이 있습니다
                            </h3>
                            <p className="text-sm text-gray-500 mb-4">
                                이전에 생성한 템플릿을 불러와서 시작하시겠습니까?
                            </p>
                            <div className="max-h-60 overflow-y-auto space-y-2">
                                {savedTemplates.map((template) => (
                                    <button
                                        key={template.id}
                                        type="button"
                                        onClick={() => applyTemplate(template)}
                                        className="w-full p-3 text-left rounded-xl border border-gray-200 hover:border-primary hover:bg-primary/5 transition-all"
                                    >
                                        <div className="font-semibold text-gray-800">{template.name}</div>
                                        <div className="text-sm text-gray-500 mt-1 line-clamp-2">
                                            {template.topic && <span className="mr-2">#{template.topic}</span>}
                                            {template.difficulty && <span className="mr-2">{template.difficulty}</span>}
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="px-6 py-4 bg-gray-50 flex justify-end gap-2">
                            <Button
                                variant="ghost"
                                onClick={() => setShowTemplateModal(false)}
                            >
                                새로 작성
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* 스터디 선호 설정 필요 모달 */}
            {showPreferenceModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white rounded-2xl shadow-xl max-w-xl w-full mx-4 overflow-hidden">
                        <div className="p-6">
                            <div className="flex items-center gap-3 mb-4">
                                <div className="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center">
                                    <AlertCircle size={24} className="text-amber-500" />
                                </div>
                                <h3 className="text-lg font-bold text-gray-900">
                                    스터디 선호 설정이 필요합니다
                                </h3>
                            </div>
                            <p className="text-sm text-gray-600 mb-2">
                                AI가 맞춤형 스터디 계획을 생성하려면 먼저 일정 설정을 완료해주세요.
                            </p>
                            <p className="text-sm text-gray-500">
                                기술 스택, 가용 일정, 선호 기간 등을 설정하면 더 정확한 추천을 받을 수 있습니다.
                            </p>
                        </div>
                        <div className="px-6 py-4 bg-gray-50 flex justify-end gap-2">
                            <Button
                                variant="ghost"
                                onClick={() => setShowPreferenceModal(false)}
                            >
                                취소
                            </Button>
                            <Button
                                variant="primary"
                                onClick={() => {
                                    setShowPreferenceModal(false);
                                    navigate('/setting', { state: { section: 'study' } });
                                }}
                            >
                                설정하러 가기
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* 입력 필수 항목 에러 모달 */}
            {showErrorModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white rounded-2xl shadow-xl max-w-md w-full mx-4 overflow-hidden">
                        <div className="p-6">
                            <div className="flex items-center gap-3 mb-4">
                                <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
                                    <AlertCircle size={24} className="text-red-500" />
                                </div>
                                <h3 className="text-lg font-bold text-gray-900">
                                    필수 항목을 입력해주세요
                                </h3>
                            </div>
                            <div className="space-y-2 mb-4">
                                {validationErrors.map((error, idx) => (
                                    <div key={idx} className="flex items-center gap-2 text-sm text-gray-700">
                                        <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                                        {error}
                                    </div>
                                ))}
                            </div>
                            <p className="text-sm text-gray-500">
                                템플릿을 불러온 경우, 일정 및 모집에서 모집기간을 설정해주세요.
                            </p>
                        </div>
                        <div className="px-6 py-4 bg-gray-50 flex justify-end">
                            <Button
                                variant="primary"
                                onClick={() => setShowErrorModal(false)}
                            >
                                확인
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </UserLayoutV2>
    );
};

export default StudyCreatePage;
