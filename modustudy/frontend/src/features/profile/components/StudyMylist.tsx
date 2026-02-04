import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Modal } from '@/shared/components';
import { Users, Clock, ChevronRight } from 'lucide-react';

export interface StudyActivity {
    id: number;
    title: string;
    status: 'active' | 'completed' | 'pending';
    description: string;
    attendanceRate: number;
    participationDays: number;
}

interface StudyMylistProps {
    studies: StudyActivity[];
}

export const StudyMylist: React.FC<StudyMylistProps> = ({ studies }) => {
    const navigate = useNavigate();
    const [isAllListOpen, setIsAllListOpen] = useState(false);

    // 진행 중 및 예정 단계 필터링 (active + pending)
    const activeStudies = studies.filter(s => s.status === 'active' || s.status === 'pending');
    // 완료된 스터디 필터링
    const completedStudies = studies.filter(s => s.status === 'completed');

    const handleCardClick = (id: number) => {
        navigate(`/study/${id}`);
    };

    const sectionHeaderClass = "text-sm font-bold text-gray-400 px-1 flex items-center justify-between w-full mb-3";
    const indicatorClass = "w-1.5 h-1.5 rounded-full mr-2";

    const StudyItem = ({ study }: { study: StudyActivity }) => (
        <Card
            key={study.id}
            variant="flat"
            className="study-activity-card relative cursor-pointer bg-white transition-all duration-300 group overflow-hidden"
            onClick={() => handleCardClick(study.id)}
        >
            <div className="absolute top-0 left-0 w-1 h-full bg-blue-500 opacity-0 group-hover:opacity-100 transition-opacity" />
            <div className="p-5">
                <div className="study-activity-header flex justify-between items-center mb-3">
                    <h3 className="font-bold text-[#1a202c] group-hover:text-blue-600 transition-colors tracking-tight">
                        {study.title}
                    </h3>
                    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-bold tracking-wider ${
                        study.status === 'active'
                            ? 'bg-blue-50 text-blue-600 border border-blue-100'
                            : study.status === 'pending'
                            ? 'bg-amber-50 text-amber-600 border border-amber-100'
                            : 'bg-green-50 text-green-600 border border-green-100'
                        }`}>
                        {study.status === 'active' ? '진행중' : study.status === 'pending' ? '예정' : '완료'}
                    </span>
                </div>
                <p className="study-activity-desc text-sm text-gray-500 mb-4 line-clamp-1 leading-relaxed">{study.description}</p>
                <div className="study-activity-stats flex items-center gap-4">
                    <div className="flex items-center gap-1.5 px-2 py-1 bg-blue-50/50 rounded-md">
                        <Users size={12} className="text-blue-500" />
                        <span className="text-[11px] font-bold text-blue-700">{study.attendanceRate}%</span>
                    </div>
                    <div className="flex items-center gap-1.5 px-2 py-1 bg-orange-50/50 rounded-md">
                        <Clock size={12} className="text-orange-500" />
                        <span className="text-[11px] font-bold text-orange-700">{study.participationDays}일</span>
                    </div>
                </div>
            </div>
            <div className="absolute bottom-4 right-4 translate-x-4 opacity-0 group-hover:translate-x-0 group-hover:opacity-100 transition-all duration-300">
                <ChevronRight size={18} className="text-blue-400" />
            </div>
        </Card>
    );

    return (
        <div className="my-studies-section space-y-5 animate-in fade-in slide-in-from-bottom-4 duration-700">
            <div className="flex justify-between items-center bg-white p-5 rounded-[24px] shadow-[0_4px_15px_rgba(0,0,0,0.05)]">
                <h2 className="section-title !m-0 !p-0 flex items-center gap-3">
                    내 스터디 활동
                </h2>
                <Button
                    variant="ghost"
                    size="sm"
                    rightIcon={<ChevronRight size={14} />}
                    className="text-gray-400 hover:text-blue-600 font-semibold transition-colors"
                    onClick={() => setIsAllListOpen(true)}
                >
                    전체 목록 보기
                </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* 왼쪽: 진행 중 및 예정 */}
                <div className="space-y-3">
                    <div className={sectionHeaderClass}>
                        <div className="flex items-center">
                            <div className={`${indicatorClass} bg-blue-500`} />
                            <span>진행 중 및 예정</span>
                        </div>
                        <span className="text-[11px] font-bold bg-blue-50 text-blue-500 px-2 py-0.5 rounded-lg border border-blue-100">
                            {activeStudies.length}
                        </span>
                    </div>
                    <div className="studies-scroll-container h-[650px] overflow-y-auto pr-2 space-y-3 custom-scrollbar">
                        {activeStudies.length > 0 ? (
                            activeStudies.map(study => <StudyItem key={study.id} study={study} />)
                        ) : (
                            <div className="h-full flex items-center justify-center text-sm text-gray-400 border-2 border-dashed rounded-2xl">
                                진행 중인 스터디가 없습니다.
                            </div>
                        )}
                    </div>
                </div>

                {/* 오른쪽: 완료 */}
                <div className="space-y-3">
                    <div className={sectionHeaderClass}>
                        <div className="flex items-center">
                            <div className={`${indicatorClass} bg-gray-300`} />
                            <span>완료된 스터디</span>
                        </div>
                        <span className="text-[11px] font-bold bg-gray-50 text-gray-400 px-2 py-0.5 rounded-lg border border-gray-100">
                            {completedStudies.length}
                        </span>
                    </div>
                    <div className="studies-scroll-container h-[650px] overflow-y-auto pr-2 space-y-3 custom-scrollbar">
                        {completedStudies.length > 0 ? (
                            completedStudies.map(study => <StudyItem key={study.id} study={study} />)
                        ) : (
                            <div className="h-full flex items-center justify-center text-sm text-gray-400 border-2 border-dashed rounded-2xl">
                                완료된 스터디가 없습니다.
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 전체 목록 모달 */}
            <Modal
                isOpen={isAllListOpen}
                onClose={() => setIsAllListOpen(false)}
                title="참여 스터디 전체 목록"
                maxWidth="3xl"
            >
                <div className="p-2">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-h-[60vh] overflow-y-auto pr-2 custom-scrollbar">
                        {[...activeStudies, ...completedStudies].map(study => (
                            <StudyItem key={study.id} study={study} />
                        ))}
                    </div>
                </div>
            </Modal>

            <style>{`
                .custom-scrollbar::-webkit-scrollbar {
                    width: 6px;
                }
                .custom-scrollbar::-webkit-scrollbar-track {
                    background: transparent;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background: #e2e8f0;
                    border-radius: 10px;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb:hover {
                    background: #cbd5e1;
                }
            `}</style>
        </div>
    );
};
