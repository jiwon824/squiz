/**
 * StudyPreferenceSection 컴포넌트
 * 스터디 선호 설정 (기술스택, 가용 일정, 선호 기간)을 관리합니다.
 * AI 스터디 추천 및 계획 생성 시 활용됩니다.
 */

import { useState, useEffect, useCallback } from 'react';
import { BookOpen, X, Plus, Check, Info } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';
import { getStudyPreference, updateStudyPreference } from '../api/settingApi';
import type { DayOfWeek, TimeSlot, StudyPreference } from '../types';

// 요일 목록
const DAYS: DayOfWeek[] = ['월', '화', '수', '목', '금', '토', '일'];

// 시간대 옵션
const TIME_SLOTS: { value: TimeSlot; label: string; desc: string }[] = [
    { value: 'morning', label: '오전', desc: '06:00 ~ 12:00' },
    { value: 'afternoon', label: '오후', desc: '12:00 ~ 18:00' },
    { value: 'evening', label: '저녁', desc: '18:00 ~ 22:00' },
    { value: 'night', label: '심야', desc: '22:00 ~ 02:00' },
];

// 기간 옵션 (주 단위)
const DURATION_OPTIONS = [2, 3, 4, 5, 6, 7, 8];

// 추천 기술스택 (자동완성용)
const SUGGESTED_TECHS = [
    'Java', 'Spring Boot', 'JPA', 'Python', 'Django', 'FastAPI',
    'JavaScript', 'TypeScript', 'React', 'Next.js', 'Vue', 'Angular',
    'Node.js', 'Express', 'NestJS', 'Go', 'Kotlin', 'Swift',
    'Docker', 'Kubernetes', 'AWS', 'GCP', 'Linux', 'Git',
    'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'Kafka',
    'PyTorch', 'TensorFlow', 'Flutter', 'React Native',
    'C', 'C++', 'Rust', 'SQL', 'GraphQL', 'Terraform',
];

