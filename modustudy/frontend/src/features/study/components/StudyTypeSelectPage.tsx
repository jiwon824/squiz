import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Calendar, Zap, Users, Clock, Target } from 'lucide-react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { cn } from '@/shared/utils/cn';

const StudyTypeSelectPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <UserLayoutV2>
            <div className="max-w-3xl mx-auto px-4 py-8">
                {/* 헤더 */}
                <div className="flex items-center gap-3 mb-8">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 hover:bg-gray-100 rounded-xl transition-colors"
                    >
                        <ChevronLeft size={24} />
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-800">스터디 개설</h1>
                        <p className="text-gray-500 text-sm mt-1">어떤 유형의 스터디를 개설하시겠어요?</p>
                    </div>
                </div>

                {/* 타입 선택 카드 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* 계획형 스터디 */}
                    <button
                        onClick={() => navigate('/study/create/planned')}
                        className={cn(
                            "p-8 rounded-2xl border-2 text-left transition-all hover:shadow-lg hover:border-primary group flex flex-col h-full",
                            "bg-white border-gray-200"
                        )}
                    >
                        <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-6 group-hover:bg-primary/20 transition-colors">
                            <Calendar size={32} className="text-primary" />
                        </div>
                        <h2 className="text-2xl font-bold text-gray-800 mb-3">계획형 스터디</h2>
                        <p className="text-gray-600 leading-relaxed mb-6 flex-1">
                            정기적으로 진행하는 장기 스터디입니다.<br />
                            커리큘럼을 세우고 체계적으로 학습할 수 있어요.
                        </p>
                        <div className="space-y-3 text-gray-500 mb-8">
                            <div className="flex items-center gap-3">
                                <Users size={18} />
                                <span className="font-medium">멤버 모집 및 관리</span>
                            </div>
                            <div className="flex items-center gap-3">
                                <Clock size={18} />
                                <span className="font-medium">정기 모임 일정 설정</span>
                            </div>
                            <div className="flex items-center gap-3">
                                <Target size={18} />
                                <span className="font-medium">커리큘럼 및 출석 관리</span>
                            </div>
                        </div>
                        <div className="pt-6 border-t border-gray-100 w-full mt-auto">
                            <span className="text-primary font-bold text-lg flex items-center gap-2">
                                계획형 스터디 만들기 <span className="transition-transform group-hover:translate-x-1">→</span>
                            </span>
                        </div>
                    </button>

                    {/* 번개형 스터디 */}
                    <button
                        onClick={() => navigate('/study/create/lightning')}
                        className={cn(
                            "p-8 rounded-2xl border-2 text-left transition-all hover:shadow-lg hover:border-amber-400 group flex flex-col h-full",
                            "bg-white border-gray-200"
                        )}
                    >
                        <div className="w-16 h-16 rounded-2xl bg-amber-100 flex items-center justify-center mb-6 group-hover:bg-amber-200 transition-colors">
                            <Zap size={32} className="text-amber-500" />
                        </div>
                        <h2 className="text-2xl font-bold text-gray-800 mb-3">번개 스터디</h2>
                        <p className="text-gray-600 leading-relaxed mb-6 flex-1">
                            1회성으로 빠르게 진행하는 스터디입니다.<br />
                            간단한 모임이나 스터디에 적합해요.
                        </p>
                        <div className="space-y-3 text-gray-500 mb-8">
                            <div className="flex items-center gap-3">
                                <Zap size={18} />
                                <span className="font-medium">1회성 빠른 스터디</span>
                            </div>
                            <div className="flex items-center gap-3">
                                <Clock size={18} />
                                <span className="font-medium">간단한 일정 설정</span>
                            </div>
                            <div className="flex items-center gap-3">
                                <Users size={18} />
                                <span className="font-medium">즉석 참여 가능</span>
                            </div>
                        </div>
                        <div className="pt-6 border-t border-gray-100 w-full mt-auto">
                            <span className="text-amber-500 font-bold text-lg flex items-center gap-2">
                                번개 스터디 만들기 <span className="transition-transform group-hover:translate-x-1">→</span>
                            </span>
                        </div>
                    </button>
                </div>

                {/* 도움말 */}
                <div className="mt-10 py-10 px-8 bg-gray-50 rounded-2xl border border-gray-200 text-center flex flex-col items-center justify-center gap-3">
                    <p className="font-bold text-gray-800 text-lg">
                        💡 어떤 걸 선택해야 할지 모르겠다면?
                    </p>
                    <p className="text-gray-600 leading-relaxed">
                        정기적으로 여러 회차에 걸쳐 진행할 스터디라면 <strong className="text-primary">계획형</strong>,<br />
                        특정 날짜에 한 번만 모일 예정이라면 <strong className="text-amber-500">번개 스터디</strong>를 선택하세요.
                    </p>
                </div>
            </div>
        </UserLayoutV2>
    );
};

export default StudyTypeSelectPage;
