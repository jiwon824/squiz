import { useRef, useEffect, useState } from 'react';

interface MascotWithEyesProps {
    className?: string;
    size?: number;
}

/**
 * 마우스 커서를 따라 눈동자가 움직이는 마스코트 컴포넌트
 * 돌고래 마스코트의 눈이 커서 방향을 바라봅니다
 */
export const MascotWithEyes = ({ className = '', size = 120 }: MascotWithEyesProps) => {
    const containerRef = useRef<HTMLDivElement>(null);
    // 왼쪽 눈, 오른쪽 눈 각각의 위치
    const [leftPupil, setLeftPupil] = useState({ x: 0, y: 0 });
    const [rightPupil, setRightPupil] = useState({ x: 0, y: 0 });

    useEffect(() => {
        const handleMouseMove = (e: MouseEvent) => {
            if (!containerRef.current) return;

            const rect = containerRef.current.getBoundingClientRect();
            const containerCenterX = rect.left + rect.width / 2;
            const containerCenterY = rect.top + rect.height / 2;

            // 마우스와 마스코트 중심 사이의 각도 계산
            const deltaX = e.clientX - containerCenterX;
            const deltaY = e.clientY - containerCenterY;

            // 거리 계산 (최대 이동 거리 제한)
            const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            const maxMove = 3; // 눈동자 최대 이동 거리 (px)

            // 정규화된 방향 벡터
            const normalizedX = distance > 0 ? deltaX / distance : 0;
            const normalizedY = distance > 0 ? deltaY / distance : 0;

            // 거리에 따른 이동량 (가까우면 작게, 멀면 최대)
            const moveAmount = Math.min(distance / 100, 1) * maxMove;

            const pupilX = normalizedX * moveAmount;
            const pupilY = normalizedY * moveAmount;

            setLeftPupil({ x: pupilX, y: pupilY });
            setRightPupil({ x: pupilX, y: pupilY });
        };

        window.addEventListener('mousemove', handleMouseMove);
        return () => window.removeEventListener('mousemove', handleMouseMove);
    }, []);

    // 이미지 크기에 따른 눈 위치 계산 (비율 기반)
    // 돌고래 이미지에서 눈의 상대적 위치 (이미지 분석 기반)
    const eyeSize = size * 0.08; // 눈동자 크기
    const leftEyePos = { x: size * 0.4, y: size * 0.32 };  // 왼쪽 눈 위치
    const rightEyePos = { x: size * 0.6, y: size * 0.32 }; // 오른쪽 눈 위치

    return (
        <div
            ref={containerRef}
            className={`relative inline-block ${className}`}
            style={{ width: size, height: size }}
        >
            {/* 마스코트 이미지 */}
            <img
                src="/images/squiz-dolphin.png"
                alt="SQUIZ 마스코트"
                className="w-full h-full object-contain"
                draggable={false}
            />

            {/* 왼쪽 눈동자 오버레이 */}
            <div
                className="absolute rounded-full pointer-events-none"
                style={{
                    width: eyeSize,
                    height: eyeSize,
                    left: leftEyePos.x - eyeSize / 2,
                    top: leftEyePos.y - eyeSize / 2,
                    transform: `translate(${leftPupil.x}px, ${leftPupil.y}px)`,
                    transition: 'transform 0.1s ease-out',
                    background: 'radial-gradient(circle at 35% 35%, #3d3d4d 0%, #1a1a2d 100%)',
                    boxShadow: 'inset 0 -1px 2px rgba(255,255,255,0.1)',
                }}
            >
                {/* 큰 하이라이트 */}
                <div
                    className="absolute rounded-full bg-white"
                    style={{
                        width: eyeSize * 0.3,
                        height: eyeSize * 0.3,
                        top: '18%',
                        left: '22%',
                        opacity: 0.9,
                    }}
                />
                {/* 작은 하이라이트 */}
                <div
                    className="absolute rounded-full bg-white"
                    style={{
                        width: eyeSize * 0.15,
                        height: eyeSize * 0.15,
                        top: '55%',
                        left: '55%',
                        opacity: 0.5,
                    }}
                />
            </div>

            {/* 오른쪽 눈동자 오버레이 */}
            <div
                className="absolute rounded-full pointer-events-none"
                style={{
                    width: eyeSize,
                    height: eyeSize,
                    left: rightEyePos.x - eyeSize / 2,
                    top: rightEyePos.y - eyeSize / 2,
                    transform: `translate(${rightPupil.x}px, ${rightPupil.y}px)`,
                    transition: 'transform 0.1s ease-out',
                    background: 'radial-gradient(circle at 35% 35%, #3d3d4d 0%, #1a1a2d 100%)',
                    boxShadow: 'inset 0 -1px 2px rgba(255,255,255,0.1)',
                }}
            >
                {/* 큰 하이라이트 */}
                <div
                    className="absolute rounded-full bg-white"
                    style={{
                        width: eyeSize * 0.3,
                        height: eyeSize * 0.3,
                        top: '18%',
                        left: '22%',
                        opacity: 0.9,
                    }}
                />
                {/* 작은 하이라이트 */}
                <div
                    className="absolute rounded-full bg-white"
                    style={{
                        width: eyeSize * 0.15,
                        height: eyeSize * 0.15,
                        top: '55%',
                        left: '55%',
                        opacity: 0.5,
                    }}
                />
            </div>
        </div>
    );
};

export default MascotWithEyes;
