import React, { useState, useEffect } from 'react';
import {
    Calendar, Check, X,
    Clock, AlertCircle, Users
} from 'lucide-react';
import { studyApi } from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

// 기본 프로필 이미지 경로 (추후 사용 예정)
// const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

interface AttendanceManagementProps {
    studyId: number;
}

interface Session {
    id: number;
    title: string;
    sessionNumber: number;
    scheduledAt: string;
}

interface AttendanceRecord {
    id: number;
    userId: number;
    userName?: string;
    status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED' | null;
    checkedAt?: string;
    excuseStatus?: string;
}

const AttendanceManagement: React.FC<AttendanceManagementProps> = ({ studyId }) => {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [selectedSession, setSelectedSession] = useState<Session | null>(null);
    const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const { showToast } = useUIStore();

    useEffect(() => {
        fetchSessions();
    }, [studyId]);

    useEffect(() => {
        if (selectedSession) {
            fetchAttendance(selectedSession.id);
        }
    }, [selectedSession]);

    const fetchSessions = async () => {
        setLoading(true);
        try {
            const response = await studyApi.getStudySessions(studyId);
            const sessionList = response || [];
            setSessions(sessionList);
            if (sessionList.length > 0) {
                setSelectedSession(sessionList[0]);
            }
        } catch (error) {
            console.error('세션 목록 조회 실패:', error);
            showToast('세션 목록을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const fetchAttendance = async (sessionId: number) => {
        try {
            const response = await studyApi.getSessionAttendance(studyId, sessionId);
            const records = response?.data || response || [];
            setAttendanceRecords(records);
        } catch (error) {
            console.error('출석 정보 조회 실패:', error);
            showToast('출석 정보를 불러오는데 실패했습니다.', 'error');
        }
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return `${date.getMonth() + 1}/${date.getDate()}`;
    };

    const formatTime = (dateString: string) => {
        const date = new Date(dateString);
        return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    };

    const getStatusStyle = (status: string | null) => {
        switch (status) {
            case 'PRESENT': return 'bg-success/10 text-success border-success/30';
            case 'ABSENT': return 'bg-error/10 text-error border-error/30';
            case 'LATE': return 'bg-warning/10 text-warning border-warning/30';
            case 'EXCUSED': return 'bg-info/10 text-info border-info/30';
            default: return 'bg-background-tertiary text-text-tertiary border-border-light';
        }
    };

    const getStatusLabel = (status: string | null) => {
        switch (status) {
            case 'PRESENT': return '출석';
            case 'ABSENT': return '결석';
            case 'LATE': return '지각';
            case 'EXCUSED': return '소명';
            default: return '미체크';
        }
    };

    const stats = {
        present: attendanceRecords.filter(r => r.status === 'PRESENT').length,
        absent: attendanceRecords.filter(r => r.status === 'ABSENT').length,
        late: attendanceRecords.filter(r => r.status === 'LATE').length,
        excused: attendanceRecords.filter(r => r.status === 'EXCUSED').length,
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">출석 정보를 불러오는 중...</p>
            </div>
        );
    }

    if (sessions.length === 0) {
        return (
            <div className="text-center py-12 bg-background-secondary rounded-2xl">
                <Calendar size={48} className="mx-auto text-text-muted mb-4" />
                <p className="text-text-secondary">아직 생성된 세션이 없습니다</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary">출석 관리</h2>
                    <p className="text-sm text-text-secondary mt-1">미팅별 출석 현황을 관리하세요</p>
                </div>
            </div>

            {/* 통계 요약 */}
            <div className="grid grid-cols-4 gap-3">
                {[
                    { label: '출석', value: stats.present, color: 'success', icon: <Check size={16} /> },
                    { label: '결석', value: stats.absent, color: 'error', icon: <X size={16} /> },
                    { label: '지각', value: stats.late, color: 'warning', icon: <Clock size={16} /> },
                    { label: '소명', value: stats.excused, color: 'info', icon: <AlertCircle size={16} /> },
                ].map((stat) => (
                    <div key={stat.label} className={`bg-${stat.color}/5 rounded-xl p-4 border border-${stat.color}/20`}>
                        <div className="flex items-center gap-2 mb-2">
                            <span className={`text-${stat.color}`}>{stat.icon}</span>
                            <span className="text-sm text-text-secondary">{stat.label}</span>
                        </div>
                        <div className={`text-2xl font-bold text-${stat.color}`}>{stat.value}</div>
                    </div>
                ))}
            </div>

            {/* 세션 선택 */}
            {selectedSession && (
                <div className="flex items-center gap-3 p-4 bg-background-secondary rounded-2xl">
                    <Calendar size={20} className="text-primary" />
                    <select
                        value={selectedSession.id}
                        onChange={(e) => {
                            const session = sessions.find(s => s.id === Number(e.target.value));
                            if (session) setSelectedSession(session);
                        }}
                        className="flex-1 bg-surface border border-border-light rounded-xl px-4 py-2 text-sm font-medium text-text-primary outline-none focus:border-primary"
                    >
                        {sessions.map((session) => (
                            <option key={session.id} value={session.id}>
                                {session.title} - {formatDate(session.scheduledAt)} {formatTime(session.scheduledAt)}
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {/* 출석 체크 리스트 */}
            <div className="bg-background-secondary rounded-2xl overflow-hidden">
                <div className="p-4 border-b border-border-light flex items-center justify-between">
                    <h3 className="font-bold text-text-primary flex items-center gap-2">
                        <Users size={18} />
                        멤버 출석 현황 ({attendanceRecords.length}명)
                    </h3>
                </div>

                <div className="divide-y divide-border-light">
                    {attendanceRecords.length === 0 ? (
                        <div className="p-8 text-center text-text-secondary">
                            출석 정보가 없습니다
                        </div>
                    ) : (
                        attendanceRecords.map((record) => (
                            <div key={record.id} className="p-4 flex items-center gap-4 hover:bg-surface/50 transition-colors">
                                {/* 아바타 */}
                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                                    {(record.userName?.trim() || `User ${record.userId}`).charAt(0).toUpperCase()}
                                </div>

                                {/* 멤버 정보 */}
                                <div className="flex-1 min-w-0">
                                    <div className="font-medium text-text-primary">
                                        {record.userName?.trim() || `User ${record.userId}`}
                                    </div>
                                    {record.checkedAt && (
                                        <div className="text-xs text-text-tertiary">
                                            체크 시각: {formatTime(record.checkedAt)}
                                        </div>
                                    )}
                                </div>

                                {/* 출석 상태 표시 */}
                                <div className={`px-3 py-1.5 rounded-lg text-xs font-medium border ${getStatusStyle(record.status)}`}>
                                    {getStatusLabel(record.status)}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default AttendanceManagement;
