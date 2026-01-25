import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SquizLogo } from '../../shared/components/SquizLogo';

export const StartPage = () => {
    const navigate = useNavigate();
    const [isExiting, setIsExiting] = useState(false);

    useEffect(() => {
        // 1초 동안 로고 애니메이션을 보여준 뒤 달려나가는 애니메이션 시작
        const timer = setTimeout(() => {
            setIsExiting(true);
        }, 1000);

        return () => clearTimeout(timer);
    }, []);

    useEffect(() => {
        if (isExiting) {
            // 달려나가는 애니메이션(0.6초)이 끝난 후 페이지 이동
            const timer = setTimeout(() => {
                navigate('/dashboard');
            }, 600);
            return () => clearTimeout(timer);
        }
    }, [isExiting, navigate]);

    return (
        <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            backgroundColor: '#F8F9FA',
            overflow: 'hidden' // 애니메이션 시 스크롤 방지
        }}>
            <style>
                {`
                    @keyframes runOut {
                        0% { transform: translateX(0) skewX(0); }
                        20% { transform: translateX(-50px) skewX(20deg); } /* 도움닫기 느낌 */
                        100% { transform: translateX(150vw) skewX(-30deg); } /* 질주 */
                    }
                    .running-out {
                        animation: runOut 0.6s cubic-bezier(0.5, 0, 0.5, 1) forwards;
                    }
                `}
            </style>
            <div className={isExiting ? 'running-out' : ''}>
                <SquizLogo width={300} height={300} />
            </div>
        </div>
    );
};
