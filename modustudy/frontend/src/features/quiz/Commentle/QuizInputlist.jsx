import React from 'react';

const QuizInputList = ({ guesses }) => {

    // 다채로운 파스텔 톤 색상 (점수별)
    const getProgressBarColor = (score) => {
        if (score >= 90) return '#4ade80'; // Pastel Green (완벽)
        if (score >= 75) return '#60a5fa'; // Pastel Blue (아주 좋음)
        if (score >= 50) return '#a78bfa'; // Pastel Purple (좋음)
        if (score >= 25) return '#fbbf24'; // Pastel Yellow (보통)
        return '#f87171'; // Pastel Red (멈)
    };

    if (!guesses || guesses.length === 0) {
        return (
            <div className="bg-gray-50 border border-gray-200 border-dashed rounded-xl p-8 w-full text-center text-gray-400 flex flex-col items-center gap-2">
                <span className="text-4xl">⌨️</span>
                <p>단어를 입력하여 유사도를 확인해보세요!<br />100점에 가까울수록 정답입니다.</p>
            </div>
        );
    }

    return (
        <div className="input-list-container w-full max-w-lg mx-auto flex flex-col gap-2 mt-4">

            <div className="flex flex-col gap-2 max-h-[500px] overflow-y-auto pr-1 custom-scrollbar">
                {guesses.map((guess, index) => {
                    const barColor = getProgressBarColor(guess.score);
                    const listIndex = guesses.length - index; // 최신순 정렬이므로 역순 번호

                    return (
                        <div key={guess.id || index} className="guess-item">
                            <div className="flex justify-between items-center mb-1">
                                <div className="flex items-center gap-2">
                                    <span className="text-gray-400 text-sm font-mono w-8">#{listIndex}</span>
                                    <span className="font-bold text-gray-800 text-lg">{guess.word}</span>
                                </div>
                                <div className="flex items-center gap-3">
                                    <span className="text-xs text-gray-400">유사도</span>
                                    <span className="font-bold text-lg" style={{ color: barColor }}>
                                        {typeof guess.score === 'number' ? guess.score.toFixed(2) : guess.score}
                                    </span>
                                </div>
                            </div>

                            {/* 프로그레스 바 */}
                            <div className="guess-progress-container">
                                <div
                                    className="guess-progress-bar"
                                    style={{
                                        width: `${guess.score}%`,
                                        backgroundColor: barColor
                                    }}
                                ></div>
                            </div>

                            {/* 추가 정보나 아이콘 */}
                            {guess.score >= 90 && (
                                <div className="mt-1 flex justify-end">
                                    <span className="text-xs font-bold text-yellow-600">✨ 매우 가까워요!</span>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default QuizInputList;
