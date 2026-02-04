import React from 'react';
import {
    ComposedChart,
    Line,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';
import {
    DailyMeetingStats,
    DailyAttendanceStats,
    DailyActivityStats
} from '../../../api/endpoints/adminApi';

interface ActivityTrendChartProps {
    meetingData: DailyMeetingStats[];
    attendanceData: DailyAttendanceStats[];
    activityData: DailyActivityStats[];
}

// 날짜 기준 데이터 병합
const mergeData = (
    meetingData: DailyMeetingStats[],
    attendanceData: DailyAttendanceStats[],
    activityData: DailyActivityStats[]
) => {
    const dateMap = new Map<string, { date: string; meetings: number; attendance: number; activity: number }>();

    // 미팅 데이터
    meetingData.forEach(item => {
        const formattedDate = new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        dateMap.set(item.date, {
            date: formattedDate,
            meetings: item.count,
            attendance: 0,
            activity: 0
        });
    });

    // 출석 데이터
    attendanceData.forEach(item => {
        const formattedDate = new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        const existing = dateMap.get(item.date);
        if (existing) {
            existing.attendance = item.count;
        } else {
            dateMap.set(item.date, {
                date: formattedDate,
                meetings: 0,
                attendance: item.count,
                activity: 0
            });
        }
    });

    // 활동 데이터
    activityData.forEach(item => {
        const formattedDate = new Date(item.date).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        const existing = dateMap.get(item.date);
        if (existing) {
            existing.activity = item.count;
        } else {
            dateMap.set(item.date, {
                date: formattedDate,
                meetings: 0,
                attendance: 0,
                activity: item.count
            });
        }
    });

    // 날짜순 정렬
    return Array.from(dateMap.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([, value]) => value);
};

const ActivityTrendChart: React.FC<ActivityTrendChartProps> = ({
    meetingData,
    attendanceData,
    activityData
}) => {
    const chartData = mergeData(meetingData, attendanceData, activityData);
    const hasData = chartData.length > 0;

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">일별 활동 추이 (30일)</h3>
            {!hasData ? (
                <div className="h-64 flex items-center justify-center text-gray-400">
                    데이터 없음
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={300}>
                    <ComposedChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis
                            dataKey="date"
                            tick={{ fontSize: 11 }}
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
                        <Legend />
                        <Bar dataKey="meetings" fill="#8b5cf6" name="미팅" barSize={20} />
                        <Line
                            type="monotone"
                            dataKey="attendance"
                            stroke="#10b981"
                            strokeWidth={2}
                            dot={{ fill: '#10b981', strokeWidth: 2 }}
                            name="출석"
                        />
                        <Line
                            type="monotone"
                            dataKey="activity"
                            stroke="#f59e0b"
                            strokeWidth={2}
                            dot={{ fill: '#f59e0b', strokeWidth: 2 }}
                            name="활동"
                        />
                    </ComposedChart>
                </ResponsiveContainer>
            )}
        </div>
    );
};

export default ActivityTrendChart;
