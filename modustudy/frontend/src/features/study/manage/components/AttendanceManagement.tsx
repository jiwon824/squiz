import React, { useState } from 'react';
import { 
    Calendar, ChevronLeft, ChevronRight, Check, X, 
    Clock, AlertCircle, Users, TrendingUp 
} from 'lucide-react';

interface AttendanceManagementProps {
    studyId: number;
}

// Mock 데이터
const mockMembers = [
    { id: 1, name: '김철수', avatar: 'K', attendanceRate: 100 },
    { id: 2, name: '이영희', avatar: 'L', attendanceRate: 95 },
    { id: 3, name: '박민수', avatar: 'P', attendanceRate: 90 },
    { id: 4, name: '정다은', avatar: 'J', attendanceRate: 85 },
    { id: 5, name: '최준호', avatar: 'C', attendanceRate: 75 },
    { id: 6, name: '한소희', avatar: 'H', attendanceRate: 80 },
];

const mockMeetings = [
    { id: 1, date: '2026-01-20', title: '10주차 정기 미팅', time: '20:00' },
    { id: 2, date: '2026-01-23', title: '11주차 정기 미팅', time: '20:00' },
    { id: 3, date: '2026-01-27', title: '12주차 정기 미팅', time: '20:00' },
];

// 출석 상태: 'present' | 'absent' | 'late' | 'excused' | null
type AttendanceStatus = 'present' | 'absent' | 'late' | 'excused' | null;

const AttendanceManagement: React.FC<AttendanceManagementProps> = ({ studyId }) => {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [selectedMeeting, setSelectedMeeting] = useState(mockMeetings[0]);
    const [viewMode, setViewMode] = useState<'calendar' | 'list'>('list');

    // Mock 출석 데이터 생성
    const [attendanceData, setAttendanceData] = useState<Record<number, AttendanceStatus>>({
        1: 'present',
        2: 'present',
        3: 'late',
        4: 'present',
        5: 'absent',
        6: 'excused',
    });

    const handleAttendanceChange = (memberId: number, status: AttendanceStatus) => {
        setAttendanceData(prev => ({ ...prev, [memberId]: status }));
    };

    const getStatusStyle = (status: AttendanceStatus) => {
        switch (status) {
            case 'present': return 'bg-success/10 text-success border-success/30';
            case 'absent': return 'bg-error/10 text-error border-error/30';
            case 'late': return 'bg-warning/10 text-warning border-warning/30';
            case 'excused': return 'bg-info/10 text-info border-info/30';
            default: return 'bg-background-tertiary text-text-tertiary border-border-light';
        }
    };

    const getStatusLabel = (status: AttendanceStatus) => {
        switch (status) {
            case 'present': return '출석';
            case 'absent': return '결석';
            case 'late': return '지각';
            case 'excused': return '소명';
            default: return '미체크';
        }
    };

    const stats = {
        present: Object.values(attendanceData).filter(s => s === 'present').length,
        absent: Object.values(attendanceData).filter(s => s === 'absent').length,
        late: Object.values(attendanceData).filter(s => s === 'late').length,
        excused: Object.values(attendanceData).filter(s => s === 'excused').length,
    };

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary">출석 관리</h2>
                    <p className="text-sm text-text-secondary mt-1">미팅별 출석 현황을 관리하세요</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => setViewMode('list')}
                        className={`px-4 py-2 rounded-xl text-sm font-medium transition-all
                            ${viewMode === 'list' ? 'bg-primary text-white' : 'bg-background-secondary text-text-secondary hover:bg-background-tertiary'}`}
                    >
                        목록
                    </button>
                    <button
                        onClick={() => setViewMode('calendar')}
                        className={`px-4 py-2 rounded-xl text-sm font-medium transition-all
                            ${viewMode === 'calendar' ? 'bg-primary text-white' : 'bg-background-secondary text-text-secondary hover:bg-background-tertiary'}`}
                    >
                        캘린더
                    </button>
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

            {/* 미팅 선택 */}
            <div className="flex items-center gap-3 p-4 bg-background-secondary rounded-2xl">
                <Calendar size={20} className="text-primary" />
                <select
                    value={selectedMeeting.id}
                    onChange={(e) => setSelectedMeeting(mockMeetings.find(m => m.id === Number(e.target.value)) || mockMeetings[0])}
                    className="flex-1 bg-surface border border-border-light rounded-xl px-4 py-2 text-sm font-medium text-text-primary outline-none focus:border-primary"
                >
                    {mockMeetings.map((meeting) => (
                        <option key={meeting.id} value={meeting.id}>
                            {meeting.title} - {meeting.date} {meeting.time}
                        </option>
                    ))}
                </select>
            </div>

            {/* 출석 체크 리스트 */}
            <div className="bg-background-secondary rounded-2xl overflow-hidden">
                <div className="p-4 border-b border-border-light flex items-center justify-between">
                    <h3 className="font-bold text-text-primary flex items-center gap-2">
                        <Users size={18} />
                        멤버 출석 현황 ({mockMembers.length}명)
                    </h3>
                    <button className="text-sm text-primary font-medium hover:underline">
                        전체 출석 처리
                    </button>
                </div>
                
                <div className="divide-y divide-border-light">
                    {mockMembers.map((member) => (
                        <div key={member.id} className="p-4 flex items-center gap-4 hover:bg-surface/50 transition-colors">
                            {/* 아바타 */}
                            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                                {member.avatar}
                            </div>
                            
                            {/* 멤버 정보 */}
                            <div className="flex-1 min-w-0">
                                <div className="font-medium text-text-primary">{member.name}</div>
                                <div className="text-xs text-text-tertiary flex items-center gap-1">
                                    <TrendingUp size={12} />
                                    누적 출석률 {member.attendanceRate}%
                                </div>
                            </div>

                            {/* 출석 상태 버튼 */}
                            <div className="flex gap-2">
                                {(['present', 'late', 'absent', 'excused'] as AttendanceStatus[]).map((status) => (
                                    <button
                                        key={status}
                                        onClick={() => handleAttendanceChange(member.id, status)}
                                        className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all
                                            ${attendanceData[member.id] === status 
                                                ? getStatusStyle(status) 
                                                : 'bg-surface border-border-light text-text-tertiary hover:border-primary/30'
                                            }`}
                                    >
                                        {getStatusLabel(status)}
                                    </button>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* 저장 버튼 */}
            <div className="flex justify-end gap-3">
                <button className="px-6 py-3 rounded-xl text-sm font-medium bg-background-secondary text-text-secondary hover:bg-background-tertiary transition-colors">
                    초기화
                </button>
                <button className="px-6 py-3 rounded-xl text-sm font-bold bg-primary text-white hover:bg-primary-dark transition-colors shadow-md">
                    저장하기
                </button>
            </div>
        </div>
    );
};

export default AttendanceManagement;
