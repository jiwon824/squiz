import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

/**
 * QUIZ CONTEST 준비중 페이지
 */
export const QuizContestComingSoon = () => {
    const navigate = useNavigate();

    return (
        <div className="relative flex flex-col items-center justify-center min-h-[80vh] p-6">
            {/* 뒤로가기 버튼 */}
            <button
                onClick={() => navigate('/quiz')}
                className="absolute top-6 left-6 flex items-center gap-2 px-4 py-2 rounded-lg text-text-secondary hover:text-text-primary hover:bg-gray-100 transition-all"
            >
                <ArrowLeft size={20} />
                <span className="font-medium">퀴즈 센터로 돌아가기</span>
            </button>

            {/* 준비중 이미지 */}
            <img
                src="/images/준비중인 페이지.png"
                alt="준비중인 페이지"
                className="max-w-full max-h-[60vh] object-contain"
            />

            {/* 안내 텍스트 */}
            <div className="mt-8 text-center">
                <h1 className="text-2xl font-black text-text-primary mb-2">
                    QUIZ CONTEST
                </h1>
                <p className="text-text-secondary">
                    실시간 온라인 대전 기능을 준비 중입니다. 조금만 기다려주세요!
                </p>
            </div>
        </div>
    );
};

export default QuizContestComingSoon;
