import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from 'recharts';
import { UserSignupStats } from '../../../api/endpoints/adminApi';

interface UserSignupChartProps {
    data: UserSignupStats[];
}

const UserSignupChart: React.FC<UserSignupChartProps> = ({ data }) => {
    const formattedData = data.map(item => ({
        ...item,
        date: new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
    }));

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">회원 가입 추이 (30일)</h3>
            {data.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-gray-400">
                    데이터 없음
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={formattedData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis
                            dataKey="date"
                            tick={{ fontSize: 12 }}
                            tickLine={false}
                            axisLine={{ stroke: '#e5e7eb' }}
                        />
                        <YAxis
                            tick={{ fontSize: 12 }}
                            tickLine={false}
                            axisLine={{ stroke: '#e5e7eb' }}
                            allowDecimals={false}
                        />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: '#fff',
                                border: '1px solid #e5e7eb',
                                borderRadius: '8px'
                            }}
                        />
                        <Line
                            type="monotone"
                            dataKey="count"
                            stroke="#3b82f6"
                            strokeWidth={2}
                            dot={{ fill: '#3b82f6', strokeWidth: 2 }}
                            activeDot={{ r: 6 }}
                            name="가입자"
                        />
                    </LineChart>
                </ResponsiveContainer>
            )}
        </div>
    );
};

export default UserSignupChart;
