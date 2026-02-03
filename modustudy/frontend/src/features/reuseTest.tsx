import React, { useState, useMemo } from 'react';
import { Button, Input, Modal, Card, BackButton, ArrowButton, DatePicker, TimePicker, FloatingInput, IconInput } from '@/shared/components';
import { X, Heart, Bookmark, Users, Star, Clock, Award, Check, Calendar, ChevronRight, Mail, Lock, Search, User } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

const ReuseTest = () => {
    const [isOpened, setIsOpened] = useState(false);
    const [selectedDate, setSelectedDate] = useState('');
    const [selectedTime, setSelectedTime] = useState('');

    return (
        <div className="p-10 bg-[#f8f9fa] min-h-screen space-y-12">
            <h1 className="text-3xl font-extrabold text-[#1a202c]">Component Playground</h1>

            {/* 1. 다양한 버튼 모음 */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-study-blue rounded-full" />
                    Buttons & Navigation
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {/* 백버튼 3종 */}
                    <article className="space-y-4 p-6 bg-white rounded-3xl shadow-sm">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Back Buttons (3 Types)</h3>
                        <div className="flex items-center gap-4">
                            {/* Type 1: 아이콘만 */}
                            <BackButton variant="icon-only" onClick={() => console.log('백버튼 클릭')} />
                            {/* Type 2: 텍스트 포함 */}
                            <BackButton variant="with-text" onClick={() => console.log('목록으로 클릭')} />
                            {/* Type 3: 텍스트 포함2 */}
                            <BackButton variant="with-text-2" onClick={() => console.log('뒤로 가기')} />
                        </div>
                    </article>

                    {/* 화살표 컴트롤 */}
                    <article className="space-y-4 p-6 bg-white rounded-3xl shadow-sm">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Arrow Controls</h3>
                        <div className="flex items-center gap-4">
                            <ArrowButton direction="left" onClick={() => console.log('이전')} size="sm" />
                            <ArrowButton direction="right" onClick={() => console.log('다음')} size="sm" />
                        </div>
                    </article>

                    {/* 메인 액션 버튼 */}
                    <article className="space-y-4 p-6 bg-white rounded-3xl shadow-sm">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Brand Actions</h3>
                        <div className="flex items-center gap-4">
                            <Button variant="primary">참여하기</Button>
                            <Button variant="outline">신청취소</Button>
                        </div>
                    </article>
                </div>
            </section>

            {/* 2. 카드 시스템 */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-green-500 rounded-full" />
                    Card Components
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {/* Elevated Card (스터디 카드 스타일) */}
                    <Card variant="elevated" className={cn("p-6 space-y-4")}>
                        <div className={cn("h-40 bg-blue-50 rounded-2xl flex items-center justify-center")}>
                            <Star className="text-blue-200" size={48} />
                        </div>
                        <div className="space-y-2">
                            <h3 className="font-bold text-lg">알고리즘 정복 스터디</h3>
                            <p className="text-sm text-gray-500 line-clamp-2">백준 골드 이상 목표로 매주 5문제씩 풀이하고 리뷰하는 스터디입니다.</p>
                        </div>
                        <div className="flex justify-between items-center pt-2">
                            <span className="text-xs font-bold text-blue-600 bg-blue-50 px-3 py-1 rounded-full">모집중</span>
                            <div className="flex items-center gap-1 text-gray-400">
                                <Users size={14} />
                                <span className="text-xs">4/6명</span>
                            </div>
                        </div>
                    </Card>

                    {/* Outline Card (상세 정보 스타일) */}
                    <Card variant="outline" className="p-6 space-y-4">
                        <div className="flex justify-between items-start">
                            <h3 className="font-bold">스터디 정보</h3>
                            <Button variant="google-ghost" isCircle size="sm"><X size={16} /></Button>
                        </div>
                        <div className="space-y-3">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="flex items-center gap-3 text-sm text-gray-600">
                                    <div className="w-1.5 h-1.5 bg-gray-300 rounded-full" />
                                    <span>스터디 정보 상세 항목 {i}</span>
                                </div>
                            ))}
                        </div>
                    </Card>

                    {/* Flat Card (정보 요약 스타일) */}
                    <Card variant="flat" className="p-6">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-sm">
                                <Heart className="text-red-400" size={24} fill="currentColor" />
                            </div>
                            <div>
                                <h3 className="font-bold">내 관심 스터디</h3>
                                <p className="text-sm text-gray-500">총 12개의 스터디</p>
                            </div>
                        </div>
                    </Card>
                </div>
            </section>

            {/* 3. Study 도메인 컴포넌트 조합 */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-orange-500 rounded-full" />
                    Study Feature Patterns
                </h2>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* 실전 스터디 카드 조합 예시 */}
                    <article className="space-y-4">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Study Card Composition</h3>
                        <Card variant="elevated" className="group">
                            {/* 카드 상단: 이미지 또는 테마 영역 */}
                            <div className="h-48 bg-gradient-to-br from-indigo-500 to-purple-600 relative p-6 flex flex-col justify-between">
                                <div className="flex justify-between items-start">
                                    <div className="flex gap-2">
                                        <span className="bg-white/20 backdrop-blur-md text-white text-[10px] font-bold px-2.5 py-1 rounded-full border border-white/30">온라인</span>
                                        <span className="bg-yellow-400 text-yellow-900 text-[10px] font-bold px-2.5 py-1 rounded-full shadow-sm">번개</span>
                                    </div>
                                    <Button variant="google-ghost" isCircle size="sm" className="text-white hover:bg-white/20">
                                        <Bookmark size={20} />
                                    </Button>
                                </div>
                                <div className="text-white">
                                    <p className="text-xs font-medium opacity-80 mb-1">C언어 • 입문</p>
                                    <h3 className="text-xl font-bold leading-tight">포인터 마스터를 위한<br />기초 C언어 스터디</h3>
                                </div>
                            </div>

                            {/* 카드 하단: 상세 정보 영역 */}
                            <div className="p-6 space-y-6">
                                <div className="flex items-center gap-6 text-sm text-gray-600 font-medium">
                                    <div className="flex items-center gap-2">
                                        <Users size={16} className="text-indigo-500" />
                                        <span>4 / 8명</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Star size={16} className="text-yellow-500" fill="currentColor" />
                                        <span>4.8 (12)</span>
                                    </div>
                                </div>

                                <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 rounded-full bg-gray-200 border-2 border-white shadow-sm overflow-hidden text-xs flex items-center justify-center font-bold text-gray-500">
                                            김
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-400">스터디장</p>
                                            <p className="text-sm font-bold text-gray-700">김공부</p>
                                        </div>
                                    </div>
                                    <Button variant="primary" size="sm">상세보기</Button>
                                </div>
                            </div>
                        </Card>
                    </article>

                    {/* 스터디 관리/상세 페이지 패턴 */}
                    <article className="space-y-4">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Management & Detail UI</h3>
                        <div className="space-y-4">
                            {/* 관리 페이지 사이드바 아이템 시뮬레이션 */}
                            <Card variant="flat" className={cn(
                                "p-4 flex items-center justify-between transition-all cursor-pointer border border-transparent",
                                "hover:bg-white hover:shadow-md hover:border-blue-100"
                            )}>
                                <div className="flex items-center gap-4">
                                    <div className="w-10 h-10 rounded-2xl bg-blue-100 flex items-center justify-center text-blue-600">
                                        <Users size={20} />
                                    </div>
                                    <div>
                                        <p className="font-bold text-gray-800">지원자 관리</p>
                                        <p className="text-xs text-gray-500">새로운 지원자 3명</p>
                                    </div>
                                </div>
                                <ChevronRight size={20} className="text-gray-300" />
                            </Card>

                            {/* 상세 페이지 정보 카드 시뮬레이션 */}
                            <Card variant="outline" className="p-6 bg-white border-l-8 border-l-blue-500">
                                <div className="flex justify-between items-start mb-4">
                                    <h3 className="text-lg font-bold flex items-center gap-2">
                                        <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                                        진행 정보
                                    </h3>
                                    <Button variant="google-ghost" size="sm" className="font-bold text-blue-600">수정</Button>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-gray-50 p-3 rounded-2xl">
                                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">정기 모임</p>
                                        <p className="text-sm font-bold">매주 토요일 14:00</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded-2xl">
                                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">모집 마감</p>
                                        <p className="text-sm font-bold">2024. 02. 15</p>
                                    </div>
                                </div>
                            </Card>
                        </div>
                    </article>
                </div>
            </section>

            {/* 4. Dashboard 디자인 패턴 */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-cyan-500 rounded-full" />
                    Dashboard Widgets
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
                    {/* 통계 카드 1: 누적 학습 시간 */}
                    <Card variant="default" className={cn(
                        "p-5 flex items-center gap-4 transition-colors",
                        "hover:border-blue-200"
                    )}>
                        <div className="w-12 h-12 bg-blue-50 rounded-2xl flex items-center justify-center text-blue-500 shadow-inner">
                            <Clock size={24} />
                        </div>
                        <div>
                            <p className="text-xs font-bold text-gray-400 uppercase tracking-tighter">Total Study</p>
                            <p className="text-xl font-black text-gray-800">128<span className="text-sm font-medium ml-1 text-gray-400">h</span></p>
                        </div>
                    </Card>

                    {/* 통계 카드 2: 퀴즈 점수 */}
                    <Card variant="default" className={cn(
                        "p-5 flex items-center gap-4 transition-colors",
                        "hover:border-orange-200"
                    )}>
                        <div className={cn(
                            "w-12 h-12 rounded-2xl flex items-center justify-center shadow-inner",
                            "bg-orange-50 text-orange-500"
                        )}>
                            <Award size={24} />
                        </div>
                        <div>
                            <p className={cn("text-xs font-bold uppercase tracking-tighter", "text-gray-400")}>Quiz Score</p>
                            <p className={cn("text-xl font-black", "text-gray-800")}>920<span className="text-sm font-medium ml-1 text-gray-400">pts</span></p>
                        </div>
                    </Card>

                    {/* 통계 카드 3: 출석률 */}
                    <Card variant="default" className={cn(
                        "p-5 flex items-center gap-4 transition-colors",
                        "hover:border-green-200"
                    )}>
                        <div className="w-12 h-12 bg-green-50 rounded-2xl flex items-center justify-center text-green-500 shadow-inner">
                            <Calendar size={24} />
                        </div>
                        <div>
                            <p className="text-xs font-bold text-gray-400 uppercase tracking-tighter">Attendance</p>
                            <p className="text-xl font-black text-gray-800">98<span className="text-sm font-medium ml-1 text-gray-400">%</span></p>
                        </div>
                    </Card>

                    {/* 활동 잔디 (Activity Grass) 시뮬레이션 */}
                    <Card variant="default" className="p-5 col-span-full xl:col-span-1 border-dashed">
                        <div className="flex justify-between items-center mb-3">
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Activity Grass</p>
                            <div className="flex gap-1">
                                {[1, 2, 3, 4].map(level => (
                                    <div
                                        key={level}
                                        className={cn(
                                            'w-2 h-2 rounded-sm',
                                            {
                                                'bg-gray-100': level === 1,
                                                'bg-blue-200': level === 2,
                                                'bg-blue-400': level === 3,
                                                'bg-blue-600': level === 4,
                                            }
                                        )}
                                    />
                                ))}
                            </div>
                        </div>
                        <div className="grid grid-cols-7 gap-1.5">
                            {Array.from({ length: 28 }).map((_, i) => {
                                const random = Math.random();
                                return (
                                    <div
                                        key={i}
                                        className={cn(
                                            'aspect-square rounded-[3px] transition-all hover:scale-110 cursor-pointer',
                                            {
                                                'bg-blue-600': random > 0.7,
                                                'bg-blue-400': random <= 0.7 && random > 0.5,
                                                'bg-blue-200': random <= 0.5 && random > 0.3,
                                                'bg-gray-100': random <= 0.3,
                                            }
                                        )}
                                    />
                                );
                            })}
                        </div>
                    </Card>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* 다크 모드 위젯 패턴 (학습 목표) */}
                    <article className="space-y-4">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Dark Widget Pattern (Goals)</h3>
                        <div className="bg-[#1e1e2d] rounded-[32px] p-8 shadow-2xl space-y-6">
                            <div className="flex justify-between items-center text-white">
                                <h3 className="text-lg font-bold">오늘의 학습 목표</h3>
                                <span className="text-xs px-2.5 py-1 bg-white/10 rounded-full text-white/50 border border-white/5">01.18 Sun</span>
                            </div>

                            <div className="space-y-3">
                                {[
                                    { text: '알고리즘 코드 리뷰 2건', done: true },
                                    { text: '네트워크 OSI 7계층 정리', done: false },
                                    { text: '스터디 과제 제출하기', done: false }
                                ].map((goal, i) => (
                                    <div
                                        key={i}
                                        className={cn(
                                            'flex items-center gap-4 p-4 rounded-2xl transition-all cursor-pointer',
                                            {
                                                'bg-white/5 opacity-50': goal.done,
                                                'bg-white/10 hover:bg-white/15': !goal.done,
                                            }
                                        )}
                                    >
                                        <div className={cn(
                                            "w-6 h-6 rounded-lg border-2 flex items-center justify-center transition-all",
                                            {
                                                "bg-blue-500 border-blue-500": goal.done,
                                                "border-white/20": !goal.done,
                                            }
                                        )}>
                                            {goal.done && <Check size={14} className="text-white" />}
                                        </div>
                                        <span className={cn(
                                            "text-sm font-medium",
                                            {
                                                "text-white/50 line-through": goal.done,
                                                "text-white": !goal.done,
                                            }
                                        )}>
                                            {goal.text}
                                        </span>
                                    </div>
                                ))}
                            </div>

                            <div className="space-y-2">
                                <div className="flex justify-between text-[11px] font-bold text-white/40 mb-1">
                                    <span>PROGRESS</span>
                                    <span>33%</span>
                                </div>
                                <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                                    <div className="h-full bg-blue-500 rounded-full" style={{ width: '33%' }} />
                                </div>
                            </div>
                        </div>
                    </article>

                    {/* 컴팩트 캘린더 패턴 (Dots) */}
                    <article className="space-y-4">
                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Compact Calendar Cells</h3>
                        <Card variant="outline" className="p-6 bg-white grid grid-cols-4 gap-4">
                            {[17, 18, 19, 20].map((day) => (
                                <div
                                    key={day}
                                    className={cn(
                                        'aspect-square flex flex-col items-center justify-center gap-2 rounded-2xl border transition-all cursor-pointer',
                                        {
                                            'bg-blue-50 border-blue-200': day === 18,
                                            'border-gray-50 hover:border-gray-200': day !== 18,
                                        }
                                    )}
                                >
                                    <span
                                        className={cn(
                                            'text-sm font-bold',
                                            {
                                                'text-blue-600': day === 18,
                                                'text-gray-400': day !== 18,
                                            }
                                        )}
                                    >
                                        {day}
                                    </span>
                                    <div className="flex gap-1">
                                        <div
                                            className={cn(
                                                'w-1.5 h-1.5 rounded-full',
                                                {
                                                    'bg-orange-400': day === 17,
                                                    'bg-blue-400': day !== 17,
                                                }
                                            )}
                                        />
                                        {day === 19 && <div className="w-1.5 h-1.5 rounded-full bg-green-400" />}
                                        <div className="w-1.5 h-1.5 rounded-full bg-indigo-400" />
                                    </div>
                                </div>
                            ))}
                        </Card>
                    </article>
                </div>
            </section>

            {/* 5. Floating Input */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-violet-500 rounded-full" />
                    Floating Label Input
                </h2>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* 사이즈 비교 */}
                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Size Variants</h3>
                        <div className="space-y-4">
                            <FloatingInput label="Extra Small" size="xs" />
                            <FloatingInput label="Small" size="sm" />
                            <FloatingInput label="Medium (Default)" size="md" />
                            <FloatingInput label="Large" size="lg" />
                            <FloatingInput label="Extra Large" size="xl" />
                        </div>
                    </Card>

                    {/* 상태별 */}
                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Status Variants</h3>
                        <div className="space-y-4">
                            <FloatingInput label="기본 상태" helperText="도움말 텍스트입니다" />
                            <FloatingInput label="에러 상태" error="올바른 이메일 형식을 입력하세요" />
                            <FloatingInput label="성공 상태" success="사용 가능한 닉네임입니다" defaultValue="모두스터디" />
                            <FloatingInput label="필수 입력" required />
                            <FloatingInput label="비활성화" disabled defaultValue="수정 불가" />
                        </div>
                    </Card>
                </div>
            </section>

            {/* 6. Icon Input */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-indigo-500 rounded-full" />
                    Icon Input
                </h2>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* 로그인 폼 예시 */}
                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Login Form Example</h3>
                        <div className="space-y-3">
                            <IconInput
                                type="email"
                                placeholder="이메일을 입력하세요"
                                leftIcon={<Mail />}
                            />
                            <IconInput
                                type="password"
                                placeholder="비밀번호를 입력하세요"
                                leftIcon={<Lock />}
                            />
                        </div>
                    </Card>

                    {/* 사이즈 + 상태 */}
                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Sizes & States</h3>
                        <div className="space-y-3">
                            <IconInput placeholder="Small" leftIcon={<Search />} size="sm" />
                            <IconInput placeholder="Medium (Default)" leftIcon={<User />} size="md" />
                            <IconInput placeholder="Large" leftIcon={<Mail />} size="lg" />
                            <IconInput placeholder="에러 상태" leftIcon={<Mail />} error="올바른 이메일을 입력하세요" />
                            <IconInput placeholder="성공 상태" leftIcon={<User />} success="확인 완료" defaultValue="김공부" />
                            <IconInput placeholder="비활성화" leftIcon={<Lock />} disabled />
                        </div>
                    </Card>
                </div>
            </section>

            {/* 7. Pickers & Inputs */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-study-blue rounded-full" />
                    Pickers & Custom Inputs
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Date Picker (Single)</h3>
                        <div className="space-y-4">
                            <DatePicker
                                value={selectedDate}
                                onChange={setSelectedDate}
                                placeholder="학습 시작일을 선택하세요"
                            />
                            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
                                <p className="text-xs font-bold text-gray-400 mb-1 uppercase">Selected Date</p>
                                <p className="text-sm font-black text-study-blue">{selectedDate || '선택된 날짜 없음'}</p>
                            </div>
                        </div>
                    </Card>

                    <Card variant="outline" className="p-8 space-y-6">
                        <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest">Time Picker (5-min Step)</h3>
                        <div className="space-y-4">
                            <TimePicker
                                value={selectedTime}
                                onChange={setSelectedTime}
                                placeholder="모임 시간을 선택하세요"
                            />
                            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
                                <p className="text-xs font-bold text-gray-400 mb-1 uppercase">Selected Time</p>
                                <p className="text-sm font-black text-study-blue">{selectedTime || '선택된 시간 없음'}</p>
                            </div>
                        </div>
                    </Card>
                </div>
            </section>

            {/* 8. 커리큘럼 로드맵 (S자 도로 스타일) */}
            <section className="space-y-6">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-emerald-500 rounded-full" />
                    Curriculum Roadmap (S-Road Style)
                </h2>
                <CurriculumRoadmapDemo />
            </section>

            {/* 6. 모달 테스트 */}
            <section className="space-y-6 pb-20">
                <h2 className="text-xl font-bold border-b pb-2 flex items-center gap-2">
                    <div className="w-1.5 h-6 bg-purple-500 rounded-full" />
                    Interactions
                </h2>
                <div className="flex gap-4">
                    <Button variant="google-primary" size="lg" onClick={() => setIsOpened(true)}>고급 모달 열기</Button>
                </div>

                <Modal
                    isOpen={isOpened}
                    onClose={() => setIsOpened(false)}
                    title="스터디 가입 신청"
                    maxWidth="md"
                >
                    <div className="space-y-6">
                        <div className="p-4 bg-gray-50 rounded-2xl space-y-2">
                            <p className="text-sm font-bold text-gray-700">신청 한마디</p>
                            <Input placeholder="스터디장에 대한 각오를 적어주세요" />
                        </div>
                        <p className="text-sm text-gray-500 text-center leading-relaxed">
                            스터디 가입 신청이 승인되면<br />
                            스터디장으로부터 알림이 전송됩니다. 계속하시겠습니까?
                        </p>
                        <div className="flex gap-3">
                            <Button variant="secondary" fullWidth onClick={() => setIsOpened(false)}>취소</Button>
                            <Button variant="primary" fullWidth onClick={() => setIsOpened(false)}>신청 완료</Button>
                        </div>
                    </div>
                </Modal>
            </section>
        </div>
    );
};

