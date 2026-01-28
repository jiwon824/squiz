// 비로그인 사용자 전용 레이아웃 V2 - Full-width, 사이드바 제거

import { motion } from 'framer-motion';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';

interface GuestLayoutV2Props {
    children: React.ReactNode;
}

export const GuestLayoutV2: React.FC<GuestLayoutV2Props> = ({ children }) => {
    const [windowWidth, setWindowWidth] = useState(window.innerWidth);

    // 반응형 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    return (
        <div className="flex flex-col h-screen bg-study-bg overflow-hidden">
            {/* 헤더 - 최소화된 버전 */}
            <header className="h-16 w-full bg-white/80 backdrop-blur-md border-b border-study-blue/10 flex items-center justify-between px-6 flex-shrink-0 z-50 shadow-sm">
                <div className="flex items-center gap-4">
                    {/* 로고 영역 */}
                    <Link to="/" className="flex items-center">
                        <SquizLogoNew width={160} height={55} className="scale-110 origin-left" />
                    </Link>
                </div>

                {/* 우측 CTA */}
                <div className="flex items-center gap-3">
                    <Link
                        to="/login"
                        className="p-2 px-5 bg-study-blue hover:bg-study-blue-dark text-white rounded-pill text-sm font-bold shadow-md shadow-study-blue/20 transition-all hover:scale-105 active:scale-95 no-underline"
                    >
                        로그인 / 회원가입
                    </Link>
                </div>
            </header>

            {/* 메인 콘텐츠 - Full Width */}
            <motion.main
                className="flex-1 flex flex-col overflow-hidden"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.3 }}
            >
                <div
                    id="guest-content-scroll"
                    className="flex-1 overflow-auto bg-study-bg"
                >
                    {children}
                </div>
            </motion.main>
        </div>
    );
};
