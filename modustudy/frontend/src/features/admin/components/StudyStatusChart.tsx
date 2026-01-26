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

const COLORS = ['#22c55e', '#3b82f6', '#6b7280', '#ef4444'];
const STATUS_LABELS: Record<string, string> = {
    'RECRUITING': '모집중',
    'IN_PROGRESS': '진행중',
    'COMPLETED': '완료',
    'CANCELLED': '취소'
};

const StudyStatusChart: React.FC<StudyStatusChartProps> = ({ data }) => {
    const formattedData = data.map(item => ({
        name: STATUS_LABELS[item.status] || item.status,
        value: item.count
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
                            {formattedData.map((_, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
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
