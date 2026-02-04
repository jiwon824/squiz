import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Legend
} from 'recharts';
import { CourseQuizStat } from '@/api/endpoints/continuousQuizApi';

interface Props {
    data: CourseQuizStat[];
}

export const CourseQuizStatsChart: React.FC<Props> = React.memo(({ data }) => {
    if (!data || data.length === 0) {
        return (
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-center h-[300px]">
                <p className="text-gray-400">데이터가 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
            <h3 className="text-lg font-bold text-gray-900 mb-6">코스별 학습 현황</h3>
            <div className="h-[300px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                        <XAxis
                            dataKey="courseName"
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#6B7280', fontSize: 12 }}
                            dy={10}
                        />
                        <YAxis
                            axisLine={false}
                            tickLine={false}
                            tick={{ fill: '#6B7280', fontSize: 12 }}
                        />
                        <Tooltip
                            content={({ active, payload, label }) => {
                                if (active && payload && payload.length) {
                                    const attempted = payload.find((p: any) => p.dataKey === 'attemptedCount');
                                    const correct = payload.find((p: any) => p.dataKey === 'correctCount');
                                    const attemptedVal = (attempted?.value as number) || 0;
                                    const correctVal = (correct?.value as number) || 0;

                                    return (
                                        <div className="bg-white p-4 border border-gray-100 shadow-xl rounded-xl min-w-[150px]">
                                            <p className="font-bold text-gray-900 mb-2">{label}</p>
                                            <div className="space-y-1.5 text-sm">
                                                <div className="flex justify-between items-center text-gray-600">
                                                    <span className="flex items-center gap-1.5">
                                                        <div className="w-2 h-2 rounded-full bg-gray-400" />
                                                        시도
                                                    </span>
                                                    <span className="font-semibold text-gray-900">{attemptedVal}문제</span>
                                                </div>
                                                <div className="flex justify-between items-center text-blue-600">
                                                    <span className="flex items-center gap-1.5">
                                                        <div className="w-2 h-2 rounded-full bg-blue-500" />
                                                        정답
                                                    </span>
                                                    <span className="font-semibold text-blue-700">{correctVal}문제</span>
                                                </div>
                                                <div className="pt-2 mt-2 border-t border-gray-50 text-right">
                                                    <span className="text-xs text-gray-400 mr-1">정답률</span>
                                                    <span className="text-sm font-bold text-blue-600">
                                                        {attemptedVal > 0 ? Math.round((correctVal / attemptedVal) * 100) : 0}%
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                }
                                return null;
                            }}
                            cursor={{ stroke: '#E5E7EB', strokeWidth: 1 }}
                        />
                        <Legend wrapperStyle={{ paddingTop: '20px' }} />
                        <Line
                            type="monotone"
                            dataKey="attemptedCount"
                            name="시도한 문제"
                            stroke="#9CA3AF"
                            strokeWidth={3}
                            dot={{ r: 4, fill: '#white', stroke: '#9CA3AF', strokeWidth: 2 }}
                            activeDot={{ r: 6, fill: '#9CA3AF' }}
                            animationDuration={1000}
                        />
                        <Line
                            type="monotone"
                            dataKey="correctCount"
                            name="맞힌 문제"
                            stroke="#3B82F6"
                            strokeWidth={3}
                            dot={{ r: 4, fill: '#white', stroke: '#3B82F6', strokeWidth: 2 }}
                            activeDot={{ r: 6, fill: '#3B82F6' }}
                            animationDuration={1000}
                        />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
});

CourseQuizStatsChart.displayName = 'CourseQuizStatsChart';
