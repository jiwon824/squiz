import React from 'react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Cell
} from 'recharts';
import { UserLevelStats } from '../../../api/endpoints/adminApi';

interface UserLevelChartProps {
    data: UserLevelStats[];
}

// 레벨별 색상 정의
const LEVEL_COLORS = [
    '#10b981', // 1 새싹 - 녹색
    '#3b82f6', // 2 파란색
    '#8b5cf6', // 3 보라색
    '#f59e0b', // 4 주황색
    '#ef4444', // 5 빨간색
    '#ec4899', // 6 핑크색
    '#14b8a6', // 7 청록색
    '#6366f1', // 8 인디고
    '#f97316', // 9 오렌지
    '#dc2626', // 10 진한빨강
];

const UserLevelChart: React.FC<UserLevelChartProps> = ({ data }) => {
    // 레벨순 정렬
    const sortedData = [...data].sort((a, b) => a.level - b.level);

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">레벨별 사용자 분포</h3>
            {data.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-gray-400">
                    데이터 없음
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={sortedData} layout="vertical">
                        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis
                            type="number"
                            tick={{ fontSize: 12 }}
                            tickLine={false}
                            axisLine={{ stroke: '#e5e7eb' }}
                            allowDecimals={false}
                        />
                        <YAxis
                            type="category"
                            dataKey="levelName"
                            tick={{ fontSize: 12 }}
                            tickLine={false}
                            axisLine={{ stroke: '#e5e7eb' }}
                            width={80}
                        />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: '#fff',
                                border: '1px solid #e5e7eb',
                                borderRadius: '8px'
                            }}
                            formatter={(value: number) => [`${value}명`, '사용자']}
                        />
                        <Bar dataKey="count" name="사용자 수" barSize={24}>
                            {sortedData.map((entry, index) => (
                                <Cell
                                    key={`cell-${index}`}
                                    fill={LEVEL_COLORS[entry.level - 1] || '#6b7280'}
                                />
                            ))}
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            )}
        </div>
    );
};

export default UserLevelChart;
