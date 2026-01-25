import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Heart, Star, Users, Calendar,
    ChevronLeft, Target, Award, Shield, Clock, MapPin, Share2, MoreVertical
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { studyService, Study } from '../services/studyService';
import StudyApplyModal from './StudyApplyModal';
import StudyListContainer from './StudyListContainer';
import { MainLayout } from '@/layouts/MainLayout';
import { Button, ArrowButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';

const StudyDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [study, setStudy] = useState<Study | null>(null);
    const [isBookmarked, setIsBookmarked] = useState(false);
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const { user } = useAuthStore();

    useEffect(() => {
        if (id) {
            const data = studyService.getStudyById(Number(id));
            if (data) {
                setStudy(data);
                setIsBookmarked(data.isBookmarked);
            }
        }
    }, [id]);

    if (!study) return null;

    const handleBookmarkToggle = () => {
        studyService.toggleBookmark(study.id);
        setIsBookmarked(!isBookmarked);
    };

    // 난이도 텍스트 변환
    const getDifficultyText = (difficulty: string) => {
        const map: { [key: string]: string } = {
            'BEGINNER': '입문',
            'INTERMEDIATE': '중급',
            'ADVANCED': '고급'
        };
        return map[difficulty] || difficulty;
    };

    return (
        <MainLayout>
            <StudyListContainer>
                <div className="study-detail-container">
                    {/* 뒤로가기 브레드크럼 */}
                    <button className="btn-back" onClick={() => navigate('/study')} style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', fontWeight: 600 }}>
                        <ChevronLeft size={20} />
                        <span>전체 목록으로</span>
                    </button>

                    {/* 히어로 섹션 */}
                    <section className={`study-detail-hero difficulty-${study.difficulty.toLowerCase()}`}>
                        <div className="hero-content">
                            <div className="hero-badges">
                                <span className={`badge badge-meeting meeting-${study.meetingType.toLowerCase()}`}>
                                    {study.meetingType === 'ONLINE' ? '온라인' : study.meetingType === 'OFFLINE' ? '오프라인' : '혼합'}
                                </span>
                                <span className="badge badge-difficulty" style={{ backgroundColor: 'var(--color-background-tertiary)', color: 'var(--color-text-primary)' }}>
                                    {getDifficultyText(study.difficulty)}
                                </span>
                            </div>
                            <h1 className="hero-title">{study.name}</h1>
                            <p className="hero-description">{study.description}</p>
                        </div>
                        <div className="hero-actions">
                            <button
                                className={`btn-bookmark-big ${isBookmarked ? 'active' : ''}`}
                                onClick={handleBookmarkToggle}
                            >
                                <Heart size={20} fill={isBookmarked ? 'currentColor' : 'none'} />
                                <span>{isBookmarked ? '찜한 스터디' : '찜하기'}</span>
                            </button>
                        </div>
                    </section>

                    {/* 메인 상세 정보 그리드 */}
                    <div className="study-detail-grid">
                        <div className="detail-main-section">
                            {/* 모집 요강 */}
                            <div className="detail-card">
                                <h2 className="detail-card-title">
                                    <Target size={22} />
                                    모집 요강
                                </h2>
                                <div className="info-grid">
                                    <div className="info-block">
                                        <span className="info-label">진행 방식</span>
                                        <span className="info-value">
                                            {study.meetingType === 'ONLINE' ? '🏡 온라인 진행' : '📍 오프라인 진행'}
                                        </span>
                                    </div>
                                    <div className="info-block">
                                        <span className="info-label">활동 지역</span>
                                        <span className="info-value">{study.region?.name || '전국 (온라인)'}</span>
                                    </div>
                                    <div className="info-block">
                                        <span className="info-label">모집 인원</span>
                                        <span className="info-value">{study.currentMembers} / {study.maxMembers} 명</span>
                                    </div>
                                    <div className="info-block">
                                        <span className="info-label">난이도</span>
                                        <span className="info-value">{getDifficultyText(study.difficulty)}</span>
                                    </div>
                                </div>
                            </div>

                            {/* 활동 스케줄 */}
                            <div className="detail-card">
                                <h2 className="detail-card-title">
                                    <Calendar size={22} />
                                    활동 스케줄
                                </h2>
                                <div className="info-grid">
                                    <div className="info-block">
                                        <span className="info-label">활동 요일</span>
                                        <span className="info-value">{study.scheduleDays}</span>
                                    </div>
                                    {study.scheduleTime && (
                                        <div className="info-block">
                                            <span className="info-label">활동 시간</span>
                                            <span className="info-value">{study.scheduleTime.substring(0, 5)} ~</span>
                                        </div>
                                    )}
                                    <div className="info-block">
                                        <span className="info-label">모집 종료</span>
                                        <span className="info-value">{study.recruitEndDate || '모집 중'}</span>
                                    </div>
                                </div>
                            </div>

                            {/* 커리큘럼/목표 (플레이스홀더) */}
                            <div className="detail-card">
                                <h2 className="detail-card-title">
                                    <Award size={22} />
                                    스터디 목표 및 커리큘럼
                                </h2>
                                <p style={{ color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                                    본 스터디는 **{study.topic}** 분야의 지식을 함께 습득하는 것을 목표로 합니다.
                                    주로 문제 풀이와 토론 형식으로 진행되며, 매주 정해진 분량의 학습 내용을 공유합니다.
                                </p>
                            </div>
                        </div>

                        {/* 우측 사이드바: 리더 정보 및 신청 */}
                        <aside className="detail-sidebar">
                            <div className="detail-card leader-card">
                                <div className="leader-profile">
                                    <div className="leader-big-avatar">
                                        {study.leader.nickname.charAt(0)}
                                    </div>
                                    <div className="leader-details">
                                        <h3>{study.leader.nickname}</h3>
                                        <div className="leader-stats">
                                            <div className="stat-item">
                                                <Star size={16} fill="currentColor" />
                                                <span>{study.leader.leaderRating.toFixed(1)}</span>
                                            </div>
                                            <div className="stat-item">
                                                <Users size={16} />
                                                <span>{study.leader.leaderReviewCount}개의 리뷰</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="apply-section">
                                    {String(user?.id) === String(study.leader.id) ? (
                                        <button className="btn-apply" onClick={() => navigate(`/study/manage/${study.id}`)}>
                                            스터디 관리하기
                                        </button>
                                    ) : (
                                        <button className="btn-apply" onClick={() => setIsApplyModalOpen(true)}>
                                            스터디 신청하기
                                        </button>
                                    )}
                                    <p style={{ fontSize: '12px', color: 'var(--color-text-tertiary)', textAlign: 'center' }}>
                                        {String(user?.id) === String(study.leader.id)
                                            ? '스터디 신청자, 멤버, 출석 등을 관리할 수 있습니다.'
                                            : '신청 후 스터디장의 승인이 필요합니다.'}
                                    </p>
                                </div>
                            </div>

                            {/* 리더 활동 잔디 (2D Grass Graph) */}
                            <div className="detail-card leader-activity-card" style={{ marginTop: '16px' }}>
                                <h4 style={{ fontSize: '14px', marginBottom: '12px', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <Target size={16} />
                                    리더 활동 지수
                                </h4>
                                <div className="grass-placeholder">
                                    <div className="grass-track">
                                        {Array.from({ length: 28 }).map((_, i) => (
                                            <div
                                                key={i}
                                                className={`grass-node level-${Math.floor(Math.random() * 4)}`}
                                                title={`Activity level: ${Math.floor(Math.random() * 4)}`}
                                            />
                                        ))}
                                    </div>
                                    <p style={{ fontSize: '11px', color: 'var(--color-text-tertiary)', marginTop: '10px', textAlign: 'center' }}>
                                        최근 한 달간의 스터디 참여 활동입니다.
                                    </p>
                                </div>
                            </div>

                            <div className="detail-card" style={{ marginTop: '20px' }}>
                                <h4 style={{ fontSize: '14px', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <Shield size={16} />
                                    안전한 스터디 참여
                                </h4>
                                <ul style={{ paddingLeft: '18px', fontSize: '12px', color: 'var(--color-text-tertiary)', margin: 0, gap: '6px', display: 'flex', flexDirection: 'column' }}>
                                    <li>비매너 사용자는 신고가 가능합니다.</li>
                                    <li>개인정보 유출에 유의해주세요.</li>
                                    <li>스터디 규칙을 준수해주세요.</li>
                                </ul>
                            </div>
                        </aside>
                    </div>
                </div>
            </StudyListContainer>

            {isApplyModalOpen && study && (
                <StudyApplyModal
                    study={study}
                    onClose={() => setIsApplyModalOpen(false)}
                />
            )}
        </MainLayout>
    );
};

export default StudyDetailPage;
