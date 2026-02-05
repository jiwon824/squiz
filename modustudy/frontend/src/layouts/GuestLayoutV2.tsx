// 비로그인 사용자 전용 레이아웃 V2 - Full-width, 사이드바 제거

import { motion } from 'framer-motion';
import { useEffect, useState } from 'react';

interface GuestLayoutV2Props {
    children: React.ReactNode;
}

export const GuestLayoutV2: React.FC<GuestLayoutV2Props> = ({ children }) => {
    const [, setWindowWidth] = useState(window.innerWidth);

    // 반응형 리사이즈 감지
    useEffect(() => {
        const handleResize = () => setWindowWidth(window.innerWidth);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    return (
        <div className="flex flex-col h-screen bg-study-bg overflow-hidden">
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