export const StudyPreferenceSection = () => {
    // 폼 상태
    const [techStack, setTechStack] = useState<string[]>([]);
    const [techInput, setTechInput] = useState('');
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [availableDays, setAvailableDays] = useState<DayOfWeek[]>([]);
    const [preferredTimeSlot, setPreferredTimeSlot] = useState<TimeSlot | null>(null);
    const [preferredDurationWeeks, setPreferredDurationWeeks] = useState(4);

    // UI 상태
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [hasChanges, setHasChanges] = useState(false);

    // 초기 데이터 로드
    useEffect(() => {
        const loadPreference = async () => {
            try {
                const pref: any = await getStudyPreference();
                // 백엔드 필드명: techStacks, preferredTimeSlots (복수형)
                setTechStack(pref.techStacks || pref.techStack || []);
                setAvailableDays((pref.availableDays || []) as DayOfWeek[]);
                const timeSlots = pref.preferredTimeSlots || [];
                setPreferredTimeSlot(timeSlots[0] || pref.preferredTimeSlot || null);
                setPreferredDurationWeeks(pref.preferredDurationWeeks || 4);
            } catch {
                // API가 아직 없으면 기본값 사용 (localStorage fallback)
                const saved = localStorage.getItem('studyPreference');
                if (saved) {
                    try {
                        const parsed: StudyPreference = JSON.parse(saved);
                        setTechStack(parsed.techStack || []);
                        setAvailableDays((parsed.availableDays || []) as DayOfWeek[]);
                        setPreferredTimeSlot(parsed.preferredTimeSlot || null);
                        setPreferredDurationWeeks(parsed.preferredDurationWeeks || 4);
                    } catch { /* 무시 */ }
                }
            } finally {
                setIsLoading(false);
            }
        };
        loadPreference();
    }, []);

    // 변경 감지
    useEffect(() => {
        if (!isLoading) {
            setHasChanges(true);
        }
    }, [techStack, availableDays, preferredTimeSlot, preferredDurationWeeks]);

    // 기술스택 추가
    const addTech = useCallback((tech: string) => {
        const trimmed = tech.trim();
        if (trimmed && !techStack.includes(trimmed)) {
            setTechStack(prev => [...prev, trimmed]);
        }
        setTechInput('');
        setShowSuggestions(false);
    }, [techStack]);

    // 기술스택 삭제
    const removeTech = useCallback((tech: string) => {
        setTechStack(prev => prev.filter(t => t !== tech));
    }, []);

    // 기술스택 입력 키 핸들러
    const handleTechKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            if (techInput.trim()) {
                addTech(techInput);
            }
        }
    };

    // 요일 토글
    const toggleDay = useCallback((day: DayOfWeek) => {
        setAvailableDays(prev =>
            prev.includes(day) ? prev.filter(d => d !== day) : [...prev, day]
        );
    }, []);

    // 시간대 선택
    const selectTimeSlot = useCallback((slot: TimeSlot) => {
        setPreferredTimeSlot(prev => prev === slot ? null : slot);
    }, []);

    // 저장
    const handleSave = async () => {
        setIsSaving(true);
        setMessage(null);

        // 백엔드 필드명에 맞춤 (techStacks, preferredTimeSlots)
        const data = {
            techStacks: techStack,
            availableDays,
            preferredTimeSlots: preferredTimeSlot ? [preferredTimeSlot] : [],
            preferredDurationWeeks,
        };

        try {
            await updateStudyPreference(data);
            setMessage({ type: 'success', text: '스터디 선호 설정이 저장되었습니다.' });
            setHasChanges(false);
        } catch {
            // API 실패 시 localStorage에 저장 (fallback)
            localStorage.setItem('studyPreference', JSON.stringify(data));
            setMessage({ type: 'success', text: '스터디 선호 설정이 저장되었습니다.' });
            setHasChanges(false);
        } finally {
            setIsSaving(false);
        }
    };

    // 필터링된 추천 목록
    const filteredSuggestions = techInput.trim()
        ? SUGGESTED_TECHS.filter(
            t => t.toLowerCase().includes(techInput.toLowerCase()) && !techStack.includes(t)
        ).slice(0, 8)
        : [];

    if (isLoading) {
        return (
            <section className="setting-section">
                <div className="loading-spinner"><div className="spinner" /></div>
            </section>
        );
    }

    return (
        <section className="setting-section">
            {/* 섹션 헤더 */}
            <div className="section-header">
                <h2 className="section-title">
                    <BookOpen className="section-title-icon" />
                    스터디 선호 설정
                </h2>
                <p className="section-description">
                    AI 스터디 추천 및 계획 생성 시 활용됩니다. 정확하게 설정할수록 더 좋은 추천을 받을 수 있습니다.
                </p>
            </div>

            <div className="space-y-8">
                {/* ===== 1. 기술스택 ===== */}
                <div className="bg-white rounded-2xl border border-gray-100 p-6">
                    <div className="flex items-center gap-2 mb-4">
                        <h3 className="text-base font-bold text-gray-900">기술 스택</h3>
                        <span className="text-xs text-gray-400">
                            ({techStack.length}개 등록)
                        </span>
                    </div>

                    {/* 등록된 기술스택 태그 */}
                    {techStack.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-4">
                            {techStack.map(tech => (
                                <span
                                    key={tech}
                                    className="inline-flex items-center gap-1 px-3 py-1.5 bg-primary/10 text-primary text-sm font-medium rounded-full"
                                >
                                    {tech}
                                    <button
                                        type="button"
                                        onClick={() => removeTech(tech)}
                                        className="ml-0.5 hover:bg-primary/20 rounded-full p-0.5 transition-colors"
                                    >
                                        <X size={12} />
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}

                    {/* 기술스택 입력 */}
                    <div className="relative">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={techInput}
                                onChange={(e) => {
                                    setTechInput(e.target.value);
                                    setShowSuggestions(true);
                                }}
                                onKeyDown={handleTechKeyDown}
                                onFocus={() => setShowSuggestions(true)}
                                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                                placeholder="기술명 입력 후 Enter (예: React, Docker)"
                                className="flex-1 p-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none text-sm"
                            />
                            <button
                                type="button"
                                onClick={() => techInput.trim() && addTech(techInput)}
                                className="px-4 bg-gray-100 hover:bg-gray-200 rounded-xl transition-colors text-gray-600"
                            >
                                <Plus size={18} />
                            </button>
                        </div>

                        {/* 자동완성 드롭다운 */}
                        {showSuggestions && filteredSuggestions.length > 0 && (
                            <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden">
                                {filteredSuggestions.map(tech => (
                                    <button
                                        key={tech}
                                        type="button"
                                        onMouseDown={(e) => {
                                            e.preventDefault();
                                            addTech(tech);
                                        }}
                                        className="w-full px-4 py-2.5 text-left text-sm hover:bg-primary/5 transition-colors flex items-center gap-2"
                                    >
                                        <Plus size={14} className="text-gray-400" />
                                        {tech}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    <p className="mt-2 text-xs text-gray-400 flex items-center gap-1">
                        <Info size={12} />
                        콤마(,) 또는 Enter로 추가. 보유한 기술을 등록하면 맞춤 추천에 활용됩니다.
                    </p>
                </div>

                {/* ===== 2. 가용 일정 ===== */}
                <div className="bg-white rounded-2xl border border-gray-100 p-6">
                    <h3 className="text-base font-bold text-gray-900 mb-4">가용 일정</h3>

                    {/* 요일 선택 */}
                    <div className="mb-5">
                        <label className="text-sm font-medium text-gray-600 mb-3 block">참여 가능한 요일</label>
                        <div className="flex gap-2">
                            {DAYS.map(day => {
                                const isSelected = availableDays.includes(day);
                                const isWeekend = day === '토' || day === '일';
                                return (
                                    <button
                                        key={day}
                                        type="button"
                                        onClick={() => toggleDay(day)}
                                        className={cn(
                                            'w-11 h-11 rounded-xl text-sm font-bold transition-all',
                                            isSelected
                                                ? 'bg-primary text-white shadow-sm'
                                                : isWeekend
                                                    ? 'bg-red-50 text-red-400 hover:bg-red-100'
                                                    : 'bg-gray-50 text-gray-500 hover:bg-gray-100'
                                        )}
                                    >
                                        {day}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {/* 시간대 선택 */}
                    <div>
                        <label className="text-sm font-medium text-gray-600 mb-3 block">선호 시간대</label>
                        <div className="grid grid-cols-2 gap-3">
                            {TIME_SLOTS.map(slot => {
                                const isSelected = preferredTimeSlot === slot.value;
                                return (
                                    <button
                                        key={slot.value}
                                        type="button"
                                        onClick={() => selectTimeSlot(slot.value)}
                                        className={cn(
                                            'p-3 rounded-xl border text-left transition-all',
                                            isSelected
                                                ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                                                : 'border-gray-200 hover:border-gray-300 bg-white'
                                        )}
                                    >
                                        <div className="flex items-center justify-between">
                                            <span className={cn(
                                                'text-sm font-bold',
                                                isSelected ? 'text-primary' : 'text-gray-700'
                                            )}>
                                                {slot.label}
                                            </span>
                                            {isSelected && <Check size={16} className="text-primary" />}
                                        </div>
                                        <span className="text-xs text-gray-400">{slot.desc}</span>
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {/* ===== 3. 선호 스터디 기간 ===== */}
                <div className="bg-white rounded-2xl border border-gray-100 p-6">
                    <h3 className="text-base font-bold text-gray-900 mb-4">선호 스터디 기간</h3>

                    <div className="space-y-4">
                        {/* 기간 버튼 그리드 */}
                        <div className="flex gap-2 flex-wrap">
                            {DURATION_OPTIONS.map(weeks => {
                                const isSelected = preferredDurationWeeks === weeks;
                                return (
                                    <button
                                        key={weeks}
                                        type="button"
                                        onClick={() => setPreferredDurationWeeks(weeks)}
                                        className={cn(
                                            'px-4 py-2.5 rounded-xl text-sm font-bold transition-all',
                                            isSelected
                                                ? 'bg-primary text-white shadow-sm'
                                                : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                                        )}
                                    >
                                        {weeks}주
                                    </button>
                                );
                            })}
                        </div>

                        {/* 기간 설명 */}
                        <div className="flex items-center gap-2 text-xs text-gray-400">
                            <Info size={12} />
                            <span>
                                {preferredDurationWeeks <= 3 && '단기 집중형 스터디에 적합합니다.'}
                                {preferredDurationWeeks >= 4 && preferredDurationWeeks <= 6 && '가장 일반적인 스터디 기간입니다.'}
                                {preferredDurationWeeks >= 7 && '장기 심화 학습에 적합합니다.'}
                            </span>
                        </div>
                    </div>
                </div>

                {/* ===== 저장 버튼 ===== */}
                <div className="flex items-center justify-between">
                    {message && (
                        <span className={cn(
                            'text-sm font-medium',
                            message.type === 'success' ? 'text-green-600' : 'text-red-500'
                        )}>
                            {message.text}
                        </span>
                    )}
                    <div className="ml-auto">
                        <Button
                            variant="primary"
                            size="lg"
                            onClick={handleSave}
                            isLoading={isSaving}
                            disabled={!hasChanges}
                            leftIcon={!isSaving ? <Check size={18} /> : undefined}
                        >
                            저장하기
                        </Button>
                    </div>
                </div>
            </div>
        </section>
    );
};
