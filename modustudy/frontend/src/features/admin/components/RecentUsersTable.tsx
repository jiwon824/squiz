import React from 'react';
import { RecentUser } from '../../../api/endpoints/adminApi';

interface RecentUsersTableProps {
    data: RecentUser[];
}

const RecentUsersTable: React.FC<RecentUsersTableProps> = ({ data }) => {
    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const getMethodBadgeColor = (method: string) => {
        switch (method) {
            case 'KAKAO':
                return 'bg-yellow-100 text-yellow-800';
            case 'GOOGLE':
                return 'bg-blue-100 text-blue-800';
            case 'NAVER':
                return 'bg-green-100 text-green-800';
            case 'EMAIL':
                return 'bg-gray-100 text-gray-800';
            default:
                return 'bg-purple-100 text-purple-800';
        }
    };

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">최근 가입 회원</h3>
            {data.length === 0 ? (
                <div className="text-center text-gray-400 py-8">최근 가입 회원 없음</div>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-gray-200">
                                <th className="text-left py-3 px-2 font-medium text-gray-600">닉네임</th>
                                <th className="text-left py-3 px-2 font-medium text-gray-600">이메일</th>
                                <th className="text-left py-3 px-2 font-medium text-gray-600">가입 방식</th>
                                <th className="text-left py-3 px-2 font-medium text-gray-600">가입일</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.map((user) => (
                                <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50">
                                    <td className="py-3 px-2 font-medium text-gray-800">{user.nickname}</td>
                                    <td className="py-3 px-2 text-gray-600">{user.email}</td>
                                    <td className="py-3 px-2">
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getMethodBadgeColor(user.loginMethod)}`}>
                                            {user.loginMethod}
                                        </span>
                                    </td>
                                    <td className="py-3 px-2 text-gray-500">{formatDate(user.createdAt)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default RecentUsersTable;
