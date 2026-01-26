import React from 'react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from 'recharts';
import { QuizStats } from '../../../api/endpoints/adminApi';

interface QuizStatsChartProps {
    data: QuizStats | null;
}

const QuizStatsChart: React.FC<QuizStatsChartProps> = ({ data }) => {
    const dailyData = data?.dailyAttempts.map(item => ({
        date: new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' }),
        count: item.count
    })) || [];

    const courseData = data?.courseParticipation.slice(0, 5).map(item => ({
        name: item.courseName.length > 15 ? item.courseName.substring(0, 15) + '...' : item.courseName,
        participants: item.participantCount
    })) || [];

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">퀴즈 통계</h3>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Daily Quiz Attempts */}
                <div>
                    <h4 className="text-sm font-medium text-gray-600 mb-3">일별 퀴즈 시도</h4>
                    {dailyData.length === 0 ? (
                        <div className="h-48 flex items-center justify-center text-gray-400">
                            데이터 없음
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={200}>
                            <BarChart data={dailyData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                                <XAxis
                                    dataKey="date"
                                    tick={{ fontSize: 10 }}
                                    tickLine={false}
                                    angle={-45}
                                    textAnchor="end"
                                    height={60}
                                />
                                <YAxis
                                    tick={{ fontSize: 10 }}
                                    tickLine={false}
                                    allowDecimals={false}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#fff',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: '8px'
                                    }}
                                />
                                <Bar
                                    dataKey="count"
                                    fill="#8b5cf6"
                                    radius={[4, 4, 0, 0]}
                                    name="시도"
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* Course Participation */}
                <div>
                    <h4 className="text-sm font-medium text-gray-600 mb-3">코스별 참여자 (TOP 5)</h4>
                    {courseData.length === 0 ? (
                        <div className="h-48 flex items-center justify-center text-gray-400">
                            데이터 없음
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={200}>
                            <BarChart data={courseData} layout="vertical">
                                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                                <XAxis
                                    type="number"
                                    tick={{ fontSize: 10 }}
                                    tickLine={false}
                                    allowDecimals={false}
                                />
                                <YAxis
                                    type="category"
                                    dataKey="name"
                                    tick={{ fontSize: 10 }}
                                    tickLine={false}
                                    width={100}
                                />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#fff',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: '8px'
                                    }}
                                />
                                <Bar
                                    dataKey="participants"
                                    fill="#10b981"
                                    radius={[0, 4, 4, 0]}
                                    name="참여자"
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    )}
                </div>
            </div>
        </div>
    );
};

export default QuizStatsChart;
