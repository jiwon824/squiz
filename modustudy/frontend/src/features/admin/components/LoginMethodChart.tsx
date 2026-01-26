import React from 'react';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Legend,
    Tooltip
} from 'recharts';
import { LoginMethodStats } from '../../../api/endpoints/adminApi';

interface LoginMethodChartProps {
    data: LoginMethodStats[];
}

const COLORS: Record<string, string> = {
    'KAKAO': '#fee500',
    'GOOGLE': '#4285f4',
    'NAVER': '#03c75a',
    'EMAIL': '#6b7280'
};

const LoginMethodChart: React.FC<LoginMethodChartProps> = ({ data }) => {
    const formattedData = data.map(item => ({
        name: item.method,
        value: item.count,
        fill: COLORS[item.method] || '#8b5cf6'
    }));

    const total = data.reduce((sum, item) => sum + item.count, 0);

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">로그인 방식 분포</h3>
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
                                <Cell key={`cell-${index}`} fill={entry.fill} />
                            ))}
                        </Pie>
                        <Tooltip
                            formatter={(value: number) => [`${value}명 (${((value / total) * 100).toFixed(1)}%)`, '사용자']}
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

export default LoginMethodChart;
