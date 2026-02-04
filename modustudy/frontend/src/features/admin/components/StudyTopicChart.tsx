import React from 'react';
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import { StudyTopicStats } from '../../../api/endpoints/adminApi';

interface StudyTopicChartProps {
    data: StudyTopicStats[];
}

// 토픽별 색상 정의
const TOPIC_COLORS = [
    '#3b82f6', // 파란색
    '#10b981', // 녹색
    '#f59e0b', // 주황색
    '#8b5cf6', // 보라색
    '#ef4444', // 빨간색
    '#ec4899', // 핑크색
    '#14b8a6', // 청록색
    '#6366f1', // 인디고
    '#f97316', // 오렌지
    '#84cc16', // 라임
    '#06b6d4', // 시안
    '#a855f7', // 퍼플
];

const StudyTopicChart: React.FC<StudyTopicChartProps> = ({ data }) => {
    // 상위 10개만 표시, 나머지는 '기타'로 묶음
    let chartData = [...data];
    if (chartData.length > 10) {
        const top10 = chartData.slice(0, 10);
        const others = chartData.slice(10);
        const othersCount = others.reduce((sum, item) => sum + item.count, 0);
        chartData = [...top10, { topicName: '기타', count: othersCount }];
    }

    const total = chartData.reduce((sum, item) => sum + item.count, 0);

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">토픽별 스터디 분포</h3>
            {data.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-gray-400">
                    데이터 없음
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                        <Pie
                            data={chartData}
                            dataKey="count"
                            nameKey="topicName"
                            cx="50%"
                            cy="50%"
                            outerRadius={100}
                            innerRadius={50}
                            labelLine={false}
                            label={({ topicName, percent }) =>
                                percent > 0.05 ? `${topicName} (${(percent * 100).toFixed(0)}%)` : ''
                            }
                        >
                            {chartData.map((_, index) => (
                                <Cell
                                    key={`cell-${index}`}
                                    fill={TOPIC_COLORS[index % TOPIC_COLORS.length]}
                                />
                            ))}
                        </Pie>
                        <Tooltip
                            contentStyle={{
                                backgroundColor: '#fff',
                                border: '1px solid #e5e7eb',
                                borderRadius: '8px'
                            }}
                            formatter={(value: number, name: string) => [
                                `${value}개 (${((value / total) * 100).toFixed(1)}%)`,
                                name
                            ]}
                        />
                        <Legend />
                    </PieChart>
                </ResponsiveContainer>
            )}
        </div>
    );
};

export default StudyTopicChart;
