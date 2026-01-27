import React from 'react';
import { Users } from 'lucide-react';
import { PopularStudy } from '../../../api/endpoints/adminApi';

interface PopularStudiesTableProps {
    data: PopularStudy[];
}

const PopularStudiesTable: React.FC<PopularStudiesTableProps> = ({ data }) => {
    const getStatusBadgeColor = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return 'bg-green-100 text-green-800';
            case 'IN_PROGRESS':
                return 'bg-blue-100 text-blue-800';
            case 'COMPLETED':
                return 'bg-gray-100 text-gray-800';
            case 'CANCELLED':
                return 'bg-red-100 text-red-800';
            default:
                return 'bg-purple-100 text-purple-800';
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return '모집중';
            case 'IN_PROGRESS':
                return '진행중';
            case 'COMPLETED':
                return '완료';
            case 'CANCELLED':
                return '취소';
            default:
                return status;
        }
    };

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">인기 스터디</h3>
            {data.length === 0 ? (
                <div className="text-center text-gray-400 py-8">스터디 없음</div>
            ) : (
                <div className="space-y-3">
                    {data.map((study, index) => (
                        <div
                            key={study.id}
                            className="flex items-center justify-between p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
                        >
                            <div className="flex items-center gap-3">
                                <div className="w-8 h-8 rounded-full bg-blue-500 text-white flex items-center justify-center font-bold text-sm">
                                    {index + 1}
                                </div>
                                <div>
                                    <p className="font-medium text-gray-800">{study.name}</p>
                                    <p className="text-xs text-gray-500">{study.topicName || '토픽 없음'}</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <div className="flex items-center gap-1 text-gray-600">
                                    <Users className="w-4 h-4" />
                                    <span className="text-sm font-medium">{study.memberCount}</span>
                                </div>
                                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeColor(study.status)}`}>
                                    {getStatusLabel(study.status)}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default PopularStudiesTable;
