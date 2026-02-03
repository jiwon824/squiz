import { motion } from 'framer-motion';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

// 멤버 타입 정의
export interface WorkspaceMember {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: 'LEADER' | 'MEMBER';
  isOnline?: boolean;
  isIdle?: boolean;
}

interface MemberListProps {
  members: WorkspaceMember[];
  isVisible: boolean;
  onMemberClick?: (member: WorkspaceMember) => void;
}

export const MemberList: React.FC<MemberListProps> = ({
  members,
  isVisible,
  onMemberClick,
}) => {
  // 역할별 분류
  const leaders = members.filter((m) => m.role === 'LEADER');
  const regularMembers = members.filter((m) => m.role === 'MEMBER');

  // 상태별 정렬 (온라인 > 자리비움 > 오프라인)
  const sortByOnline = (list: WorkspaceMember[]) =>
    [...list].sort((a, b) => {
      const rank = (member: WorkspaceMember) => {
        if (!member.isOnline) return 2;
        if (member.isIdle) return 1;
        return 0;
      };
      return rank(a) - rank(b);
    });

  const renderMember = (member: WorkspaceMember) => (
    <div
      key={member.id}
      className="member-list__item"
      onClick={() => onMemberClick?.(member)}
    >
      <div className="member-list__avatar">
        <img src={getProfileImageUrl(member.profileImageUrl)} alt={member.nickname} />
        <div
          className={cn(
            'member-list__status',
            !member.isOnline
              ? 'member-list__status--offline'
              : member.isIdle
              ? 'member-list__status--idle'
              : 'member-list__status--online'
          )}
        />
      </div>
      <div>
        <div className="member-list__name">{member.nickname}</div>
      </div>
    </div>
  );

  return (
    <motion.div
      className="member-list"
      initial={{ width: 240 }}
      animate={{ width: isVisible ? 240 : 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
    >
      <div className="member-list__inner">
        {/* 리더 섹션 */}
        {leaders.length > 0 && (
          <div className="member-list__section">
            <div className="member-list__title">
              스터디장 — {leaders.length}
            </div>
            {sortByOnline(leaders).map(renderMember)}
          </div>
        )}

        {/* 일반 멤버 섹션 */}
        {regularMembers.length > 0 && (
          <div className="member-list__section">
            <div className="member-list__title">
              멤버 — {regularMembers.length}
            </div>
            {sortByOnline(regularMembers).map(renderMember)}
          </div>
        )}

        {/* 멤버가 없는 경우 */}
        {members.length === 0 && (
          <div className="member-list__section">
            <div className="member-list__title">멤버 없음</div>
          </div>
        )}
      </div>
    </motion.div>
  );
};
