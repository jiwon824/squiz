import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Info, Settings, Calendar, Plus, Trash2, BookOpen } from 'lucide-react';
import StudyListContainer from './StudyListContainer';
import { MainLayout } from '@/layouts/MainLayout';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { cn } from '@/shared/utils/cn';
import '../styles/StudyCreatePage.css';

interface CurriculumItem {
    week: number;
    description: string;
}

const StudyCreatePage: React.FC = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        topic: '알고리즘',
        difficulty: 'BEGINNER',
        meetingType: 'ONLINE',
        maxMembers: 4,
        scheduleDays: [] as string[],
        studyType: 'PLANNED',
        curriculum: [{ week: 1, description: '' }] as CurriculumItem[]
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleOptionToggle = (name: string, value: string) => {
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleCurriculumChange = (index: number, value: string) => {
        const newCurriculum = [...formData.curriculum];
        newCurriculum[index].description = value;
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const addWeek = () => {
        setFormData(prev => ({
            ...prev,
            curriculum: [...prev.curriculum, { week: prev.curriculum.length + 1, description: '' }]
        }));
    };

    const removeWeek = (index: number) => {
        const newCurriculum = formData.curriculum
            .filter((_, i) => i !== index)
            .map((item, i) => ({ ...item, week: i + 1 }));
        setFormData(prev => ({ ...prev, curriculum: newCurriculum }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('Form submitted:', formData);
        // TODO: API 연결
        alert('스터디가 생성되었습니다! (모의)');
        navigate('/study');
    };

    return (
        <MainLayout>
            <StudyListContainer>
                <div className="study-create-container">
                    <header className="study-create-header">
                        <Button 
                            variant="back-button" 
                            onClick={() => navigate(-1)}
                            leftIcon={<ChevronLeft size={20} />}
                            className="pl-0 text-text-secondary hover:text-text-primary mb-4"
                        >
                            뒤로가기
                        </Button>
                        <h1 className="text-2xl font-bold text-text-primary mb-2">새로운 스터디 시작하기</h1>
                        <p className="text-text-secondary">함께 성장할 팀원을 모집해보세요.</p>
                    </header>

                    <form className="study-create-card flex flex-col gap-8" onSubmit={handleSubmit}>
                        {/* 기본 정보 스택 */}
                        <div className="create-form-section">
                            <h2 className="section-title flex items-center gap-2 text-lg font-bold text-text-primary mb-4">
                                <Info size={20} className="text-primary" />
                                기본 정보
                            </h2>
                            <div className="form-grid flex flex-col gap-4">
                                <div className="form-group full-width">
                                    <Input
                                        label="스터디 이름"
                                        name="name"
                                        placeholder="모두가 이해하기 쉬운 이름을 지어주세요"
                                        required
                                        value={formData.name}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div className="form-group full-width flex flex-col gap-1.5">
                                    <label htmlFor="description" className="text-sm font-bold text-gray-700 ml-1">스터디 설명</label>
                                    <textarea
                                        id="description"
                                        name="description"
                                        className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none min-h-[120px] resize-none text-base"
                                        placeholder="스터디의 목표와 진행 방식에 대해 자세히 적어주세요"
                                        required
                                        value={formData.description}
                                        onChange={handleChange}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* 커리큘럼 섹션 (새로 추가됨) */}
                        <div className="create-form-section">
                            <h2 className="section-title flex items-center gap-2 text-lg font-bold text-text-primary mb-4">
                                <BookOpen size={20} className="text-primary" />
                                커리큘럼 계획
                            </h2>
                            <div className="flex flex-col gap-3">
                                <p className="text-sm text-text-tertiary mb-2">
                                    주차별 학습 목표를 설정하면 참여자들이 스터디 방향을 파악하는 데 도움이 됩니다.
                                </p>
                                
                                {formData.curriculum.map((item, index) => (
                                    <div key={index} className="flex gap-2 items-start animate-slide-in-up">
                                        <div className="flex-shrink-0 w-16 pt-3.5 text-center">
                                            <span className="text-sm font-bold text-primary">{item.week}주차</span>
                                        </div>
                                        <div className="flex-grow">
                                            <Input
                                                placeholder={`${item.week}주차 학습 목표나 주제를 입력하세요`}
                                                value={item.description}
                                                onChange={(e) => handleCurriculumChange(index, e.target.value)}
                                                required
                                            />
                                        </div>
                                        <div className="flex-shrink-0 pt-0.5">
                                            {formData.curriculum.length > 1 && (
                                                <Button 
                                                    type="button" 
                                                    variant="ghost" 
                                                    size="md"
                                                    onClick={() => removeWeek(index)}
                                                    className="text-text-tertiary hover:text-error hover:bg-error/10"
                                                >
                                                    <Trash2 size={18} />
                                                </Button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                                
                                <div className="mt-2 pl-16">
                                    <Button 
                                        type="button" 
                                        variant="outline" 
                                        onClick={addWeek} 
                                        fullWidth
                                        leftIcon={<Plus size={18} />}
                                        className="border-dashed"
                                    >
                                        주차 추가하기
                                    </Button>
                                </div>
                            </div>
                        </div>

                        {/* 상세 설정 섹션 */}
                        <div className="create-form-section">
                            <h2 className="section-title flex items-center gap-2 text-lg font-bold text-text-primary mb-4">
                                <Settings size={20} className="text-primary" />
                                상세 설정
                            </h2>
                            <div className="form-grid grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="form-group flex flex-col gap-1.5">
                                    <label className="text-sm font-bold text-gray-700 ml-1">주제</label>
                                    <div className="relative">
                                        <select
                                            name="topic"
                                            className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all outline-none appearance-none"
                                            value={formData.topic}
                                            onChange={handleChange}
                                        >
                                            <option value="알고리즘">알고리즘</option>
                                            <option value="프론트엔드">프론트엔드</option>
                                            <option value="백엔드">백엔드</option>
                                            <option value="데이터베이스">데이터베이스</option>
                                            <option value="CS">CS 지식</option>
                                            <option value="안드로이드">안드로이드</option>
                                            <option value="iOS">iOS</option>
                                        </select>
                                        <div className="absolute right-4 top-1/2 transform -translate-y-1/2 pointer-events-none text-gray-500">
                                            <ChevronLeft size={16} className="-rotate-90" />
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group flex flex-col gap-1.5">
                                    <label className="text-sm font-bold text-gray-700 ml-1">최대 인원</label>
                                    <Input
                                        type="number"
                                        name="maxMembers"
                                        min="2"
                                        max="50"
                                        value={formData.maxMembers}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div className="form-group flex flex-col gap-1.5">
                                    <label className="text-sm font-bold text-gray-700 ml-1">권장 난이도</label>
                                    <div className="flex gap-2 p-1 bg-gray-50 rounded-2xl border border-gray-200">
                                        {['BEGINNER', 'INTERMEDIATE', 'ADVANCED'].map((level) => (
                                            <button
                                                key={level}
                                                type="button"
                                                className={cn(
                                                    "flex-1 py-2.5 text-sm font-semibold rounded-xl transition-all",
                                                    formData.difficulty === level 
                                                        ? "bg-white text-primary shadow-sm ring-1 ring-gray-100" 
                                                        : "text-text-tertiary hover:bg-gray-100"
                                                )}
                                                onClick={() => handleOptionToggle('difficulty', level)}
                                            >
                                                {level === 'BEGINNER' && '입문'}
                                                {level === 'INTERMEDIATE' && '중급'}
                                                {level === 'ADVANCED' && '고급'}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div className="form-group flex flex-col gap-1.5">
                                    <label className="text-sm font-bold text-gray-700 ml-1">진행 방식</label>
                                    <div className="flex gap-2 p-1 bg-gray-50 rounded-2xl border border-gray-200">
                                        {['ONLINE', 'OFFLINE', 'HYBRID'].map((type) => (
                                            <button
                                                key={type}
                                                type="button"
                                                className={cn(
                                                    "flex-1 py-2.5 text-sm font-semibold rounded-xl transition-all",
                                                    formData.meetingType === type
                                                        ? "bg-white text-primary shadow-sm ring-1 ring-gray-100"
                                                        : "text-text-tertiary hover:bg-gray-100"
                                                )}
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
                        </div>

                        {/* 버튼 영역 */}
                        <div className="flex gap-3 pt-4 border-t border-gray-100">
                            <Button 
                                type="button" 
                                variant="google-outline" 
                                size="xl"
                                fullWidth
                                onClick={() => navigate(-1)}
                            >
                                취소
                            </Button>
                            <Button 
                                type="submit" 
                                variant="primary" 
                                size="xl"
                                fullWidth
                                className="shadow-lg shadow-primary/20"
                            >
                                개설하기
                            </Button>
                        </div>
                    </form>
                </div>
            </StudyListContainer>
        </MainLayout>
    );
};

export default StudyCreatePage;