export default ReuseTest;

// ============================================
// 커리큘럼 로드맵 컴포넌트 (S자 도로 스타일)
// ============================================

interface CurriculumStop {
    session: number;
    title: string;
    date: string;
    description?: string;
    isCompleted?: boolean;
}

interface CurriculumRoadmapProps {
    curriculum: CurriculumStop[];
    currentSession?: number; // 현재 진행 중인 회차
    onStopClick?: (session: number) => void;
}

export const CurriculumRoadmap: React.FC<CurriculumRoadmapProps> = ({
    curriculum,
    currentSession = 1,
    onStopClick
}) => {
    const totalStops = curriculum.length;

    // SVG 크기 설정
    const svgWidth = 800;
    const svgHeight = Math.max(400, totalStops * 120);
    const roadWidth = 50;

    // S자 도로 경로 생성
    const generateRoadPath = () => {
        const segments: string[] = [];
        const startX = 150;
        const endX = svgWidth - 150;
        const segmentHeight = 120;

        segments.push(`M ${startX} 60`);

        for (let i = 0; i < totalStops; i++) {
            const y = 60 + i * segmentHeight;
            const nextY = y + segmentHeight;
            const isLeftToRight = i % 2 === 0;

            if (i < totalStops - 1) {
                if (isLeftToRight) {
                    // 왼쪽에서 오른쪽으로, 그 다음 아래로 커브
                    segments.push(`L ${endX - 60} ${y}`);
                    segments.push(`Q ${endX} ${y} ${endX} ${y + 60}`);
                    segments.push(`L ${endX} ${nextY - 60}`);
                    segments.push(`Q ${endX} ${nextY} ${endX - 60} ${nextY}`);
                } else {
                    // 오른쪽에서 왼쪽으로, 그 다음 아래로 커브
                    segments.push(`L ${startX + 60} ${y}`);
                    segments.push(`Q ${startX} ${y} ${startX} ${y + 60}`);
                    segments.push(`L ${startX} ${nextY - 60}`);
                    segments.push(`Q ${startX} ${nextY} ${startX + 60} ${nextY}`);
                }
            } else {
                // 마지막 세그먼트
                if (isLeftToRight) {
                    segments.push(`L ${endX} ${y}`);
                } else {
                    segments.push(`L ${startX} ${y}`);
                }
            }
        }

        return segments.join(' ');
    };

    // 각 정류장의 위치 계산
    const getStopPosition = (index: number) => {
        const segmentHeight = 120;
        const startX = 150;
        const endX = svgWidth - 150;
        const y = 60 + index * segmentHeight;
        const isLeftToRight = index % 2 === 0;

        // 각 세그먼트의 시작 위치
        const x = isLeftToRight ? startX + 50 : endX - 50;

        return { x, y };
    };

    const roadPath = generateRoadPath();

    // 나무 배경 위치 고정 (useMemo로 렌더링마다 변경되지 않도록)
    const treePositions = useMemo(() => {
        return [...Array(8)].map((_, i) => ({
            x: 50 + (i * 97) % 700,
            y: 30 + (i * 73) % (Math.max(400, totalStops * 120) - 60),
            isGreen: i % 2 === 0
        }));
    }, [totalStops]);

    // 자동차 진행률 계산 (정류장 위치에 맞게 조정)
    const carProgress = useMemo(() => {
        const sessionIndex = currentSession - 1;
        if (totalStops <= 1) return 3;

        // 각 정류장별로 도로 경로상의 진행률 계산
        // 경로: 시작 → 가로이동 → 커브 → 세로이동 → 커브 → 가로이동 반복
        // 정류장은 각 가로 구간의 시작 부분에 위치

        // 한 세그먼트(가로+커브+세로+커브)가 차지하는 비율
        const segmentPercent = 100 / totalStops;

        // 각 정류장은 해당 세그먼트의 시작 ~ 초반부에 위치 (약 15% 지점)
        const stopOffsetInSegment = 0.15;
        const progress = sessionIndex * segmentPercent + segmentPercent * stopOffsetInSegment;

        return Math.max(2, Math.min(98, progress));
    }, [currentSession, totalStops]);

    // 현재 세션이 역방향(오른쪽→왼쪽)인지 확인
    const isReversed = (currentSession - 1) % 2 !== 0;

    return (
        <div className="relative w-full overflow-x-auto">
            <svg
                width={svgWidth}
                height={svgHeight}
                viewBox={`0 0 ${svgWidth} ${svgHeight}`}
                className="mx-auto"
            >
                {/* 배경 장식 - 나무들 (위치 고정) */}
                <g className="trees">
                    {treePositions.map((tree, i) => (
                        <g key={`tree-${i}`} transform={`translate(${tree.x}, ${tree.y})`}>
                            <polygon
                                points="0,-15 12,10 -12,10"
                                fill={tree.isGreen ? '#22c55e' : '#84cc16'}
                                opacity={0.6}
                            />
                            <rect x="-2" y="10" width="4" height="8" fill="#92400e" opacity={0.6} />
                        </g>
                    ))}
                </g>

                {/* 도로 그림자 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#1f2937"
                    strokeWidth={roadWidth + 10}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    opacity={0.1}
                    transform="translate(4, 4)"
                />

                {/* 도로 본체 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#374151"
                    strokeWidth={roadWidth}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />

                {/* 도로 중앙선 (점선) */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#fbbf24"
                    strokeWidth={3}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeDasharray="20, 15"
                />

                {/* 도로 가장자리 선 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#f3f4f6"
                    strokeWidth={roadWidth}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    opacity={0.3}
                    strokeDasharray="5, 5"
                />

                {/* 정류장들 */}
                {curriculum.map((stop, index) => {
                    const pos = getStopPosition(index);
                    const isCompleted = stop.isCompleted || index < currentSession - 1;
                    const isCurrent = index === currentSession - 1;
                    const isLeftSide = index % 2 !== 0;

                    return (
                        <g
                            key={stop.session}
                            className="cursor-pointer"
                            onClick={() => onStopClick?.(stop.session)}
                        >
                            {/* 정류장 연결선 */}
                            <line
                                x1={pos.x}
                                y1={pos.y}
                                x2={isLeftSide ? pos.x + 80 : pos.x - 80}
                                y2={pos.y - 30}
                                stroke={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#d1d5db'}
                                strokeWidth={2}
                                strokeDasharray={isCompleted ? 'none' : '4, 4'}
                            />

                            {/* 정류장 핀 */}
                            <g transform={`translate(${pos.x}, ${pos.y - 15})`}>
                                {/* 핀 그림자 */}
                                <ellipse cx={0} cy={18} rx={8} ry={4} fill="#000" opacity={0.2} />

                                {/* 핀 본체 */}
                                <path
                                    d="M0,-20 C-12,-20 -15,-8 -15,0 C-15,12 0,25 0,25 C0,25 15,12 15,0 C15,-8 12,-20 0,-20"
                                    fill={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#9ca3af'}
                                    className="drop-shadow-lg"
                                />

                                {/* 핀 내부 원 */}
                                <circle
                                    cx={0}
                                    cy={-5}
                                    r={8}
                                    fill="white"
                                />

                                {/* 회차 번호 */}
                                <text
                                    x={0}
                                    y={-1}
                                    textAnchor="middle"
                                    fontSize={10}
                                    fontWeight="bold"
                                    fill={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#6b7280'}
                                >
                                    {stop.session}
                                </text>
                            </g>

                            {/* 정류장 정보 카드 */}
                            <g transform={`translate(${isLeftSide ? pos.x + 90 : pos.x - 200}, ${pos.y - 55})`}>
                                {/* 카드 배경 */}
                                <rect
                                    x={0}
                                    y={0}
                                    width={110}
                                    height={50}
                                    rx={8}
                                    fill="white"
                                    stroke={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#e5e7eb'}
                                    strokeWidth={2}
                                    className="drop-shadow-sm"
                                />

                                {/* 제목 */}
                                <text
                                    x={10}
                                    y={20}
                                    fontSize={11}
                                    fontWeight="bold"
                                    fill="#1f2937"
                                >
                                    {stop.title.length > 10 ? stop.title.slice(0, 10) + '...' : stop.title}
                                </text>

                                {/* 날짜 */}
                                <text
                                    x={10}
                                    y={38}
                                    fontSize={9}
                                    fill="#6b7280"
                                >
                                    {stop.date}
                                </text>

                                {/* 완료 체크 */}
                                {isCompleted && (
                                    <g transform="translate(90, 10)">
                                        <circle cx={0} cy={0} r={8} fill="#22c55e" />
                                        <path
                                            d="M-4,0 L-1,3 L4,-3"
                                            stroke="white"
                                            strokeWidth={2}
                                            fill="none"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                        />
                                    </g>
                                )}
                            </g>
                        </g>
                    );
                })}

                {/* 자동차 - 도로를 따라 이동 */}
                <g
                    className="car-on-road"
                    style={{
                        offsetPath: `path("${roadPath}")`,
                        offsetDistance: `${carProgress}%`,
                        offsetRotate: '0deg',
                        transition: 'offset-distance 0.6s ease-in-out',
                    } as React.CSSProperties}
                >
                    {/* 자동차 본체 - 역방향일 때 좌우 반전 */}
                    <g transform={`scale(${isReversed ? -0.8 : 0.8}, 0.8) translate(${isReversed ? 20 : -20}, -12)`}>
                        {/* 차체 그림자 */}
                        <ellipse cx={25} cy={28} rx={22} ry={6} fill="#000" opacity={0.2} />

                        {/* 차체 */}
                        <rect x={5} y={8} width={40} height={18} rx={4} fill="#ef4444" />
                        <rect x={12} y={2} width={26} height={12} rx={3} fill="#ef4444" />

                        {/* 창문 */}
                        <rect x={14} y={4} width={10} height={8} rx={2} fill="#bfdbfe" />
                        <rect x={26} y={4} width={10} height={8} rx={2} fill="#bfdbfe" />

                        {/* 헤드라이트 */}
                        <rect x={42} y={12} width={4} height={4} rx={1} fill="#fef08a" />
                        <rect x={42} y={18} width={4} height={4} rx={1} fill="#fef08a" />

                        {/* 테일라이트 */}
                        <rect x={4} y={12} width={3} height={10} rx={1} fill="#fca5a5" />

                        {/* 바퀴 */}
                        <circle cx={15} cy={26} r={5} fill="#1f2937" />
                        <circle cx={15} cy={26} r={2} fill="#6b7280" />
                        <circle cx={35} cy={26} r={5} fill="#1f2937" />
                        <circle cx={35} cy={26} r={2} fill="#6b7280" />
                    </g>
                </g>
            </svg>

            {/* CSS for offset-path */}
            <style>{`
                .car-on-road {
                    position: absolute;
                }
            `}</style>
        </div>
    );
};

// 데모용 래퍼 컴포넌트
export const CurriculumRoadmapDemo: React.FC = () => {
    const [currentSession, setCurrentSession] = useState(1);

    // 샘플 커리큘럼 데이터
    const sampleCurriculum: CurriculumStop[] = [
        { session: 1, title: 'OT & 환경설정', date: '2026-02-15', description: '개발 환경 구축', isCompleted: true },
        { session: 2, title: 'React 기초', date: '2026-02-22', description: 'JSX와 컴포넌트' },
        { session: 3, title: 'State & Props', date: '2026-03-01', description: '상태 관리 기초' },
        { session: 4, title: 'Hooks 심화', date: '2026-03-08', description: 'useEffect, useRef' },
        { session: 5, title: '프로젝트 발표', date: '2026-03-15', description: '최종 결과물 공유' },
    ];

    return (
        <div className="p-8 bg-gradient-to-b from-sky-50 to-green-50 rounded-3xl">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h3 className="text-xl font-bold text-gray-800">📍 커리큘럼 로드맵</h3>
                    <p className="text-sm text-gray-500 mt-1">현재 진행 회차: {currentSession} / {sampleCurriculum.length}</p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCurrentSession(prev => Math.max(1, prev - 1))}
                        disabled={currentSession <= 1}
                    >
                        ← 이전
                    </Button>
                    <Button
                        variant="primary"
                        size="sm"
                        onClick={() => setCurrentSession(prev => Math.min(sampleCurriculum.length, prev + 1))}
                        disabled={currentSession >= sampleCurriculum.length}
                    >
                        다음 →
                    </Button>
                </div>
            </div>

            <CurriculumRoadmap
                curriculum={sampleCurriculum}
                currentSession={currentSession}
                onStopClick={(session) => {
                    console.log(`회차 ${session} 클릭됨`);
                    setCurrentSession(session);
                }}
            />

            {/* 범례 */}
            <div className="mt-6 flex items-center justify-center gap-6 text-sm">
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-green-500" />
                    <span className="text-gray-600">완료</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-blue-500" />
                    <span className="text-gray-600">진행 중</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-gray-400" />
                    <span className="text-gray-600">예정</span>
                </div>
            </div>
        </div>
    );
};