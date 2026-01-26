import { useState, KeyboardEvent, useEffect } from 'react';
import { cn } from '@/shared/utils/cn';
import { Check, Plus, X } from 'lucide-react';

interface GoalTask {
    id: number;
    title: string;
    completed: boolean;
}

interface DailyGoalsWidgetProps {
    date?: Date | string;
}

export const DailyGoalsWidget = ({ date }: DailyGoalsWidgetProps) => {
    // 날짜 포맷팅
    const targetDate = date ? new Date(date) : new Date();
    const dateStr = `${String(targetDate.getMonth() + 1).padStart(2, '0')}.${String(targetDate.getDate()).padStart(2, '0')}`;
    const dayStr = targetDate.toLocaleDateString('en-US', { weekday: 'short' });

    const [tasks, setTasks] = useState<GoalTask[]>([]);
    const [inputValue, setInputValue] = useState('');
    const [isInputFocused, setIsInputFocused] = useState(false);

    // 날짜가 변경되면 해당 날짜의 데이터를 불러오는 로직 (Mock)
    useEffect(() => {
        // 실제로는 API 호출이 필요함
        // GET /api/v1/users/me/daily-goals?date=YYYY-MM-DD

        // 데모를 위해 날짜별로 다른 데이터를 보여주거나 초기화
        const seed = targetDate.getDate(); // 날짜 숫자를 시드로 사용
        const mockTasks: GoalTask[] = [];

        // 예시: 짝수 날짜에는 완료된 항목이 있도록 시뮬레이션
        if (seed % 2 === 0) {
            mockTasks.push({ id: 1, title: '알고리즘 문제 풀기', completed: true });
            mockTasks.push({ id: 2, title: 'React 복습', completed: false });
        } else {
            // 홀수 날짜는 빈 상태 혹은 다른 데이터
        }

        setTasks(mockTasks);
    }, [dateStr]); // dateStr이 변경될 때마다 실행

    // 진행률 계산
    const completedCount = tasks.filter(t => t.completed).length;
    const progress = tasks.length > 0 ? Math.round((completedCount / tasks.length) * 100) : 0;

    const toggleTask = (id: number) => {
        setTasks(tasks.map(t =>
            t.id === id ? { ...t, completed: !t.completed } : t
        ));
    };

    const addTask = () => {
        if (!inputValue.trim()) return;
        const newTask: GoalTask = {
            id: Date.now(),
            title: inputValue.trim(),
            completed: false
        };
        setTasks([...tasks, newTask]);
        setInputValue('');
    };

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
            addTask();
        }
    };

    const removeTask = (id: number, e: React.MouseEvent) => {
        e.stopPropagation();
        setTasks(tasks.filter(t => t.id !== id));
    };

    return (
        <div className="bg-white dark:bg-[#1e1e2d] rounded-2xl p-5 md:p-6 xl:p-8 shadow-xl dark:shadow-2xl space-y-6 border border-gray-100 dark:border-none transition-colors duration-300 w-full h-auto xl:h-full flex flex-col">
            {/* Header */}
            <div className="flex justify-between items-center h-8 gap-2 shrink-0">
                <h3 className="text-base md:text-lg font-bold text-gray-800 dark:text-white transition-colors whitespace-nowrap">
                    오늘의 학습 목표
                </h3>
                <span className="flex items-center justify-center h-full text-[10px] md:text-xs px-2.5 bg-gray-100 border-gray-200 text-gray-500 dark:bg-white/10 dark:text-white/50 border dark:border-white/5 rounded-full transition-colors whitespace-nowrap">
                    {dateStr} {dayStr}
                </span>
            </div>

            {/* Input Area */}
            <div className={cn(
                "flex items-center gap-3 px-4 py-3 rounded-2xl transition-all border shrink-0",
                isInputFocused
                    ? "bg-white dark:bg-[#1e1e2d] border-blue-500 ring-1 ring-blue-500"
                    : "bg-gray-50 dark:bg-black/20 border-transparent hover:bg-gray-100 dark:hover:bg-black/30"
            )}>
                <Plus className={cn("w-5 h-5 transition-colors", isInputFocused ? "text-blue-500" : "text-gray-400 dark:text-white/30")} />
                <input
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onFocus={() => setIsInputFocused(true)}
                    onBlur={() => setIsInputFocused(false)}
                    placeholder="새로운 목표 추가..."
                    className="w-full bg-transparent border-none outline-none text-sm text-gray-700 dark:text-white placeholder-gray-400 dark:placeholder-white/30"
                />
            </div>

            {/* Task List */}
            <div className="space-y-3 min-h-[100px] max-h-[400px] xl:max-h-none xl:flex-1 overflow-y-auto pr-1 scrollbar-hide">
                {tasks.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-8 text-gray-400 dark:text-white/20 text-sm h-full">
                        <p>등록된 목표가 없어요</p>
                    </div>
                )}
                {tasks.map((task) => (
                    <div
                        key={task.id}
                        onClick={() => toggleTask(task.id)}
                        className={cn(
                            "flex items-center gap-3 md:gap-4 p-3 md:p-4 rounded-2xl transition-all cursor-pointer group select-none relative",
                            task.completed
                                ? "bg-gray-50/50 dark:bg-white/5 opacity-50"
                                : "bg-gray-50 hover:bg-gray-100 dark:bg-white/10 dark:hover:bg-white/15"
                        )}
                    >
                        {/* Checkbox */}
                        <div className={cn(
                            "shrink-0 w-5 h-5 md:w-6 md:h-6 rounded-lg border-2 flex items-center justify-center transition-all",
                            task.completed
                                ? "bg-blue-500 border-blue-500"
                                : "border-gray-300 dark:border-white/20 group-hover:border-blue-400"
                        )}>
                            {task.completed && (
                                <Check className="w-3 md:w-3.5 h-3 md:h-3.5 text-white" strokeWidth={3} />
                            )}
                        </div>

                        {/* Text */}
                        <span className={cn(
                            "text-xs md:text-sm font-medium transition-colors break-all pr-6",
                            task.completed
                                ? "text-gray-400 dark:text-white/50 line-through"
                                : "text-gray-700 dark:text-white"
                        )}>
                            {task.title}
                        </span>

                        {/* Delete Button (Hover) */}
                        <button
                            onClick={(e) => removeTask(task.id, e)}
                            className="absolute right-3 p-1 rounded-full opacity-0 group-hover:opacity-100 hover:bg-gray-200 dark:hover:bg-white/20 text-gray-400 dark:text-white/40 transition-all"
                        >
                            <X className="w-3 h-3 md:w-4 md:h-4" />
                        </button>
                    </div>
                ))}
            </div>

            {/* Progress */}
            <div className="space-y-2 shrink-0">
                <div className="flex justify-between text-[10px] md:text-[11px] font-bold text-gray-400 dark:text-white/40 mb-1 transition-colors">
                    <span>PROGRESS</span>
                    <span>{progress}%</span>
                </div>
                <div className="h-1.5 md:h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden transition-colors">
                    <div
                        className="h-full bg-blue-500 rounded-full transition-all duration-500 ease-out"
                        style={{ width: `${progress}%` }}
                    ></div>
                </div>
            </div>
        </div>
    );
};
