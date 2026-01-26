import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { scheduleStore } from '../services/scheduleStore';
import { Calendar } from './Calendar';
import { BackButton } from '@/shared/components';
import './CalendarExpandWidget.css';
import './ActivitySection.css';

// 캘린더 확장 위젯 - 대시보드 플래너 페이지
export const CalendarExpandWidget = () => {
    const navigate = useNavigate();
    const [isAdding, setIsAdding] = useState(false);

    // 일정 추가 모달 전용 상태
    const [modalDate, setModalDate] = useState<string>('');
    const [modalTitle, setModalTitle] = useState('');
    const [modalType, setModalType] = useState<'study' | 'project' | 'mentoring'>('study');

    // 전역 스토어 상태 연동
    const [plannerState, setPlannerState] = useState(scheduleStore.getState());
    const { schedules, goals, tags, memo } = plannerState;

    const [newGoal, setNewGoal] = useState('');
    const [tagInput, setTagInput] = useState('');
    const [selectedDate, setSelectedDate] = useState<string | null>(null);

    const [currentDate] = useState(new Date(2026, 0, 18));

    // 스토어 구독
    useEffect(() => {
        const unsubscribe = scheduleStore.subscribe((newState) => {
            setPlannerState(newState);
        });
        return () => {
            unsubscribe();
        };
    }, []);

    // 일정 추가 모달 열기
    const handleOpenAddModal = (date?: string) => {
        setModalDate(date || new Date().toISOString().split('T')[0]);
        setModalTitle('');
        setModalType('study');
        setIsAdding(true);
    };

    // 일정 저장
    const handleConfirmAddSchedule = (e: React.FormEvent) => {
        e.preventDefault();
        if (!modalTitle.trim()) return;
        scheduleStore.addSchedule(modalTitle, modalType, modalDate);
        setIsAdding(false);
    };

    // 목표 추가
    const handleAddGoal = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newGoal.trim()) return;
        scheduleStore.addGoal(newGoal);
        setNewGoal('');
    };

    // 태그 추가
    const handleAddTag = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && tagInput.trim()) {
            scheduleStore.addTag(tagInput.trim());
            setTagInput('');
        }
    };

    const selectedSchedules = selectedDate ? schedules.filter(s => s.date === selectedDate) : [];

    return (
        <div className="planner-page">
            {/* 페이지 헤더 */}
            <header className="planner-header">
                <div className="header-left">
                    <BackButton 
                        variant="icon-only" 
                        onClick={() => navigate(-1)} 
                    />
                    <div className="planner-date-display">
                        <span className="year-label">{currentDate.getFullYear()}</span>
                        <span className="month-label">{currentDate.getMonth() + 1}월</span>
                    </div>
                </div>

                <div className="header-right action-group">
                    <button className="planner-action-btn primary" onClick={() => handleOpenAddModal()}>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        <span>일정 추가</span>
                    </button>
                    <button className="planner-action-btn secondary" onClick={() => scheduleStore.syncGoogleCalendar()}>
                        <img src="https://www.gstatic.com/images/branding/product/1x/calendar_2020q4_48dp.png" alt="G" />
                        <span>Google 연동</span>
                    </button>
                </div>
            </header>

            {/* 메인 콘텐츠 그리드 */}
            <main className="planner-content-grid">
                {/* 왼쪽: 캘린더 영역 */}
                <section className="planner-calendar-area">
                    <Calendar
                        currentDate={currentDate}
                        schedules={schedules}
                        onDateClick={(date) => setSelectedDate(date)}
                        onQuickAdd={(date) => handleOpenAddModal(date)}
                    />
                </section>

                {/* 오른쪽: 사이드바 위젯 */}
                <aside className="planner-sidebar-area">
                    {/* 학습 목표 위젯 */}
                    <div className="sidebar-widget goals-widget">
                        <div className="goals-header">
                            <h3>오늘의 학습 목표</h3>
                            <span className="goal-date">01.18 Sun</span>
                        </div>

                        <form className="goal-input-form-dark" onSubmit={handleAddGoal}>
                            <input
                                type="text"
                                placeholder="새로운 목표 추가..."
                                value={newGoal}
                                onChange={(e) => setNewGoal(e.target.value)}
                            />
                            <button type="submit">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                            </button>
                        </form>

                        <div className="goals-list-dark scroll-hide">
                            {goals.map(goal => (
                                <div
                                    key={goal.id}
                                    className={`goal-item-dark ${goal.completed ? 'completed' : ''}`}
                                    onClick={() => scheduleStore.toggleGoal(goal.id)}
                                >
                                    <div className="goal-checkbox-dark">
                                        {goal.completed && <span>✓</span>}
                                    </div>
                                    <span className="goal-text">{goal.text}</span>
                                </div>
                            ))}
                        </div>

                        <div className="goals-footer-dark">
                            <div className="progress-info">
                                <p>{goals.filter(g => g.completed).length} / {goals.length} Done</p>
                                <span>{goals.length > 0 ? Math.round((goals.filter(g => g.completed).length / goals.length) * 100) : 0}%</span>
                            </div>
                            <div className="progress-bar-dark">
                                <div
                                    className="progress-fill-dark"
                                    style={{ width: `${goals.length > 0 ? (goals.filter(g => g.completed).length / goals.length) * 100 : 0}%` }}
                                ></div>
                            </div>
                        </div>
                    </div>

                    {/* 학습 메모 위젯 */}
                    <div className="sidebar-widget memo-widget">
                        <div className="memo-header">
                            <h3>학습 메모</h3>
                            <div className="tag-list">
                                {tags.map(tag => (
                                    <span key={tag.id} className="tag-badge">
                                        #{tag.text}
                                        <button onClick={(e) => { e.stopPropagation(); scheduleStore.removeTag(tag.id); }}>×</button>
                                    </span>
                                ))}
                                <input
                                    className="tag-input"
                                    placeholder="#태그"
                                    value={tagInput}
                                    onChange={(e) => setTagInput(e.target.value)}
                                    onKeyDown={handleAddTag}
                                />
                            </div>
                        </div>
                        <textarea
                            placeholder="오늘의 인사이트를 기록하세요..."
                            value={memo}
                            onChange={(e) => scheduleStore.updateMemo(e.target.value)}
                        ></textarea>
                    </div>
                </aside>
            </main>

            {/* 일정 추가 모달 */}
            {isAdding && (
                <div className="day-select-overlay" onClick={() => setIsAdding(false)}>
                    <div className="schedule-modal animate-modal-pop" onClick={e => e.stopPropagation()}>
                        <header className="modal-header-formal">
                            <h2>새 일정 추가</h2>
                            <button className="close-btn" onClick={() => setIsAdding(false)}>×</button>
                        </header>

                        <form className="schedule-formal-form" onSubmit={handleConfirmAddSchedule}>
                            <div className="form-group">
                                <label>일정 제목</label>
                                <input
                                    type="text"
                                    className="formal-input"
                                    placeholder="무엇을 할 계획인가요?"
                                    value={modalTitle}
                                    onChange={(e) => setModalTitle(e.target.value)}
                                    autoFocus
                                    required
                                />
                            </div>

                            <div className="form-row">
                                <div className="form-group flex-1">
                                    <label>날짜</label>
                                    <input
                                        type="date"
                                        className="formal-input"
                                        value={modalDate}
                                        onChange={(e) => setModalDate(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-group flex-1">
                                    <label>유형</label>
                                    <select
                                        className="formal-select"
                                        value={modalType}
                                        onChange={(e) => setModalType(e.target.value as any)}
                                    >
                                        <option value="study">스터디</option>
                                        <option value="project">프로젝트</option>
                                        <option value="mentoring">멘토링</option>
                                    </select>
                                </div>
                            </div>

                            <button type="submit" className="confirm-btn-formal primary">
                                일정 저장하기
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* 날짜 상세 모달 */}
            {selectedDate && !isAdding && (
                <div className="day-select-overlay" onClick={() => setSelectedDate(null)}>
                    <div className="day-select-modal animate-modal-pop" onClick={e => e.stopPropagation()}>
                        <header className="modal-header-compact">
                            <div className="date-info">
                                <h2>{selectedDate.split('-')[1]}월 {selectedDate.split('-')[2]}일</h2>
                                <span>전체 일정 목록</span>
                            </div>
                            <button className="close-btn" onClick={() => setSelectedDate(null)}>×</button>
                        </header>
                        <div className="modal-schedule-list scroll-hide" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                            {selectedSchedules.length > 0 ? (
                                selectedSchedules.map(s => (
                                    <div key={s.id} className={`modal-schedule-item ${s.type}`}>
                                        <div className="type-indicator"></div>
                                        <div className="item-content">
                                            <span className="item-title">{s.title}</span>
                                            <span className="item-type">{s.type.toUpperCase()}</span>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="empty-modal-state" style={{ textAlign: 'center', padding: '2rem', opacity: 0.5 }}>
                                    <p>등록된 일정이 없습니다.</p>
                                </div>
                            )}
                        </div>
                        <button className="modal-add-btn" onClick={() => {
                            const d = selectedDate;
                            setSelectedDate(null);
                            handleOpenAddModal(d);
                        }}>
                            + 새 일정 추가하기
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};
