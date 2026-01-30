import React from 'react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Legend,
    Tooltip
} from 'recharts';
import { StudyStatusStats } from '../../../api/endpoints/adminApi';

interface StudyStatusChartProps {
    data: StudyStatusStats[];
}

// Google Design System 색상 (colors.ts 기반)
const STATUS_COLORS: Record<string, string> = {
    'DRAFT': '#5F6368',       // gray.500 - 비활성/임시저장
    'SCHEDULED': '#FBBC04',   // google.yellow - 예정/알림
    'RECRUITING': '#4285F4',  // google.blue - 활성/모집중
    'RECRUIT_CLOSED': '#34A853', // google.green - 모집 성공
    'PENDING': '#f9ab00',     // google.yellow.dark - 대기/주의
    'IN_PROGRESS': '#1a73e8', // google.blue.dark - 진행중
    'COMPLETED': '#137333',   // google.green.dark - 완료
    'CANCELLED': '#EA4335',   // google.red - 취소
};
const STATUS_LABELS: Record<string, string> = {
    'DRAFT': '임시저장',
    'SCHEDULED': '모집예정',
    'RECRUITING': '모집중',
    'RECRUIT_CLOSED': '모집완료',
    'PENDING': '확정대기',
    'IN_PROGRESS': '진행중',
    'COMPLETED': '완료',
    'CANCELLED': '취소'
};

const StudyStatusChart: React.FC<StudyStatusChartProps> = ({ data }) => {
    const formattedData = data.map(item => ({
        name: STATUS_LABELS[item.status] || item.status,
        value: item.count,
        color: STATUS_COLORS[item.status] || '#5F6368'
    }));

    const total = data.reduce((sum, item) => sum + item.count, 0);

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">스터디 상태 분포</h3>
            {data.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-gray-400">
                    데이터 없음
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                        <Pie
                            data={formattedData}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={100}
                            paddingAngle={2}
                            dataKey="value"
                            label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                            labelLine={false}
                        >
                            {formattedData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={entry.color} />
                            ))}
                        </Pie>
                        <Tooltip
                            formatter={(value: number) => [`${value}개 (${((value / total) * 100).toFixed(1)}%)`, '개수']}
                            contentStyle={{
                                backgroundColor: '#fff',
                                border: '1px solid #e5e7eb',
                                borderRadius: '8px'
                            }}
                        />
                        <Legend />
                    </PieChart>
                </ResponsiveContainer>
            )}
        </div>
    );
};

export default StudyStatusChart;
