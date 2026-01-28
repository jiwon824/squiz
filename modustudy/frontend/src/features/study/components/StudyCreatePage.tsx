import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Info, Calendar, Plus, Trash2, BookOpen, MapPin, AlertCircle, Clock, Users, Target, Shield, Sparkles, Loader2 } from 'lucide-react';
import { MainLayout } from '@/layouts/MainLayout';
import { useUIStore } from '@/store/uiStore';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { cn } from '@/shared/utils/cn';
import { DateRangePicker } from './DateRangePicker';
import { DatePicker, TimePicker } from '@/shared/components';
import {
    getTopics, getFormats, getProvinces, getDistricts, createStudy, generateStudyPlan, getMyTemplates, createTemplate,
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
    const showToast = useUIStore((state) => state.showToast);
    const { user } = useAuthStore();
    const hasCheckedSavedData = useRef(false);

    // API 데이터 상태
    const [topics, setTopics] = useState<TopicParent[]>([]);
    const [formats, setFormats] = useState<FormatItem[]>([]);
    const [provinces, setProvinces] = useState<RegionItem[]>([]);
    const [districts, setDistricts] = useState<RegionItem[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [aiTopicInput, setAiTopicInput] = useState('');
    const [isAiGenerating, setIsAiGenerating] = useState(false);
    const [showDifficultyInfo, setShowDifficultyInfo] = useState(false);
    const [savedTemplates, setSavedTemplates] = useState<StudyTemplateItem[]>([]);
    const [showTemplateModal, setShowTemplateModal] = useState(false);
    const [isSavingTemplate, setIsSavingTemplate] = useState(false);

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

    // DB에서 저장된 템플릿 확인
    useEffect(() => {
        if (!user?.id || hasCheckedSavedData.current) return;
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
                console.log('저장된 템플릿 없음');
            }
        };
        checkSavedTemplates();
    }, [user?.id]);

    // 템플릿 선택 시 폼에 적용
    const applyTemplate = (template: StudyTemplateItem) => {
        setFormData(prev => ({
            ...prev,
            name: template.name || prev.name,
            description: template.description || prev.description,
            goal: template.goal || prev.goal,
            textbook: template.textbook || prev.textbook,
            prerequisites: template.prerequisites || prev.prerequisites,
            processDetail: template.processDetail || prev.processDetail,
            difficulty: template.difficulty || prev.difficulty,
        }));
        setShowTemplateModal(false);
        showToast('템플릿을 불러왔습니다.', 'success');
    };

    // 사용자 기술스택 기반 추천
    const [userTechStack, setUserTechStack] = useState<string[]>([]);

    useEffect(() => {
        const loadTechStack = async () => {
            try {
                // API에서 기술스택 가져오기
                const pref: any = await getStudyPreference();
                const techStacks = pref.techStacks || pref.techStack || [];
                if (techStacks.length > 0) {
                    setUserTechStack(techStacks);
                    return;
                }
            } catch { /* API 실패 시 localStorage fallback */ }

            // localStorage fallback
            try {
                const saved = localStorage.getItem('studyPreference');
                if (saved) {
                    const pref = JSON.parse(saved);
                    const stack = pref.techStacks || pref.techStack || [];
                    if (stack.length > 0) setUserTechStack(stack);
                }
            } catch { /* 무시 */ }
        };
        loadTechStack();
    }, []);

    // 선호 설정 여부 확인
    const hasStudyPreference = userTechStack.length > 0;

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

    const handleProvinceChange = (provinceId: number | null) => {
        setFormData(prev => ({ ...prev, provinceId, districtId: null }));
    };

    const handleTopicParentChange = (parentId: number | null) => {
        setFormData(prev => ({ ...prev, topicParentId: parentId, topicId: null }));
    };

    const handleDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => ({ ...prev, startDate: start, endDate: end }));
    };

    const handleRecruitDateRangeChange = (start: string | null, end: string | null) => {
        setFormData(prev => ({ ...prev, recruitStartDate: start, recruitEndDate: end }));
    };

    const handleCurriculumChange = (index: number, field: keyof CurriculumItem, value: string) => {
        const newCurriculum = [...formData.curriculum];
        newCurriculum[index] = { ...newCurriculum[index], [field]: value };
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const addSession = () => {
        setFormData(prev => ({
            ...prev,
            curriculum: [...prev.curriculum, {
                session: prev.curriculum.length + 1,
                description: '',
                type: 'ONLINE',
                date: ''
            }]
        }));
    };

    const removeSession = (index: number) => {
        const newCurriculum = formData.curriculum
            .filter((_, i) => i !== index)
            .map((item, i) => ({ ...item, session: i + 1 }));
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    // AI 스터디 계획 생성
    const handleAiGenerate = async () => {
        if (!aiTopicInput.trim()) {
            showToast('스터디 주제를 입력해주세요.', 'error');
            return;
        }

        // 선호 설정 안한 경우 안내
        if (!hasStudyPreference) {
            const confirmed = window.confirm(
                '스터디 선호 설정을 먼저 완료해주세요.\n\n' +
                '선호 설정을 하면 AI가 맞춤형 스터디 계획을 생성할 수 있습니다.\n\n' +
                '설정 페이지로 이동하시겠습니까?'
            );
            if (confirmed) {
                navigate('/settings/study-preference');
            }
            return;
        }

        setIsAiGenerating(true);
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
            // 선호 요일이 있으면 그 요일에 맞춰 시작일 조정
            if (availableDays.length > 0) {
                const dayMap: Record<string, number> = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };
                const targetDay = dayMap[availableDays[0]];
                if (targetDay !== undefined) {
                    const currentDay = studyStartDate.getDay();
                    const daysToAdd = (targetDay - currentDay + 7) % 7;
                    if (daysToAdd > 0 && daysToAdd <= 7) {
                        studyStartDate.setDate(studyStartDate.getDate() + daysToAdd);
                    }
                }
            }
            const studyStart = formatDate(studyStartDate);
            const studyEndDate = new Date(studyStartDate);
            studyEndDate.setDate(studyEndDate.getDate() + preferredDurationWeeks * 7);
            const studyEnd = formatDate(studyEndDate);

            // 시간대 → 시간 변환
            const timeSlotToTime: Record<string, string> = {
                morning: '10:00',
                afternoon: '14:00',
                evening: '19:00',
                night: '22:00',
            };
            const scheduleTime = preferredTimeSlot ? timeSlotToTime[preferredTimeSlot] : '19:00';

            const result: AiStudyPlanResponse = await generateStudyPlan({
                topic: aiTopicInput.trim(),
                techStack,
                schedule,
                durationWeeks: preferredDurationWeeks,
            });

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
                updated.scheduleTime = scheduleTime;

                // 난이도 매핑
                if (['BEGINNER', 'INTERMEDIATE', 'ADVANCED'].includes(result.difficulty)) {
                    updated.difficulty = result.difficulty;
                }

                // 토픽 매칭 (세부주제명으로 검색 - 정확 매칭 → 부분 매칭)
                if (result.topic && topics.length > 0) {
                    let found = false;
                    const topicLower = result.topic.toLowerCase();
                    // 1차: 정확 매칭
                    for (const parent of topics) {
                        const child = parent.children.find(c => c.name === result.topic);
                        if (child) {
                            updated.topicParentId = parent.id;
                            updated.topicId = child.id;
                            found = true;
                            break;
                        }
                    }
                    // 2차: 부분 매칭 (포함 검색)
                    if (!found) {
                        for (const parent of topics) {
                            const child = parent.children.find(c =>
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
                }

                // 형식 매칭 (형식명으로 검색 - 정확 매칭 → 부분 매칭)
                if (result.format && formats.length > 0) {
                    const formatLower = result.format.toLowerCase();
                    let matched = formats.find(f => f.name === result.format);
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

                // 선호 기간(주) → 총 회차 반영
                updated.totalSessions = preferredDurationWeeks;

                // AI 일정 제안은 사용자 선호가 없을 때만 적용
                if (result.scheduleSuggestion && availableDays.length === 0) {
                    if (result.scheduleSuggestion.days?.length > 0) {
                        updated.scheduleDays = result.scheduleSuggestion.days;
                    }
                    if (result.scheduleSuggestion.time) {
                        const timePart = result.scheduleSuggestion.time.split('-')[0];
                        if (timePart) updated.scheduleTime = timePart;
                    }
                }

                // 커리큘럼 생성 (AI 응답 또는 기본 틀)
                updated.hasCurriculum = true;
                const curriculum: CurriculumItem[] = [];

                if (result.curriculum && result.curriculum.length > 0) {
                    // AI가 생성한 커리큘럼 사용
                    for (let i = 0; i < result.curriculum.length; i++) {
                        const aiCurr = result.curriculum[i];
                        const sessionDate = new Date(recruitEndDate);
                        sessionDate.setDate(sessionDate.getDate() + i * 7);

                        // AI 커리큘럼 내용을 description에 통합
                        let desc = aiCurr.title || `${i + 1}주차 학습`;
                        if (aiCurr.description) {
                            desc += `\n${aiCurr.description}`;
                        }
                        if (aiCurr.learning_goals && aiCurr.learning_goals.length > 0) {
                            desc += `\n\n📌 학습 목표:\n${aiCurr.learning_goals.map(g => `• ${g}`).join('\n')}`;
                        }
                        if (aiCurr.assignments && aiCurr.assignments.length > 0) {
                            desc += `\n\n📝 과제:\n${aiCurr.assignments.map(a => `• ${a}`).join('\n')}`;
                        }
                        if (aiCurr.resources && aiCurr.resources.length > 0) {
                            desc += `\n\n📚 참고 자료:\n${aiCurr.resources.map(r => `• ${r}`).join('\n')}`;
                        }

                        curriculum.push({
                            session: i + 1,
                            description: desc,
                            type: 'ONLINE',
                            date: formatDate(sessionDate),
                        });
                    }
                } else {
                    // AI 커리큘럼이 없으면 기본 틀 생성
                    for (let i = 1; i <= preferredDurationWeeks; i++) {
                        const sessionDate = new Date(recruitEndDate);
                        sessionDate.setDate(sessionDate.getDate() + (i - 1) * 7);
                        curriculum.push({
                            session: i,
                            description: `${i}주차 학습`,
                            type: 'ONLINE',
                            date: formatDate(sessionDate),
                        });
                    }
                }
                updated.curriculum = curriculum;

                return updated;
            });

            showToast('AI가 스터디 계획을 생성했습니다! 내용을 확인해주세요.', 'success');
        } catch (err: any) {
            const message = err?.response?.data?.error?.message || 'AI 생성에 실패했습니다. 다시 시도해주세요.';
            showToast(message, 'error');
            console.error('AI 스터디 계획 생성 실패:', err);
        } finally {
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
            };

            await createStudy(payload);
            showToast('스터디가 생성되었습니다!', 'success');
            navigate('/study');
        } catch (err: any) {
            const message = err?.response?.data?.error?.message || '스터디 생성에 실패했습니다.';
            showToast(message, 'error');
            console.error('스터디 생성 실패:', err);
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
                                            disabled={isAiGenerating || !aiTopicInput.trim()}
                                            leftIcon={isAiGenerating ? <Loader2 size={18} className="animate-spin" /> : <Sparkles size={18} />}
                                            className="shrink-0"
                                        >
                                            {isAiGenerating ? '생성 중...' : 'AI 생성'}
                                        </Button>
                                    </div>
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
                                                    onClick={() => setFormData(prev => ({ ...prev, maxMembers: Math.max(2, prev.maxMembers - 1) }))}
                                                    className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg text-gray-600 font-bold transition-all"
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
                                                    onBlur={() => setFormData(prev => ({ ...prev, maxMembers: Math.max(2, prev.maxMembers) }))}
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
                                                    onBlur={() => setFormData(prev => ({ ...prev, totalSessions: Math.max(1, prev.totalSessions) }))}
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
                                                    minDate={formData.recruitEndDate}
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
                                                    { key: 'STRICT', label: '엄격', desc: '1회 결석 시 강퇴', activeClass: 'border-red-600 bg-red-100 text-red-800' },
                                                    { key: 'NORMAL', label: '보통', desc: '경고 후 조치', activeClass: 'border-red-300 bg-red-50 text-red-600' },
                                                    { key: 'NONE', label: '없음', desc: '자율 참여', activeClass: 'border-gray-300 bg-gray-50 text-gray-500' }
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
                                    {isSubmitting ? '생성 중...' : '스터디 개설하기'}
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
        </MainLayout>
    );
};

export default StudyCreatePage;
