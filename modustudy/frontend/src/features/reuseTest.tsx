import { useState } from 'react';
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