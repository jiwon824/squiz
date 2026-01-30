import React, { useState, useEffect } from 'react';
import { studyApi } from '@/api/endpoints/studyApi';
import { UserMinus, Users, CheckCircle2, Search, Crown, Shield, TrendingUp } from 'lucide-react';
import RoleBadge from '@/shared/components/RoleBadge';
import { useUIStore } from '@/store/uiStore';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

interface MemberManagementProps {
    studyId: number;
    maxMembers: number;
}

interface Member {
    userId: number;
    nickname: string;
    profileImage?: string;
    role: string;
    joinedAt: string;
    attendanceRate: number;
}

const MemberManagement: React.FC<MemberManagementProps> = ({ studyId, maxMembers }) => {
    const [members, setMembers] = useState<Member[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const { showToast } = useUIStore();

    useEffect(() => {
        loadMembers();
    }, [studyId]);

    const loadMembers = async () => {
        setLoading(true);
        try {
            const response = await studyApi.getStudyMembers(studyId, 0, 100);
            console.log('[MemberManagement] API 응답:', response);
            const content = response?.data?.content || response?.content || [];
            console.log('[MemberManagement] content:', content);
            console.log('[MemberManagement] 멤버 수:', content.length);

            const mappedMembers: Member[] = content.map((member: any) => ({
                userId: member.userId || member.id,
                nickname: member.nickname || member.userName || '익명',
                profileImage: member.profileImage,
                role: member.role || 'MEMBER',
                joinedAt: member.joinedAt || member.createdAt || new Date().toISOString(),
                attendanceRate: member.attendanceRate || 0,
            }));

            console.log('[MemberManagement] 매핑된 멤버:', mappedMembers);
            setMembers(mappedMembers);
        } catch (error) {
            console.error('멤버 목록 조회 실패:', error);
            showToast('멤버 목록을 불러오는데 실패했습니다.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleExpel = (userId: number, nickname: string) => {
        // TODO: 백엔드 API 구현 후 연동
        showToast('멤버 강퇴 기능은 준비 중입니다.', 'info');
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
    };

    const filteredMembers = members.filter(member =>
        member.nickname.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const getAttendanceColor = (rate: number) => {
        if (rate >= 90) return 'text-success';
        if (rate >= 70) return 'text-warning';
        return 'text-error';
    };

    const getRoleIcon = (role: string) => {
        if (role === 'LEADER') return <Crown size={14} className="text-warning" />;
        if (role === 'MANAGER') return <Shield size={14} className="text-info" />;
        return null;
    };

    if (loading) {
        return (
            <div className="text-center py-12">
                <div className="inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p className="text-text-secondary mt-4">멤버 목록을 불러오는 중...</p>
            </div>
        );
    }

    if (members.length === 0) {
        return (
            <div className="text-center py-12 bg-background-secondary rounded-2xl">
                <Users size={48} className="mx-auto text-text-muted mb-4" />
                <p className="text-text-secondary">현재 스터디에 참여 중인 멤버가 없습니다</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-bold text-text-primary">멤버 관리</h2>
                    <p className="text-sm text-text-secondary mt-1">스터디원을 관리하고 출석률을 확인하세요</p>
                </div>
                <div className="flex items-center gap-2 px-4 py-2 bg-success/10 text-success rounded-xl">
                    <CheckCircle2 size={16} />
                    <span className="text-sm font-medium">{members.length}/{maxMembers}명</span>
                </div>
            </div>

            {/* 검색 */}
            <div className="relative">
                <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-text-tertiary" />
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="멤버 검색..."
                    className="w-full pl-11 pr-4 py-3 bg-background-secondary border border-border-light rounded-xl text-sm outline-none focus:border-primary transition-colors"
                />
            </div>

            {/* 멤버 그리드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredMembers.map((member) => (
                    <div
                        key={member.userId}
                        className="bg-background-secondary rounded-2xl p-5 border border-border-light hover:shadow-md transition-all group"
                    >
                        <div className="flex items-start gap-4">
                            {/* 아바타 */}
                            <div className="relative">
                                <div className="w-14 h-14 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-white font-bold text-xl overflow-hidden">
                                    <img src={member.profileImage || DEFAULT_PROFILE_IMAGE} alt={member.nickname} className="w-full h-full object-cover" />
                                </div>
                                {getRoleIcon(member.role) && (
                                    <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-surface border-2 border-background-secondary flex items-center justify-center">
                                        {getRoleIcon(member.role)}
                                    </div>
                                )}
                            </div>

                            {/* 정보 */}
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                    <span className="font-bold text-text-primary">{member.nickname}</span>
                                    <RoleBadge role={member.role} />
                                </div>
                                <div className="text-xs text-text-tertiary mb-3">
                                    {formatDate(member.joinedAt)} 가입
                                </div>

                                {/* 출석률 바 */}
                                <div className="space-y-1">
                                    <div className="flex items-center justify-between text-xs">
                                        <span className="text-text-tertiary flex items-center gap-1">
                                            <TrendingUp size={12} />
                                            출석률
                                        </span>
                                        <span className={`font-bold ${getAttendanceColor(member.attendanceRate)}`}>
                                            {member.attendanceRate}%
                                        </span>
                                    </div>
                                    <div className="h-2 bg-border-light rounded-full overflow-hidden">
                                        <div 
                                            className={`h-full rounded-full transition-all ${
                                                member.attendanceRate >= 90 ? 'bg-success' :
                                                member.attendanceRate >= 70 ? 'bg-warning' : 'bg-error'
                                            }`}
                                            style={{ width: `${member.attendanceRate}%` }}
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 강퇴 버튼 */}
                            {member.role !== 'LEADER' && (
                                <button
                                    onClick={() => handleExpel(member.userId, member.nickname)}
                                    className="opacity-0 group-hover:opacity-100 w-9 h-9 rounded-xl bg-error/10 text-error flex items-center justify-center hover:bg-error/20 transition-all"
                                    title="강퇴하기"
                                >
                                    <UserMinus size={16} />
                                </button>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {filteredMembers.length === 0 && searchQuery && (
                <div className="text-center py-8 text-text-secondary">
                    '{searchQuery}' 검색 결과가 없습니다
                </div>
            )}
        </div>
    );
};

export default MemberManagement;
